package com.changlu.blogloom.exception;

/**
 * @Description: 非法请求异常
 * @Author: changlu
 * @Date: 2026-09-13
 */

public class BadRequestException extends RuntimeException {
	public BadRequestException() {
	}

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
