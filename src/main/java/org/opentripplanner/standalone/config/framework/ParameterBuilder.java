package org.opentripplanner.standalone.config.framework;

import static org.opentripplanner.standalone.config.framework.ConfigType.BOOLEAN;
import static org.opentripplanner.standalone.config.framework.ConfigType.DOUBLE;
import static org.opentripplanner.standalone.config.framework.ConfigType.DURATION;
import static org.opentripplanner.standalone.config.framework.ConfigType.FEED_SCOPED_ID;
import static org.opentripplanner.standalone.config.framework.ConfigType.INTEGER;
import static org.opentripplanner.standalone.config.framework.ConfigType.LINEAR_FUNCTION;
import static org.opentripplanner.standalone.config.framework.ConfigType.LOCALE;
import static org.opentripplanner.standalone.config.framework.ConfigType.LONG;
import static org.opentripplanner.standalone.config.framework.ConfigType.OBJECT;
import static org.opentripplanner.standalone.config.framework.ConfigType.REGEXP;
import static org.opentripplanner.standalone.config.framework.ConfigType.STRING;
import static org.opentripplanner.standalone.config.framework.ConfigType.ZONE_ID;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.OtpAppException;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.LocalDateUtils;

public class ParameterBuilder {

  /**
   * The node-info builder is used while parsing a configuration file to "build" information about
   * parameter "under construction". This meta information is used later, fr example to generate
   * user documentation.
   */
  private final NodeInfo.Builder info = NodeInfo.of();

  /**
   * The parameter "under construction" belong to the {@code target} node-adaptor. The parameter
   * is stored on the target when it is build.
   */
  private final NodeAdapter target;

  public ParameterBuilder(NodeAdapter target, String paramName) {
    this.target = target;
    info.withName(paramName);
  }

  /** Add documentation metadata. This can be used to generate a user documentation */
  public ParameterBuilder doc(OtpVersion since, String summary) {
    this.info.withSince(since).withSummary(summary);
    return this;
  }

  /** Add documentation detail description to a parameter. */
  public ParameterBuilder description(String description) {
    this.info.withDescription(description);
    return this;
  }

  /** Add documentation detail description to a parameter. */
  public ParameterBuilder example(Object example) {
    this.info.withExample(example);
    return this;
  }

  /** @throws OtpAppException if parameter is missing. */
  public Boolean asBoolean() {
    return ofRequired(BOOLEAN).asBoolean();
  }

  public Boolean asBoolean(boolean defaultValue) {
    return ofOptional(BOOLEAN, defaultValue, JsonNode::asBoolean);
  }

  public Map<String, Boolean> asBooleanMap() {
    return ofOptionalMap(BOOLEAN, JsonNode::asBoolean);
  }

  /** @throws OtpAppException if parameter is missing. */
  public double asDouble() {
    return ofRequired(DOUBLE).asDouble();
  }

  public double asDouble(double defaultValue) {
    return ofOptional(DOUBLE, defaultValue, JsonNode::asDouble);
  }

  public Optional<Double> asDoubleOptional() {
    return Optional.ofNullable(ofOptional(DOUBLE, null, JsonNode::asDouble));
  }

  public List<Double> asDoubles(List<Double> defaultValue) {
    return ofArrayAsList(DOUBLE, defaultValue, JsonNode::asDouble);
  }

  /** @throws OtpAppException if parameter is missing. */
  public int asInt() {
    return ofRequired(INTEGER).asInt();
  }

  public int asInt(int defaultValue) {
    return ofOptional(INTEGER, defaultValue, JsonNode::asInt);
  }

  public long asLong(long defaultValue) {
    return ofOptional(LONG, defaultValue, JsonNode::asLong);
  }

  /** @throws OtpAppException if parameter is missing. */
  public String asString() {
    return ofRequired(STRING).asText();
  }

  public String asString(String defaultValue) {
    return ofOptional(STRING, defaultValue, JsonNode::asText);
  }

  public List<String> asStringList(List<String> defaultValue) {
    return ofArrayAsList(STRING, defaultValue, JsonNode::asText);
  }

  public Map<String, String> asStringMap() {
    return ofOptionalMap(STRING, JsonNode::asText);
  }

  public NodeAdapter asObject() {
    info.withOptional().withType(OBJECT);
    return buildObject();
  }

  public <T> List<T> asObjects(Function<NodeAdapter, T> mapper) {
    info.withRequired().withArray(OBJECT);
    return buildAndListObjects(List.of(), mapper);
  }

