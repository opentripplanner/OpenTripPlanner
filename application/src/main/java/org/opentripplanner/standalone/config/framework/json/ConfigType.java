package org.opentripplanner.standalone.config.framework.json;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.text.MarkdownFormatter;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * These are the types we support in the NodeAdaptor
 */
public enum ConfigType {
  BOOLEAN(JsonType.basic, "This is the Boolean JSON type", "true", "false"),
  STRING(JsonType.string, "This is the String JSON type.", "This is a string!"),
  DOUBLE(JsonType.basic, "A decimal floating point _number_. 64 bit.", "3.15"),
  INTEGER(JsonType.basic, "A decimal integer _number_. 32 bit.", "1", "-7", "2100"),
  LONG(JsonType.basic, "A decimal integer _number_. 64 bit.", "-1234567890"),
  ENUM(JsonType.string, "A fixed set of string literals.", "RAIL", "BUS"),
  ENUM_MAP(
    JsonType.object,
    "List of key/value pairs, where the key is a enum and the value can be any given type.",
    "{ 'RAIL: 1.2, 'BUS': 2.3 }"
  ),
  ENUM_SET(JsonType.object, "List of enum string values", "[ 'RAIL', 'TRAM' ]"),
  LOCALE(
    JsonType.string,
    "_`Language[\\_country[\\_variant]]`_. A Locale object represents a specific " +
    "geographical, political, or cultural region. For more information see the [Java Locale]" +
    "(https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Locale.html).",
    "en_US",
    "nn_NO"
  ),
  DATE(JsonType.string, "Local date. The format is _YYYY-MM-DD_ (ISO-8601).", "2020-09-21"),
  DATE_OR_PERIOD(
    JsonType.string,
    "A _local date_, or a _period_ relative to today. The local date has the format " +
    "`YYYY-MM-DD` and the period has the format `PnYnMnD` or `-PnYnMnD` where `n` is a integer " +
    "number.",
    "P1Y",
    "-P3M2D",
    "P1D"
  ),
  DURATION(
    JsonType.string,
    "A _duration_ is a amount of time. The format is `PnDTnHnMnS` or `nDnHnMnS` where " +
    "`n` is a  integer number. The `D`(days), `H`(hours), `M`(minutes) and `S`(seconds) are not " +
    "case sensitive.",
    "3h",
    "2m",
    "1d5h2m3s",
    "-P2dT-1s"
  ),
  REGEXP(
    JsonType.string,
    "A regular expression pattern used to match a sting.",
    "$^",
    "gtfs",
    "\\w{3})-.*\\.xml"
  ),
  URI(
    JsonType.string,
    "An URI path to a resource like a file or a URL. Relative URIs are resolved relative " +
    "to the OTP base path.",
    "http://foo.bar/",
    "file:///Users/jon/local/file",
    "graph.obj"
  ),
  TIME_ZONE(JsonType.string, "Time-Zone ID", "UTC", "Europe/Paris", "-05:00"),
  FEED_SCOPED_ID(JsonType.string, "FeedScopedId", "NO:1001", "1:101"),
  GRAM(
    JsonType.string,
    "Weight in grams or kilograms. If no unit is specified the unit is assumed to be grams.",
    "0g",
    "170g",
    "1.7 kg"
  ),
  COST_LINEAR_FUNCTION(
    JsonType.string,
    """
      A cost-linear-function used to calculate a cost from another cost or time/duration.

      Given a function of time:
      ```
      f(t) = a + b * t
      ```
      then `a` is the constant time part, `b` is the time-coefficient, and `t` is the variable.
      If `a=0s` and `b=0.0`, then the cost is always `0`(zero).

      Examples: `0s + 2.5t`, `10m + 0t` and `1h5m59s + 9.9t`

      The `constant` must be 0 or a positive number or duration. The unit is seconds unless
      specified using the duration format. A duration is automatically converted to a cost.
      The `coefficient` must be in range: [0.0, 100.0]
    """
  ),
  TIME_PENALTY(
    JsonType.string,
    """
      A time-penalty is used to add a penalty to the duration/arrival-time/departure-time for
      a path. It will be invisible to the end user, but used during the routing when comparing
      stop-arrival/paths.

      Given a function of time:
      ```
      f(t) = a + b * t
      ```
      then `a` is the constant time part, `b` is the time-coefficient, and `t` is the variable.
      If `a=0s` and `b=0.0`, then the cost is always `0`(zero).

      Examples: `0s + 2.5t`, `10m + 0 x` and `1h5m59s + 9.9t`

      The `constant` must be 0 or a positive number (seconds) or a duration.
      The `coefficient` must be in range: [0.0, 100.0]
    """
  ),
  MAP(
    JsonType.object,
    "List of key/value pairs, where the key is a string and the value can be any given type.",
    "{ 'one': 1.2, 'two': 2.3 }"
  ),
  OBJECT(
    JsonType.object,
    "Config object containing nested elements",
    "'walk': { 'speed': 1.3, 'reluctance': 5 }"
  ),
  ARRAY(
    JsonType.array,
    "Config object containing an array/list of elements",
    "'array': [ 1, 2, 3 ]"
  );

