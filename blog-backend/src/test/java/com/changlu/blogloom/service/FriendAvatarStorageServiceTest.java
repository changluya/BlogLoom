package com.changlu.blogloom.service;

import com.changlu.blogloom.exception.BadRequestException;
import com.changlu.blogloom.service.storage.UploadChannel;
import com.changlu.blogloom.service.storage.UploadChannelManager;
import com.changlu.blogloom.service.storage.UploadedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FriendAvatarStorageServiceTest {
	private UploadChannel channel;
	private FriendAvatarStorageService service;

	@BeforeEach
	void setUp() {
		UploadChannelManager manager = mock(UploadChannelManager.class);
		channel = mock(UploadChannel.class);
		when(manager.getCurrentChannel()).thenReturn(channel);
		service = new FriendAvatarStorageService(manager);
	}

	@Test
	void shouldUploadToFriendDirectory() {
		MockMultipartFile file = new MockMultipartFile("file", "avatar.PNG", "image/png", new byte[]{1, 2, 3});
		when(channel.upload(any(byte[].class), eq("png"), eq("friend/12"), anyString()))
				.thenReturn(new UploadedResource("avatar.png", "friend/12/avatar.png", "/static/friend/12/avatar.png"));

		String url = service.save(12L, file);

		assertEquals("/static/friend/12/avatar.png", url);
		verify(channel).upload(any(byte[].class), eq("png"), eq("friend/12"), anyString());
	}

	@Test
	void shouldRejectUnsupportedFileType() {
		MockMultipartFile file = new MockMultipartFile("file", "avatar.svg", "image/svg+xml", new byte[]{1});
		assertThrows(BadRequestException.class, () -> service.save(12L, file));
	}
}
