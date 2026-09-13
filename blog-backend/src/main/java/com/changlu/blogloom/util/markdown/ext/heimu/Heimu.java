package com.changlu.blogloom.util.markdown.ext.heimu;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Delimited;

/**
 * @Description: A heimu node containing text and other inline nodes nodes as children.
 * @Author: changlu
 * @Date: 2026-09-13
 */
public class Heimu extends CustomNode implements Delimited {
	private static final String DELIMITER = "@@";

	@Override
	public String getOpeningDelimiter() {
		return DELIMITER;
	}

	@Override
	public String getClosingDelimiter() {
		return DELIMITER;
	}
}
