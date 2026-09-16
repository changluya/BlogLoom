package com.changlu.blogloom.module.knowledge.service;

import com.changlu.blogloom.module.knowledge.domain.dto.KnowledgeDirectoryCreate;
import com.changlu.blogloom.module.knowledge.domain.vo.KnowledgeTreeNode;

import java.util.List;

public interface KnowledgeNodeService {
	List<KnowledgeTreeNode> getTree();
	KnowledgeTreeNode createDirectory(KnowledgeDirectoryCreate request);
	void rename(Long id, String name);
	void move(Long id, Long targetParentId, Integer targetSort);
	void deleteNode(Long id);
	void deleteNodes(List<Long> ids);
	int repairMissingBlogs();
}
