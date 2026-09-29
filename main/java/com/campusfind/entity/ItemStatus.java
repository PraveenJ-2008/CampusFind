package com.campusfind.entity;

public enum ItemStatus {
    FOUND,
    CLAIMED,
    RETURNED,
    AVAILABLE; // Retained for backward compatibility

    public static ItemStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        String val = value.trim().toUpperCase();
        if ("AVAILABLE".equals(val)) {
            return AVAILABLE;
        }
        return ItemStatus.valueOf(val);
    }
}
