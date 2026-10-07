package com.changlu.blogloom.module.seo.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeoTocItem {
	private final String id;
	private final String title;
	private final int level;
}
