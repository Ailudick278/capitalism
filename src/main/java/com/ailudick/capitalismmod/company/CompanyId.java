package com.ailudick.capitalismmod.company;

import java.util.concurrent.ThreadLocalRandom;

/** Generates and validates an 18-character, unified-social-credit-code style ID. */
public final class CompanyId {
    private static final String ALPHABET = "0123456789ABCDEFGHJKLMNPQRTUWXY";
    private static final int[] WEIGHTS = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};

    private CompanyId() {
    }

    public static String generate() {
        StringBuilder id = new StringBuilder(17);
        id.append('9').append('1');
        for (int i = 0; i < 15; i++) {
            id.append(ALPHABET.charAt(ThreadLocalRandom.current().nextInt(ALPHABET.length())));
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += ALPHABET.indexOf(id.charAt(i)) * WEIGHTS[i];
        }
        id.append(ALPHABET.charAt((31 - sum % 31) % 31));
        return id.toString();
    }

    public static boolean isValid(String id) {
        if (id == null || id.length() != 18) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int value = ALPHABET.indexOf(id.charAt(i));
            if (value < 0) {
                return false;
            }
            sum += value * WEIGHTS[i];
        }
        int check = ALPHABET.indexOf(id.charAt(17));
        return check >= 0 && check == (31 - sum % 31) % 31;
    }
}
