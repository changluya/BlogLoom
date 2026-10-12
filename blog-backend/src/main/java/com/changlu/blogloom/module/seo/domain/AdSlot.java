package com.changlu.blogloom.module.seo.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdSlot {
	private String name;
	private String slot;
	private String position;
	private Boolean enabled;
	private String format;
	private String layout;
	private String layoutKey;
	private Boolean fullWidth;
	private String alignment;
}