  private final JsonType type;
  private final String description;
  private final String[] examples;

  ConfigType(JsonType type, String description, String... examples) {
    this.type = type;
    this.description = description;
    this.examples = examples;
  }

  public String description() {
    return description;
  }

  public String examplesToMarkdown() {
    return Arrays.stream(examples)
      .map(StringUtils::quoteReplace)
      .map(this::quote)
      .map(MarkdownFormatter::code)
      .collect(Collectors.joining(", "));
  }

  public String docName() {
    return EnumMapper.toString(this);
  }

  /**
   * Quote the given {@code value} is the JSON type is a {@code string}.
   */
  public String quote(Object value) {
    return type == JsonType.string ? quoteText(value) : value.toString();
  }

  /**
   * Return {@code true} if the JSON type is {@code basic} or {@code string}.
   */
  public boolean isSimple() {
    return type == JsonType.basic || type == JsonType.string;
  }

  /**
   * Return {@code true} if the JSON type is {@code object} or {@code array}.
   */
  public boolean isComplex() {
    return type == JsonType.object || type == JsonType.array;
  }

  /**
   * Return {@code true} for all map and array types: {@code ARRAY}, {@code MAP}, and
   * {@code ENUM_MAP}.
   */
  public boolean isMapOrArray() {
    return EnumSet.of(ARRAY, MAP, ENUM_MAP).contains(this);
  }

  /** Internal to this class */
  private enum JsonType {
    basic,
    string,
    object,
    array,
  }

  /**
   * Return the {@link ConfigType} matching the Java type. This method only support the
   * basic and string types like String and Integer. Do not use this for {@link #isComplex()} types.
   */
  static ConfigType of(Class<?> javaType) {
    if (Boolean.class.isAssignableFrom(javaType)) {
      return BOOLEAN;
    }
    if (Double.class.isAssignableFrom(javaType)) {
      return DOUBLE;
    }
    if (Duration.class.isAssignableFrom(javaType)) {
      return DURATION;
    }
    if (Integer.class.isAssignableFrom(javaType)) {
      return INTEGER;
    }
    if (Long.class.isAssignableFrom(javaType)) {
      return LONG;
    }
    if (String.class.isAssignableFrom(javaType)) {
      return STRING;
    }
    throw new IllegalArgumentException("Type not supported: " + javaType);
  }

  /**
   * Get basic and string type value of given {@code node}. The "type-safe" {@link JsonNode}
   * methods are used. This method should not be used with {@link #isComplex()} types.
   */
  @SuppressWarnings("unchecked")
  <T> T valueOf(JsonNode node) {
    return switch (this) {
      case BOOLEAN -> (T) (Boolean) node.asBoolean();
      case DOUBLE -> (T) (Double) node.asDouble();
      case INTEGER -> (T) (Integer) node.asInt();
      case LONG -> (T) (Long) node.asLong();
      case STRING -> (T) node.asText();
      case DURATION -> (T) DurationUtils.duration(node.asText());
      default -> throw new IllegalArgumentException("Unsupported element type: " + this);
    };
  }

  /**
   * Return the given input formatted as a quoted text.
   */
  private static String quoteText(@Nullable Object text) {
    return text == null ? "" : "\"" + text + "\"";
  }
}
