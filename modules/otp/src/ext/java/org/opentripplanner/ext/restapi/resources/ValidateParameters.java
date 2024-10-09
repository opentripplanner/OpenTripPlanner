package org.opentripplanner.ext.restapi.resources;

import jakarta.ws.rs.BadRequestException;
import java.util.HashSet;
import java.util.Set;

class ValidateParameters {

  /** This is set to avoid repeating the same error message twice. */
  private final Set<String> errors = new HashSet<>();

  ValidateParameters positiveOrZero(String name, Double value) {
    return assertTrue(value >= 0, pad(name) + " is negative: " + value);
  }

  ValidateParameters withinBounds(String name, Double value, double min, double max) {
    notNull(name, value);
    if (value != null) {
      assertTrue(
        value >= min || value <= max,
        pad(name) + " is not within bounds [" + min + ", " + max + "]"
      );
    }
    return this;
  }

  ValidateParameters lessThan(String aName, Double aSmall, String bName, Double bBig) {
    notNull(aName, aSmall);
    notNull(bName, bBig);
    if (aSmall != null && bBig != null) {
      assertTrue(aSmall < bBig, pad(aName) + " in not less than " + pad(bName));
    }
    return this;
  }

  ValidateParameters notNull(String name, Object value) {
    return assertFalse(value == null, pad(name) + " is missing");
  }

  ValidateParameters assertTrue(boolean expr, String message) {
    return assertFalse(!expr, message);
  }

  ValidateParameters assertFalse(boolean expr, String message) {
    if (expr) {
      errors.add(message);
    }
    return this;
  }

  void validate() {
    if (errors.isEmpty()) {
      return;
    }
    throw new BadRequestException(String.join(", ", errors));
  }

  private String pad(String name) {
    return "'" + name + "'";
  }
}
