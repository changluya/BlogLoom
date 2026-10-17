package com.changlu.blogloom.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitBehaviorTest {
	@Test
	void shouldDistinguishPageViewsFromInteractions() {
		assertTrue(VisitBehavior.INDEX.isPageView());
		assertTrue(VisitBehavior.BLOG.isPageView());
		assertTrue(VisitBehavior.SEARCH.isPageView());
		assertFalse(VisitBehavior.CLICK_FRIEND.isPageView());
		assertFalse(VisitBehavior.LIKE_MOMENT.isPageView());
		assertFalse(VisitBehavior.CHECK_PASSWORD.isPageView());
	}
}
