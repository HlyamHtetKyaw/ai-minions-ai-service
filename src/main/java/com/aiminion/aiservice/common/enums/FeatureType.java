package com.aiminion.aiservice.common.enums;

public enum FeatureType {
	TEXT(1),
	IMAGE(2),
	AUDIO(3),
	VIDEO(4);

	private final int code;

	FeatureType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static FeatureType fromCode(Integer code) {
		if (code == null) {
			return null;
		}
		for (FeatureType value : values()) {
			if (value.code == code) {
				return value;
			}
		}
		throw new IllegalArgumentException("Unknown FeatureType code: " + code);
	}
}

