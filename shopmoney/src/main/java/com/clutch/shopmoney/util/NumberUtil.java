package com.clutch.shopmoney.util;

import java.text.DecimalFormat;

public final class NumberUtil {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###");

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

    public static String formatAmount(long amount) {
        return DECIMAL_FORMAT.format(amount);
    }
}
