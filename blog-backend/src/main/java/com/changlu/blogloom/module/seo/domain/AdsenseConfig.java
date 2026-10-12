package com.changlu.blogloom.module.seo.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AdsenseConfig {
	private Integer version;
	private Boolean enabled;
	private String client;
	private Boolean autoAds;
	private String adsTxt;
	private List<AdSlot> slots = new ArrayList<>();
}
