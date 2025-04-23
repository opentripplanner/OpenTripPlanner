package org.opentripplanner.framework.model;

import static java.lang.Double.compare;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * A representation of the weight of something in grams. The precision is 1 mg, if needed
 * it should be possible to refactor this to support micrograms.
 */
public final class Gram implements Serializable, Comparable<Gram> {

  public static final Gram ZERO = new Gram(0);

  // PAttern used to split number and unit with an optonal space in between
  private static final Pattern PATTERN = Pattern.compile("([\\d\\.]+) ?([kmg]*)");
  private static final int GRAM_PRECISION = 1_000;
  private static final int KILO_GRAM_PRECISION = 1_000 * GRAM_PRECISION;

  private final long value;

  private Gram(long value) {
    this.value = value;
  }

  private static Gram ofPrecisionDouble(double value) {
    long v = Math.round(value);
    return v == 0 ? ZERO : new Gram(v);
  }

  private static Gram ofPrecisionInt(long value) {
    return value == 0 ? ZERO : new Gram(value);
  }

  public static Gram of(long value) {
    return ofPrecisionInt(value * GRAM_PRECISION);
  }

  public static Gram of(double value) {
    return ofPrecisionDouble(value * GRAM_PRECISION);
  }

  /**
   * Create a new Gram object based on the string input. An optional unit can be added.
   * 'g'(grams) and 'kg'(kilograms) is supported.
   * <p>
   * Format: {@code [Decimal number] ('g'|'kg')?}
   * <p>
   * Examples: {@code 0}, {@code 7g}, {@code 2.5kg} and {@code 1.1 kg}.
   */
  public static Gram of(String value) {
    return parse(value);
  }

  public static Gram ofNullable(Double value) {
    return value == null ? ZERO : of(value.doubleValue());
  }

  public Gram plus(Gram g) {
    return new Gram(this.value + g.value);
  }

  public Gram multiply(long factor) {
    return new Gram(this.value * factor);
  }

  public Gram multiply(double factor) {
    return ofPrecisionDouble(this.value * factor);
  }

  public Gram dividedBy(long scalar) {
    return ofPrecisionDouble(value / (double) scalar);
  }

  public Gram dividedBy(double scalar) {
    return ofPrecisionDouble(this.value / scalar);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (Gram) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(value);
  }

  @Override
  public int compareTo(Gram o) {
    return compare(value, o.value);
  }

  @Override
  public String toString() {
    if (value % KILO_GRAM_PRECISION == 0) {
      return value / KILO_GRAM_PRECISION + "kg";
    }
    if (value % GRAM_PRECISION == 0) {
      return value / GRAM_PRECISION + "g";
    }
    if (value > 1000) {
      return (value / 1000.0) + "g";
    }
    return value + "mg";
  }

  public double asDouble() {
    return (double) this.value / GRAM_PRECISION;
  }

  public boolean isZero() {
    return value == 0;
  }

  private static Gram parse(String value) {
    var m = PATTERN.matcher(value);
    if (!m.matches()) {
      throw new IllegalArgumentException("Parse error! Illegal gram value: '%s'".formatted(value));
    }
    var num = m.group(1);
    var unit = m.group(2);

    double v = Double.parseDouble(num);
    if ("kg".equalsIgnoreCase(unit)) {
      return ofPrecisionDouble(KILO_GRAM_PRECISION * v);
    }
    if ("g".equalsIgnoreCase(unit) || unit.isBlank()) {
      return ofPrecisionDouble(GRAM_PRECISION * v);
    }
    if ("mg".equalsIgnoreCase(unit)) {
      return ofPrecisionDouble(v);
    }
    throw new IllegalArgumentException("Parse error! Illegal gram value: '%s'".formatted(value));
  }
}