  public <T> List<T> asObjects(List<T> defaultValues, Function<NodeAdapter, T> mapper) {
    info.withOptional(defaultValues.isEmpty() ? "[]" : defaultValues.toString()).withArray(OBJECT);
    return buildAndListObjects(defaultValues, mapper);
  }

  public <T extends Enum<T>> T asEnum(Class<T> enumType) {
    //noinspection unchecked
    info.withRequired().withEnum((Class<Enum<?>>) enumType);
    return parseEnum(build().asText(), enumType);
  }

  /** Get optional enum value. Parser is not case sensitive. */
  @SuppressWarnings("unchecked")
  public <T extends Enum<T>> T asEnum(T defaultValue) {
    info.withEnum((Class<Enum<?>>) defaultValue.getClass()).withOptional(defaultValue.name());
    // Do not inline the node, calling the build is required.
    var node = build();
    return exist() ? parseEnum(node.asText(), (Class<T>) defaultValue.getClass()) : defaultValue;
  }

  public <T extends Enum<T>> Set<T> asEnumSet(Class<T> enumClass) {
    //noinspection unchecked
    info.withOptional().withEnumSet((Class<Enum<?>>) enumClass);
    List<T> result = buildAndListValues(List.of(), it -> parseEnum(it.asText(), enumClass));
    return result.isEmpty() ? EnumSet.noneOf(enumClass) : EnumSet.copyOf(result);
  }

  /**
   * Get a map of enum values listed in the config like this: (This example have Boolean values)
   * <pre>
   * key : {
   *   A : true,  // turned on
   *   B : false  // turned off
   *   // Commented out to use default value
   *   // C : true
   * }
   * </pre>
   *
   * @param <E>  The enum type
   * @param <T>  The map value type.
   * @return a map of listed enum values as keys with value, or an empty map if not set.
   */
  public <T, E extends Enum<E>> Map<E, T> asEnumMap(Class<E> enumType, Class<T> elementJavaType) {
    var elementType = ConfigType.of(elementJavaType);
    //noinspection unchecked
    info.withOptional().withEnumMap((Class<Enum<?>>) enumType, elementType);

    var mapNode = buildObject();

    if (mapNode.isEmpty()) {
      return Map.of();
    }

    EnumMap<E, T> result = new EnumMap<>(enumType);

    for (E v : enumType.getEnumConstants()) {
      var key = v.name();
      if (mapNode.exist(key)) {
        var child = mapNode.rawNode(key);
        result.put(v, parseConfigType(elementType, child));
      }
    }
    return result;
  }

  /**
   * Get a map of enum values listed in the config like the {@link #asEnumMap(Class, Class)}, but
   * verify that all enum keys are listed. This can be used for settings where there is no
   * appropriate default value. Note! This method return {@code null}, not an empty map, if the
   * given parameter is not present.
   */
  public <T, E extends Enum<E>> Map<E, T> asEnumMapAllKeysRequired(
    Class<E> enumType,
    Class<T> elementJavaType
  ) {
    Map<E, T> map = asEnumMap(enumType, elementJavaType);
    if (map.isEmpty()) {
      return null;
    }

    EnumSet<E> reminding = EnumSet.allOf(enumType);
    reminding.removeAll(map.keySet());

    if (!reminding.isEmpty()) {
      throw error("The following enum map keys are missing: %s".formatted(reminding));
    }
    return map;
  }

  /**
   * This method provide support for custom types. If the type is used in one place, then this
   * method provide an easy way to support it. Be aware that custom types are not documented in
   * the type section in the configuration documents. Also, providing user-friendly messages
   * is left to the caller.
   */
  public <T> T asCustomStingType(T defaultValue, Function<String, T> mapper) {
    return ofOptional(STRING, defaultValue, node -> mapper.apply(node.asText()));
  }

  /* Java util/time types */

  /**
   * Parses the sting using {@link LocalDateUtils#asRelativeLocalDate(String, LocalDate)} and
   * return a new (relative) LocalDate.
   */
  public LocalDate asDateOrRelativePeriod(String defaultValue, ZoneId timeZone) {
    return ofOptionalString(DURATION, defaultValue, s -> parseRelativeLocalDate(s, timeZone));
  }

  public Duration asDuration(Duration defaultValue) {
    return ofOptional(DURATION, defaultValue, node -> parseDuration(node.asText()));
  }

  public Duration asDuration() {
    return ofRequired(DURATION, node -> parseDuration(node.asText()));
  }

