package org.opentripplanner.netex.mapping.support;

import java.time.LocalDateTime;
import org.rutebanken.netex.model.ValidBetween;

/**
 * This helper class provides methods for checking if a period is valid now, in the past, or in the
 * future according to the current date.
 */
class ValidityHelper {

  private ValidityHelper() {}

  // No validity information treated as valid now
  static boolean isValidNow(ValidBetween validBetween) {
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

  static boolean isValidPast(ValidBetween validBetween) {
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

  static boolean isValidFuture(ValidBetween validBetween) {
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
