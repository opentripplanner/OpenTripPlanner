package org.opentripplanner.framework.model;

import static java.lang.Double.compare;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * A representation of the weight of something in grams. The precision is 1/100g.
 */
public final class Gram implements Serializable, Comparable<Gram> {

  public static final Gram ZERO = new Gram(0);

  // PAttern used to split number and unit with an optonal space in between
  private static final Pattern PATTERN = Pattern.compile("([\\d\\.]+) ?([kg]*)");
  private static final int PRECISION = 100;
  private static final int HALF_PRECISION = PRECISION / 2;
  private static final int KILO_GRAM_PRECISION = 1_000 * PRECISION;

  private final int value;

  private Gram(int value) {
    this.value = value;
  }

  private static Gram ofPrecisionDouble(double value) {
    int v = (int) Math.round(value);
    return v == 0 ? ZERO : new Gram(v);
  }

  private static Gram ofPrecisionInt(int value) {
    return value == 0 ? ZERO : new Gram(value);
  }

  public static Gram of(int value) {
    return ofPrecisionInt(value * PRECISION);
  }

  public static Gram of(double value) {
    return ofPrecisionDouble(value * PRECISION);
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

  public Gram multiply(int factor) {
    return new Gram(this.value * factor);
  }

  public Gram multiply(double factor) {
    return ofPrecisionDouble(this.value * factor);
  }

  public Gram dividedBy(int scalar) {
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
    if (value % PRECISION == 0) {
      return value / PRECISION + "g";
    }
    return asDouble() + "g";
  }

  public double asInt() {
    return (value + HALF_PRECISION) / PRECISION;
  }

  public double asDouble() {
    return (double) this.value / PRECISION;
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
      return ofPrecisionDouble(PRECISION * v);
    }
    throw new IllegalArgumentException("Parse error! Illegal gram value: '%s'".formatted(value));
  }
}