  /**
   * Parse int using given unit or as duration string. See {@link DurationUtils#duration(String)}.
   * This version can be used to be backwards compatible when moving from an integer value
   * to a duration.
   */
  public Duration asDuration2(Duration defaultValue, ChronoUnit unit) {
    return ofOptional(DURATION, defaultValue, node -> parseDuration(node.asText(), unit));
  }

  /**
   * Parse int using given unit or as duration string. See {@link DurationUtils#duration(String)}.
   * This version can be used to be backwards compatible when moving from an integer value
   * to a duration.
   */
  public Duration asDuration2(ChronoUnit unit) {
    return ofRequired(DURATION, node -> parseDuration(node.asText(), unit));
  }

  public List<Duration> asDurations(List<Duration> defaultValues) {
    return ofArrayAsList(DURATION, defaultValues, node -> parseDuration(node.asText()));
  }

  public Locale asLocale(Locale defaultValue) {
    return ofOptional(LOCALE, defaultValue, jsonNode -> parseLocale(jsonNode.asText()));
  }

  public Pattern asPattern(String defaultValue) {
    return ofOptionalString(REGEXP, defaultValue, Pattern::compile);
  }

  public URI asUri() {
    return ofRequired(ConfigType.URI, n -> parseUri(n.asText()));
  }

  public URI asUri(String defaultValue) {
    return ofOptionalString(ConfigType.URI, defaultValue, this::parseUri);
  }

  public List<URI> asUris() {
    return ofArrayAsList(ConfigType.URI, List.of(), n -> parseUri(n.asText()));
  }

  public ZoneId asZoneId(ZoneId defaultValue) {
    return ofOptional(ZONE_ID, defaultValue, n -> parseZoneId(n.asText()));
  }

  /* Custom OTP types */

  public FeedScopedId asFeedScopedId(FeedScopedId defaultValue) {
    return exist() ? FeedScopedId.parseId(ofType(FEED_SCOPED_ID).asText()) : defaultValue;
  }

  public List<FeedScopedId> asFeedScopedIds(List<FeedScopedId> defaultValues) {
    info.withOptional(defaultValues.toString()).withArray(FEED_SCOPED_ID);
    return buildAndListValues(defaultValues, it -> FeedScopedId.parseId(it.asText()));
  }

  public DoubleFunction<Double> asLinearFunction(DoubleFunction<Double> defaultValue) {
    return ofOptional(LINEAR_FUNCTION, defaultValue, n -> parseLinearFunction(n.asText()));
  }

  /* private method */

  private String paramName() {
    return info.name();
  }

  private boolean exist() {
    return target.exist(paramName());
  }

  private JsonNode ofType(ConfigType type) {
    info.withType(type);
    return build();
  }

  private <T> T ofType(ConfigType type, Function<JsonNode, T> body) {
    info.withType(type);
    return body.apply(build());
  }

  private JsonNode ofRequired(ConfigType type) {
    info.withRequired();
    return ofType(type);
  }

  private <T> T ofRequired(ConfigType type, Function<JsonNode, T> body) {
    info.withRequired();
    return ofType(type, body);
  }

  private <T> T ofOptional(ConfigType type, T defaultValue, Function<JsonNode, T> body) {
    info.withType(type).withOptional(defaultValue == null ? null : defaultValue.toString());
    // Do not inline the build() call, if not called the metadata is not saved.
    var node = build();
    return exist() ? body.apply(node) : defaultValue;
  }

  private <T> T ofOptionalString(
    ConfigType type,
    String defaultValueAsString,
    Function<String, T> mapper
  ) {
    info.withType(type).withOptional(defaultValueAsString);
    // Do not inline the build() call, if not called the metadata is not saved.
    var node = build();
    return exist()
      ? mapper.apply(node.asText())
      : (defaultValueAsString == null ? null : mapper.apply(defaultValueAsString));
  }

  private <T> List<T> ofArrayAsList(
    ConfigType elementType,
    List<T> defaultValue,
    Function<JsonNode, T> mapper
  ) {
    info.withOptional(String.valueOf(defaultValue)).withArray(elementType);
    return buildAndListValues(defaultValue, mapper);
  }

  private <T> Map<String, T> ofOptionalMap(ConfigType elementType, Function<JsonNode, T> mapper) {
    info.withOptional().withMap(elementType);
    var mapNode = buildObject();

    if (mapNode.isEmpty()) {
      return Map.of();
    }

    Map<String, T> result = new HashMap<>();

    Iterator<String> names = mapNode.parameterNames();
    while (names.hasNext()) {
      var parameterName = names.next();
      result.put(parameterName, mapper.apply(mapNode.rawNode(parameterName)));
    }
    return result;
  }

