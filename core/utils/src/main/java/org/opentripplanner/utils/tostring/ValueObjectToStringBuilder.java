package org.opentripplanner.utils.tostring;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Use this to-string-builder to build value objects. A [ValueObject](http://wiki.c2.com/?ValueObject)
 * is usually a small object/class with a few fields. We want the {@code toString()} to be small and
 * easy to read. The text should be short and without class and field name prefixes.
 * <p>
 * Examples:
 * <pre>
 * - Money:  "5 kr", "USD 100"
 * - Time:   "2020-01-15", "3h3m5s", "14:23:59"
 * - Coordinate:  "(60.23451, 10.345678)"
 * </pre>
 * <p>
 * {@code ClassName{field1:value, field2:value, ..., NOT-SET:[fieldX, ...]}}
 * <p>
 * Use the {@link #of()}  factory method to create a instance of this class.
 */
public class ValueObjectToStringBuilder {

  private static final String FIELD_SEPARATOR = " ";

  private final StringBuilder sb = new StringBuilder();
  private final OtpNumberFormat numFormat = new OtpNumberFormat();

  boolean skipSep = true;
  boolean skipNull = false;

  /** Use factory method: {@link #of()}. */
  private ValueObjectToStringBuilder() {}

  /**
   * Create a new toString builder for a [ValueObject](http://wiki.c2.com/?ValueObject) type. The
   * builder will NOT include metadata(class and field names) when building the string.
   */
  public static ValueObjectToStringBuilder of() {
    return new ValueObjectToStringBuilder();
  }

  /* General purpose formatters */

  /**
   * {@code null} values are skipped after this method is called. This is the default behavior.
   */
  public ValueObjectToStringBuilder skipNull() {
    this.skipNull = true;
    return this;
  }

  /**
   * Use {@link #skipNull()} and {@code skipNull(false)} to turn the skip flag on and off. Example:
   *
   * <pre>
   * ValueObjectToStringBuilder.of()
   *   .skipNull().addNum(null).addText("?")
   *   .includeNull().addNum(null).addText("!")
   *
   * is "?null!"
   * </pre>
   */
  public ValueObjectToStringBuilder includeNull() {
    this.skipNull = false;
    return this;
  }

  public ValueObjectToStringBuilder addNum(Number num) {
    return addIt(num, numFormat::formatNumber);
  }

  public ValueObjectToStringBuilder addNum(Number num, String unit) {
    return addIt(num, it -> numFormat.formatNumber(it, unit));
  }

  public ValueObjectToStringBuilder addBool(Boolean value, String ifTrue, String ifFalse) {
    return addIt(value, it -> it ? ifTrue : ifFalse);
  }

  /** Add a quoted string value */
  public ValueObjectToStringBuilder addStr(String value) {
    return addIt(value, it -> "'" + it + "'");
  }

  /**
   * Add plain text without quotes or any pending whitespace separator after it. Include white space
   * if you need it.
   */
  public ValueObjectToStringBuilder addText(String label) {
    sb.append(label);
    skipSep = true;
    return this;
  }

  public ValueObjectToStringBuilder addEnum(Enum<?> value) {
    return addIt(value, Enum::name);
  }

  public ValueObjectToStringBuilder addObj(Object obj) {
    return addIt(obj, Object::toString);
  }

  /* Special purpose formatters */

  /**
   * Add a Coordinate location: (longitude, latitude). The coordinate is printed with a precision of
   * 5 digits after the period. The precision level used in OTP is 7 digits, so 2 coordinates that
   * appear to be equal (by toString) might not be exactly equals.
   */
  public ValueObjectToStringBuilder addCoordinate(Number lat, Number lon) {
    if (skipNull && lat == null && lon == null) {
      return this;
    }
    return addIt(
      "(" + numFormat.formatCoordinate(lat) + ", " + numFormat.formatCoordinate(lon) + ")"
    );
  }

  /**
   * Add time in seconds since midnight. Format:  HH:mm:ss.
   */
  public ValueObjectToStringBuilder addServiceTime(int secondsPastMidnight) {
    // Use a NOT_SET value which is unlikely to be used
    return addServiceTime(secondsPastMidnight, -87_654_321);
  }

  /**
   * Add time in seconds since midnight. Format:  HH:mm:ss. Ignore if not set.
   */
  public ValueObjectToStringBuilder addServiceTime(int secondsPastMidnight, int notSet) {
    return addIt(TimeUtils.timeToStrCompact(secondsPastMidnight, notSet));
  }

  /**
   * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
   * seconds) is only added if they are not zero {@code 0}. This is the same format as the {@link
   * Duration#toString()}, but without the 'PT' prefix.
   */
  public ValueObjectToStringBuilder addDuration(Duration duration) {
    return addIt(duration, DurationUtils::durationToStr);
  }

  /**
   * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
   * seconds) is only added if they are not zero {@code 0}. This is the same format as the {@link
   * Duration#toString()}, but without the 'PT' prefix.
   */
  public ValueObjectToStringBuilder addDurationSec(Integer durationSeconds) {
    return addIt(durationSeconds, DurationUtils::durationToStr);
  }

  public ValueObjectToStringBuilder addTime(Instant time) {
    return addIt(time, Object::toString);
  }

  /**
   * Add a cost in the format $N, as in "transit seconds", not centi-seconds as used by Raptor.
   */
  public ValueObjectToStringBuilder addCost(Integer costSeconds) {
    return addIt(costSeconds, OtpNumberFormat::formatCost);
  }

  /**
   * Add a cost in the format $N.NN or $N (if decimals are zero). The cost is interoperated as
   * a generalized-cost like the cost used by Raptor in "centi-seconds"
   */
  public ValueObjectToStringBuilder addCostCenti(Integer costCentiSeconds) {
    return addIt(costCentiSeconds, OtpNumberFormat::formatCostCenti);
  }

  /**
   * Add a cost in the format $N.NNu or $Nu, where 'N' is the number and 'u' is the unit.
   */
  public ValueObjectToStringBuilder addCostCenti(Integer costCentiSeconds, String unit) {
    return addIt(costCentiSeconds, it -> OtpNumberFormat.formatCost(it, unit));
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  /* private methods */

  private ValueObjectToStringBuilder addIt(String value) {
    return addIt(value, it -> it);
  }

  private <T> ValueObjectToStringBuilder addIt(T value, Function<T, String> mapToString) {
    if (skipNull && value == null) {
      return this;
    }
    if (skipSep) {
      skipSep = false;
    } else {
      sb.append(FIELD_SEPARATOR);
    }
    sb.append(value == null ? "null" : mapToString.apply(value));
    return this;
  }
}
