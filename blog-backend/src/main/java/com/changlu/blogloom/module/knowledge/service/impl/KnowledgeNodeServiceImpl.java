package com.changlu.blogloom.module.knowledge.service.impl;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.exception.NotFoundException;
import com.changlu.blogloom.module.knowledge.dao.KnowledgeNodeMapper;
import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeDirectoryCreate;
import com.changlu.blogloom.module.knowledge.domain.entity.KnowledgeNode;
import com.changlu.blogloom.module.knowledge.domain.enums.KnowledgeNodeType;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeTreeNode;
import com.changlu.blogloom.module.knowledge.service.KnowledgeNodeService;
import com.changlu.blogloom.service.BlogService;
import com.changlu.blogloom.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class KnowledgeNodeServiceImpl implements KnowledgeNodeService {
	private final KnowledgeNodeMapper mapper;
	private final BlogService blogService;
	private final CommentService commentService;

	public KnowledgeNodeServiceImpl(KnowledgeNodeMapper mapper, BlogService blogService, CommentService commentService) {
		this.mapper = mapper;
		this.blogService = blogService;
		this.commentService = commentService;
	}

	@Override
	public List<KnowledgeTreeNode> getTree() {
		List<KnowledgeNode> nodes = mapper.findAll();
		Map<Long, KnowledgeTreeNode> values = new LinkedHashMap<>();
		for (KnowledgeNode node : nodes) values.put(node.getId(), KnowledgeTreeNode.from(node));
		List<KnowledgeTreeNode> roots = new ArrayList<>();
		for (KnowledgeTreeNode node : values.values()) {
			if (Objects.equals(node.getParentId(), 0L) || !values.containsKey(node.getParentId())) roots.add(node);
			else values.get(node.getParentId()).getChildren().add(node);
		}
		return roots;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public KnowledgeTreeNode createDirectory(KnowledgeDirectoryCreate request) {
		Long parentId = normalizeParent(request.getParentId());
		String name = validateName(request.getName());
		validateDirectory(parentId);
		ensureUnique(parentId, name, KnowledgeNodeType.DIR.name(), null);
		KnowledgeNode node = new KnowledgeNode();
		node.setParentId(parentId);
		node.setName(name);
		node.setType(KnowledgeNodeType.DIR.name());
		node.setSort(mapper.findMaxSort(parentId) + 1);
		mapper.insert(node);
		return KnowledgeTreeNode.from(node);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void rename(Long id, String name) {
		KnowledgeNode node = requireNode(id);
		String value = validateName(name);
		ensureUnique(node.getParentId(), value, node.getType(), id);
		mapper.updateName(id, value);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void move(Long id, Long targetParentId, Integer targetSort) {
		KnowledgeNode node = requireNode(id);
		Long parentId = normalizeParent(targetParentId);
		validateDirectory(parentId);
		if (id.equals(parentId) || (KnowledgeNodeType.DIR.name().equals(node.getType()) && isDescendant(id, parentId))) {
			throw new BadRequestException("不能将目录移动到自身或子目录");
		}
		ensureUnique(parentId, node.getName(), node.getType(), id);
		mapper.move(id, parentId, mapper.findMaxSort(parentId) + 1);
		List<Long> siblings = mapper.findChildIds(parentId);
		siblings.remove(id);
		int index = targetSort == null ? siblings.size() : Math.max(0, Math.min(targetSort, siblings.size()));
		siblings.add(index, id);
		for (int i = 0; i < siblings.size(); i++) mapper.updateSort(siblings.get(i), i);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteNode(Long id) {
		deleteNodes(Collections.singletonList(id));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteNodes(List<Long> ids) {
		if (ids == null || ids.isEmpty()) throw new BadRequestException("请选择要删除的节点");
		List<KnowledgeNode> allNodes = mapper.findAll();
		Map<Long, KnowledgeNode> nodes = new HashMap<>();
		Map<Long, List<KnowledgeNode>> children = new HashMap<>();
		for (KnowledgeNode node : allNodes) {
			nodes.put(node.getId(), node);
			children.computeIfAbsent(node.getParentId(), ignored -> new ArrayList<>()).add(node);
		}
		Set<Long> selected = new LinkedHashSet<>(ids);
		if (selected.contains(null)) throw new BadRequestException("删除节点 ID 不能为空");
		for (Long id : selected) if (!nodes.containsKey(id)) throw new NotFoundException("知识库节点不存在: " + id);
		for (Long id : selected) {
			if (!hasSelectedAncestor(nodes.get(id), nodes, selected)) {
				deleteRecursively(nodes.get(id), children, new HashSet<>());
			}
		}
	}

	@Override
	public int repairMissingBlogs() { return mapper.backfillBlogs(); }

	private void deleteRecursively(KnowledgeNode node, Map<Long, List<KnowledgeNode>> children, Set<Long> visiting) {
		if (!visiting.add(node.getId())) throw new BadRequestException("知识库目录层级存在循环，无法删除");
		for (KnowledgeNode child : children.getOrDefault(node.getId(), Collections.emptyList())) {
			deleteRecursively(child, children, visiting);
		}
		if (KnowledgeNodeType.DOC.name().equals(node.getType()) && node.getBlogId() != null && mapper.countBlog(node.getBlogId()) > 0) {
			blogService.deleteBlogTagByBlogId(node.getBlogId());
			commentService.deleteCommentsByBlogId(node.getBlogId());
			blogService.deleteBlogById(node.getBlogId());
		} else {
			mapper.delete(node.getId());
		}
		visiting.remove(node.getId());
	}

	private boolean hasSelectedAncestor(KnowledgeNode node, Map<Long, KnowledgeNode> nodes, Set<Long> selected) {
		Set<Long> visited = new HashSet<>();
		Long parentId = node.getParentId();
		while (parentId != null && parentId != 0L && visited.add(parentId)) {
			if (selected.contains(parentId)) return true;
			KnowledgeNode parent = nodes.get(parentId);
			if (parent == null) return false;
			parentId = parent.getParentId();
		}
		return false;
	}

	private boolean isDescendant(Long id, Long target) {
		Long cursor = target;
		while (cursor != 0L) {
			if (id.equals(cursor)) return true;
			KnowledgeNode node = mapper.findById(cursor);
			if (node == null) return false;
			cursor = node.getParentId();
		}
		return false;
	}

	private KnowledgeNode requireNode(Long id) {
		KnowledgeNode node = mapper.findById(id);
		if (node == null) throw new NotFoundException("知识库节点不存在");
		return node;
	}

	private void validateDirectory(Long id) {
		if (id == 0L) return;
		KnowledgeNode node = requireNode(id);
		if (!KnowledgeNodeType.DIR.name().equals(node.getType())) throw new BadRequestException("目标节点不是目录");
	}

	private void ensureUnique(Long parentId, String name, String type, Long excludeId) {
		if (mapper.countSameName(parentId, name, type, excludeId) > 0) throw new BadRequestException("同级已存在同名节点");
	}

	private Long normalizeParent(Long id) { return id == null ? 0L : id; }

	private String validateName(String name) {
		if (name == null || name.trim().isEmpty()) throw new BadRequestException("节点名称不能为空");
		String value = name.trim();
		if (value.length() > 255 || value.contains("/") || value.contains("\\")) throw new BadRequestException("节点名称不合法");
		return value;
	}
}
