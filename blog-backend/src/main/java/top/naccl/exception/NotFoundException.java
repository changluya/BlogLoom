package top.naccl.exception;

/**
 * @Description: 404异常
 * @Author: changlu
 * @Date: 2026-09-13
 */

public class NotFoundException extends RuntimeException {
	public NotFoundException() {
	}

	public NotFoundException(String message) {
		super(message);
	}

	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
