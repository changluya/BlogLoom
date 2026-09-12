package top.naccl.exception;

/**
 * @Description: 持久化异常
 * @Author: changlu
 * @Date: 2026-09-13
 */

public class PersistenceException extends RuntimeException {
	public PersistenceException() {
	}

	public PersistenceException(String message) {
		super(message);
	}

	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
