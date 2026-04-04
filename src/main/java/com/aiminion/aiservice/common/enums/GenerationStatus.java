package com.aiminion.aiservice.common.enums;

public enum GenerationStatus {
	PENDING(1),
	SUCCESS(2),
	FAILED(3);

	private final int code;

	GenerationStatus(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static GenerationStatus fromCode(Integer code) {
		if (code == null) {
			return null;
		}
		for (GenerationStatus value : values()) {
			if (value.code == code) {
				return value;
			}
		}
		throw new IllegalArgumentException("Unknown GenerationStatus code: " + code);
	}
}

