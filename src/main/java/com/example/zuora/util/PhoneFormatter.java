package com.example.zuora.util;

/**
 * Utility class for formatting phone numbers
 */
public class PhoneFormatter {

    /**
     * Formats a phone number to standard US format: (XXX) XXX-XXXX
     * Handles various input formats:
     * - 10 digits: 1234567890 -> (123) 456-7890
     * - 11 digits with leading 1: 11234567890 -> (123) 456-7890
     * - With country code: +11234567890 -> (123) 456-7890
     * - Already formatted: (123) 456-7890 -> (123) 456-7890
     *
     * @param phone The raw phone number string
     * @return Formatted phone number, or original string if formatting fails
     */
    public static String format(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");

        // Handle empty result after stripping
        if (digits.isEmpty()) {
            return phone;
        }

        // Remove leading 1 (country code) if present
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }

        // Format as (XXX) XXX-XXXX if 10 digits
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        }

        // Return original if not standard US format
        return phone;
    }

    /**
     * Checks if the phone number appears to be valid
     * Valid formats: 10 digits, or 11 digits starting with 1
     *
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }

        String digits = phone.replaceAll("[^0-9]", "");

        // Remove leading 1 if present
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }

        return digits.length() == 10;
    }
}