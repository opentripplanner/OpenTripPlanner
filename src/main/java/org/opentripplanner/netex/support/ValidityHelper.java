package org.opentripplanner.netex.support;

import org.rutebanken.netex.model.ValidBetween;

import java.time.LocalDateTime;

/**
 * This helper class provides methods for checking if a period is valid now, in the past, or in the future according
 * to the current date.
 */
public class ValidityHelper {
    private ValidityHelper() {}

    // No validity information treated as valid now
    public static boolean isValidNow(ValidBetween validBetween) {
        LocalDateTime now = LocalDateTime.now();
        if (validBetween != null) {
            if (validBetween.getFromDate() == null && validBetween.getToDate() == null) {
                return true;
            }

            if (validBetween.getFromDate() != null && validBetween.getFromDate().isAfter(now)) {
                return false;
            }

            if (validBetween.getToDate() != null && validBetween.getToDate().isBefore(now)) {
                return false;
            }

            return true;
        }
        return true;
    }

    public static boolean isValidPast(ValidBetween validBetween) {
        LocalDateTime now = LocalDateTime.now();
        if (validBetween != null) {
            if (validBetween.getToDate() == null) {
                return false;
            }

            if (validBetween.getToDate() != null && validBetween.getToDate().isAfter(now)) {
                return false;
            }

            return true;
        }
        return false;
    }

    public static boolean isValidFuture(ValidBetween validBetween) {
        LocalDateTime now = LocalDateTime.now();
        if (validBetween != null) {
            if (validBetween.getFromDate() == null) {
                return false;
            }

            if (validBetween.getFromDate() != null && validBetween.getFromDate().isBefore(now)) {
                return false;
            }

            return true;
        }
        return false;
    }
}