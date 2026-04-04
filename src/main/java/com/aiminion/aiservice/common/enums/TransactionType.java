package com.aiminion.aiservice.common.enums;

public enum TransactionType {
	TOPUP(1),
	PURCHASE_MEMBER_LEVEL(2);

	private final int code;

	TransactionType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static TransactionType fromCode(Integer code) {
		if (code == null) {
			return null;
		}
		for (TransactionType value : values()) {
			if (value.code == code) {
				return value;
			}
		}
		throw new IllegalArgumentException("Unknown TransactionType code: " + code);
	}
}

