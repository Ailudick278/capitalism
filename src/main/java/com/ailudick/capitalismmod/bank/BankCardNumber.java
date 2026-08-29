package com.ailudick.capitalismmod.bank;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates realistic 19-digit bank card numbers with a valid Luhn check digit.
 */
public final class BankCardNumber {
    private BankCardNumber() {
    }

    /**
     * Generates a 19-digit card number: "6222" BIN prefix (China UnionPay debit),
     * 14 random digits, and a Luhn check digit as the final digit.
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder("6222");
        for (int i = 0; i < 14; i++) {
            sb.append((char) ('0' + ThreadLocalRandom.current().nextInt(10)));
        }
        sb.append(luhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    /** Formats a 19-digit card number as "6222-1234-5678-9012-345". */
    public static String format(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 19) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "-"
                + cardNumber.substring(4, 8) + "-"
                + cardNumber.substring(8, 12) + "-"
                + cardNumber.substring(12, 16) + "-"
                + cardNumber.substring(16, 19);
    }

    /** Computes the Luhn check digit for the given digits, assuming the check digit is the final (rightmost) position. */
    private static int luhnCheckDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(digits.length() - 1 - i) - '0';
            // The check digit itself is position 1 (from the right), so these digits start at position 2.
            int positionFromRight = i + 2;
            if (positionFromRight % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }
}
