package com.clutch.shopmoney.util;

public final class NumberUtil {
    private NumberUtil() {
    }

    public static Long parsePositiveLong(String input) {
        try {
            long value = Long.parseLong(input);
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
