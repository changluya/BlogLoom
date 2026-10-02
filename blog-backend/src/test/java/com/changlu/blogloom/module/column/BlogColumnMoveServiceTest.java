package com.changlu.blogloom.module.column;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.module.column.dao.BlogColumnMapper;
import com.changlu.blogloom.module.column.dao.BlogColumnRelationMapper;
import com.changlu.blogloom.module.column.domain.entity.BlogColumn;
import com.changlu.blogloom.module.column.service.ColumnCoverStorageService;
import com.changlu.blogloom.module.column.service.impl.BlogColumnServiceImpl;
import com.changlu.blogloom.service.BlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BlogColumnMoveServiceTest {

	private BlogColumnMapper columnMapper;
	private BlogColumnServiceImpl service;

	@BeforeEach
	void setUp() {
		columnMapper = mock(BlogColumnMapper.class);
		service = new BlogColumnServiceImpl(columnMapper, mock(BlogColumnRelationMapper.class),
				mock(ColumnCoverStorageService.class), mock(BlogService.class));
	}

	@Test
	@DisplayName("同级拖拽：按目标下标移动专栏并重建根级顺序")
	void moveByIndexReordersSameLevelInOneServiceCall() {
		when(columnMapper.findById(2L)).thenReturn(column(2L, 0L));
		when(columnMapper.findSiblingIds(0L, 2L)).thenReturn(Arrays.asList(1L, 3L));
		when(columnMapper.move(anyLong(), eq(0L), anyInt())).thenReturn(1);

		service.move(2L, 0L, null, 0);

		InOrder order = inOrder(columnMapper);
		order.verify(columnMapper).move(2L, 0L, 10);
		order.verify(columnMapper).move(1L, 0L, 20);
		order.verify(columnMapper).move(3L, 0L, 30);
		verify(columnMapper).findSiblingIds(0L, 2L);
	}

	@Test
	@DisplayName("跨父级拖拽：子专栏移入新父级后按目标下标排序")
	void moveByIndexReordersTargetLevelWhenChangingParent() {
		when(columnMapper.findById(3L)).thenReturn(column(3L, 1L));
		when(columnMapper.findById(2L)).thenReturn(column(2L, 0L));
		when(columnMapper.countChildren(3L)).thenReturn(0);
		when(columnMapper.findSiblingIds(2L, 3L)).thenReturn(Arrays.asList(4L, 5L));
		when(columnMapper.move(anyLong(), eq(2L), anyInt())).thenReturn(1);

		service.move(3L, 2L, null, 1);

		InOrder order = inOrder(columnMapper);
		order.verify(columnMapper).move(4L, 2L, 10);
		order.verify(columnMapper).move(3L, 2L, 20);
		order.verify(columnMapper).move(5L, 2L, 30);
	}

	@Test
	@DisplayName("父级拖拽：一级专栏可移动到根目录末尾")
	void moveParentToEndKeepsRootOrderCorrect() {
		when(columnMapper.findById(1L)).thenReturn(column(1L, 0L));
		when(columnMapper.findSiblingIds(0L, 1L)).thenReturn(Arrays.asList(2L, 3L));
		when(columnMapper.move(anyLong(), eq(0L), anyInt())).thenReturn(1);

		service.move(1L, 0L, null, 2);

		InOrder order = inOrder(columnMapper);
		order.verify(columnMapper).move(2L, 0L, 10);
		order.verify(columnMapper).move(3L, 0L, 20);
		order.verify(columnMapper).move(1L, 0L, 30);
	}

	@Test
	@DisplayName("子级拖拽：子专栏在同一父级内可移动到末尾")
	void moveChildWithinSameParentKeepsChildOrderCorrect() {
		when(columnMapper.findById(12L)).thenReturn(column(12L, 1L));
		when(columnMapper.findById(1L)).thenReturn(column(1L, 0L));
		when(columnMapper.countChildren(12L)).thenReturn(0);
		when(columnMapper.findSiblingIds(1L, 12L)).thenReturn(Arrays.asList(11L, 13L));
		when(columnMapper.move(anyLong(), eq(1L), anyInt())).thenReturn(1);

		service.move(12L, 1L, null, 2);

		InOrder order = inOrder(columnMapper);
		order.verify(columnMapper).move(11L, 1L, 10);
		order.verify(columnMapper).move(13L, 1L, 20);
		order.verify(columnMapper).move(12L, 1L, 30);
	}

	@Test
	@DisplayName("层级提升：子专栏可移动到根目录的指定位置")
	void promoteChildToRootAtRequestedPosition() {
		when(columnMapper.findById(12L)).thenReturn(column(12L, 1L));
		when(columnMapper.findSiblingIds(0L, 12L)).thenReturn(Arrays.asList(1L, 2L));
		when(columnMapper.move(anyLong(), eq(0L), anyInt())).thenReturn(1);

		service.move(12L, 0L, null, 1);

		InOrder order = inOrder(columnMapper);
		order.verify(columnMapper).move(1L, 0L, 10);
		order.verify(columnMapper).move(12L, 0L, 20);
		order.verify(columnMapper).move(2L, 0L, 30);
	}

	@Test
	@DisplayName("层级下降：无子节点的一级专栏可移入另一个一级专栏")
	void demoteLeafRootIntoAnotherRootAtRequestedPosition() {
		when(columnMapper.findById(2L)).thenReturn(column(2L, 0L));
		when(columnMapper.findById(1L)).thenReturn(column(1L, 0L));
		when(columnMapper.countChildren(2L)).thenReturn(0);
		when(columnMapper.findSiblingIds(1L, 2L)).thenReturn(Arrays.asList(11L, 12L));
		when(columnMapper.move(anyLong(), eq(1L), anyInt())).thenReturn(1);

		service.move(2L, 1L, null, 1);

		InOrder order = inOrder(columnMapper);
		order.verify(columnMapper).move(11L, 1L, 10);
		order.verify(columnMapper).move(2L, 1L, 20);
		order.verify(columnMapper).move(12L, 1L, 30);
	}

	@Test
	@DisplayName("层级保护：有子节点的一级专栏不能降级以避免产生三级结构")
	void rejectDemotingRootThatStillHasChildren() {
		when(columnMapper.findById(2L)).thenReturn(column(2L, 0L));
		when(columnMapper.findById(1L)).thenReturn(column(1L, 0L));
		when(columnMapper.countChildren(2L)).thenReturn(1);

		assertThrows(BadRequestException.class, () -> service.move(2L, 1L, null, 0));

		verify(columnMapper, never()).findSiblingIds(anyLong(), anyLong());
		verify(columnMapper, never()).move(anyLong(), anyLong(), anyInt());
	}

	@Test
	@DisplayName("兼容性：未传目标下标时仍按 targetSort 执行单条更新")
	void legacyTargetSortMoveRemainsSingleUpdate() {
		when(columnMapper.findById(2L)).thenReturn(column(2L, 0L));
		when(columnMapper.move(2L, 0L, 35)).thenReturn(1);

		service.move(2L, 0L, 35, null);

		verify(columnMapper).findById(2L);
		verify(columnMapper).move(2L, 0L, 35);
		verify(columnMapper, never()).findSiblingIds(anyLong(), anyLong());
		verifyNoMoreInteractions(columnMapper);
	}

	private BlogColumn column(Long id, Long parentId) {
		BlogColumn column = new BlogColumn();
		column.setId(id);
		column.setParentId(parentId);
		return column;
	}
}