  private JsonNode build() {
    return target.addAndValidateParameterNode(info.build());
  }

  private NodeAdapter buildObject() {
    JsonNode node = target.addAndValidateParameterNode(info.build());
    return target.path(paramName(), node);
  }

  private <T> List<T> buildAndListValues(List<T> defaultValues, Function<JsonNode, T> mapper) {
    JsonNode array = build();
    if (array.isMissingNode()) {
      return defaultValues;
    }
    if (!array.isArray()) {
      throw error("The parameter is not a JSON array as expected.");
    }
    List<T> values = new ArrayList<>();
    for (JsonNode node : array) {
      values.add(mapper.apply(node));
    }
    return values;
  }

  private <T> List<T> buildAndListObjects(List<T> defaultValues, Function<NodeAdapter, T> parse) {
    var array = build();
    if (array.isMissingNode()) {
      return defaultValues;
    }
    if (!array.isArray()) {
      throw error("The parameter is not a JSON array as expected.");
    }
    List<T> values = new ArrayList<>();
    int i = 0;
    for (JsonNode node : array) {
      values.add(parse.apply(target.path("[" + i + "]", node)));
      ++i;
    }
    return values;
  }

  /* parse/map from string to a specific type, handle parse error */

  private <T> T parseConfigType(ConfigType elementType, JsonNode child) {
    try {
      return ConfigType.getParameter(elementType, child);
    } catch (Exception e) {
      throw error(
        "The parameter value '%s' is not of type %s.".formatted(
            child.asText(),
            elementType.docName()
          ),
        e
      );
    }
  }

  private <T extends Enum<T>> T parseEnum(String value, Class<T> ofType) {
    var upperCaseValue = value.toUpperCase();
    return Stream
      .of(ofType.getEnumConstants())
      .filter(it -> it.name().toUpperCase().equals(upperCaseValue))
      .findFirst()
      .orElseThrow(() -> {
        throw error(
          "The parameter value '%s' is not in legal. Expected one of %s.".formatted(
              value,
              List.of(ofType.getEnumConstants())
            )
        );
      });
  }

  private LocalDate parseRelativeLocalDate(String value, ZoneId zoneId) {
    try {
      return LocalDateUtils.asRelativeLocalDate(value, LocalDate.now(zoneId));
    } catch (DateTimeParseException e) {
      throw error("The parameter value '%s' is not a Period or LocalDate.".formatted(value), e);
    }
  }

  private Duration parseDuration(String value) {
    try {
      return DurationUtils.duration(value);
    } catch (DateTimeParseException e) {
      throw error("The parameter value '%s' is not a duration.".formatted(value), e);
    }
  }

  private Duration parseDuration(String value, ChronoUnit unit) {
    try {
      return DurationUtils.duration(value, unit);
    } catch (DateTimeParseException e) {
      throw error("The parameter value '%s' is not a duration.".formatted(value), e);
    }
  }

  private Locale parseLocale(String text) {
    String[] parts = text.split("[-_ ]+");
    return switch (parts.length) {
      case 1 -> new Locale(parts[0]);
      case 2 -> new Locale(parts[0], parts[1]);
      case 3 -> new Locale(parts[0], parts[1], parts[2]);
      default -> throw error(
        "The parameter is not a valid Locale: '" +
        text +
        "'. Use: <Language>[_<country>[_<variant>]]."
      );
    };
  }

  private DoubleFunction<Double> parseLinearFunction(String text) {
    try {
      return RequestFunctions.parse(text);
    } catch (IllegalArgumentException ignore) {
      throw error(
        "Unable to parse linear function: " +
        text +
        ". Expected format: " +
        "\"a + b x\" (\"2.0 + 7.1 x\")."
      );
    }
  }

  private URI parseUri(String text) {
    try {
      if (text == null || text.isBlank()) {
        return null;
      }
      return new URI(text);
    } catch (URISyntaxException e) {
      throw error(
        "Unable to parse URI parameter value '%s'. Not parsable by java.net.URI class.".formatted(
            text
          ),
        e
      );
    }
  }

  private ZoneId parseZoneId(String text) {
    try {
      return ZoneId.of(text);
    } catch (DateTimeException e) {
      throw error(
        "Unable to parse parameter value: '" +
        text +
        "'. Expected a value parsable by java.time.ZoneId class."
      );
    }
  }

  private OtpAppException error(String message) {
    return target.createException(message, paramName());
  }

  private OtpAppException error(String message, Exception e) {
    return target.createException(message, paramName(), e);
  }
}
