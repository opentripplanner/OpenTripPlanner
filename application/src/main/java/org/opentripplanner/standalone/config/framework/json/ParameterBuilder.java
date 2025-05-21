package org.opentripplanner.standalone.config.framework.json;

import static org.opentripplanner.standalone.config.framework.json.ConfigType.BOOLEAN;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.COST_LINEAR_FUNCTION;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.DOUBLE;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.DURATION;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.FEED_SCOPED_ID;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.GRAM;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.INTEGER;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.LOCALE;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.LONG;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.OBJECT;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.REGEXP;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.STRING;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.TIME_PENALTY;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.TIME_ZONE;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.LocalDateUtils;

/**
 * TODO RT_AB: add Javadoc to clarify whether this is building a declarative representation of the
 *   parameter, or building a concrete key-value pair for a parameter in a config file being read
 *   at server startup, or both.
 */
public class ParameterBuilder {

  private static final Object UNDEFINED = new Object();

  /**
   * The node-info builder is used while parsing a configuration file to "build" information about
   * parameter "under construction". This meta information is used later, for example to generate
   * user documentation.
   */
  private final NodeInfoBuilder info = NodeInfo.of();

  /**
   * The parameter "under construction" belong to the {@code target} node-adaptor. The parameter
   * is stored on the target when it is build.
   */
  private final NodeAdapter target;

  /**
   * Sometimes we want to use a different default value in the documentation than in the
   * returned parameters. This is the case when the default value is derived from other
   * parameters, and the default value is just a fallback. In these cases we temporarily need to
   * keep the doc-default-value.
   */
  private Object docDefaultValue = UNDEFINED;

  public ParameterBuilder(NodeAdapter target, String paramName) {
    this.target = target;
    info.withName(paramName);
  }

  /** Add version where this parameter first was added. */
  public ParameterBuilder since(OtpVersion version) {
    this.info.withSince(version);
    return this;
  }

  /** Add documentation metadata. This can be used to generate a user documentation */
  public ParameterBuilder summary(String summary) {
    this.info.withSummary(summary);
    return this;
  }

  /** Add documentation detail description to a parameter. */
  public ParameterBuilder description(String description) {
    this.info.withDescription(description);
    return this;
  }

  public ParameterBuilder experimentalFeature() {
    this.info.withExperimentalFeature();
    return this;
  }

  /**
   * Add documentation for optional field with default value to a parameter.
   * <p>
   * <b>Note!</b> This is only required if the default value in the documentation should be
   * different from the value passed in with {@code #asNnnn(defaultValue)}. This may be the case if
   * the value passed to {@code #asNnnn(defaultValue)} is derived from other parameters and the
   * <em>code</em> default is just a fallback.
   * <p>
   * If the given default value is not of type String, then {@code String.valueOf(defaultValue)}
   * is applied, there is
   */
  public ParameterBuilder docDefaultValue(Object defaultValue) {
    this.docDefaultValue = defaultValue;
    return this;
  }

  /** @throws OtpAppException if parameter is missing. */
  public Boolean asBoolean() {
    return ofRequired(BOOLEAN).asBoolean();
  }

  public Boolean asBoolean(boolean defaultValue) {
    return ofOptional(BOOLEAN, defaultValue, JsonNode::asBoolean);
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

  public Gram asGram(Gram defaultValue) {
    return Gram.of(ofOptional(GRAM, defaultValue.toString(), JsonNode::asText));
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

  public Set<String> asStringSet(Collection<String> defaultValue) {
    List<String> dft = (defaultValue instanceof List<String>)
      ? (List<String>) defaultValue
      : List.copyOf(defaultValue);
    return Set.copyOf(ofArrayAsList(STRING, dft, JsonNode::asText));
  }

  public List<String> asStringList(Collection<String> defaultValue) {
    List<String> dft = (defaultValue instanceof List<String>)
      ? (List<String>) defaultValue
      : List.copyOf(defaultValue);
    return ofArrayAsList(STRING, dft, JsonNode::asText);
  }

  public Map<String, String> asStringMap() {
    return ofOptionalMap(STRING, JsonNode::asText);
  }

  public NodeAdapter asObject() {
    info.withOptional().withType(OBJECT);
    return buildObject();
  }

  /**
   * Return a list with objects of type {@code T}. The given {@code mapper} is used to map
   * the nested child JSON nodes into elements.
   * <p>
   * An empty list is returned if there is no elements in the list or the list is not present.
   */
  public <T> List<T> asObjects(Function<NodeAdapter, T> mapper) {
    return asObjects(List.of(), mapper);
  }

  public <T> List<T> asObjects(List<T> defaultValues, Function<NodeAdapter, T> mapper) {
    setInfoOptional(defaultValues);
    info.withArray(OBJECT);
    return buildAndListComplexArrayElements(defaultValues, mapper);
  }

  /**
   * @deprecated Avoid using required enum types, when adding/removing new Enum values
   *             you potentially break forward/backward compatibility. If the new enum
   *             value is used in the config, an earlier version of OTP can not read
   *             the required value.
   */
  @Deprecated
  public <T extends Enum<T>> T asEnum(Class<T> enumType) {
    info.withRequired().withEnum(enumType);
    // throws exception if enum value is missing
    return parseRequiredEnum(build().asText(), enumType);
  }

  /** Get optional enum value. Parser is not case sensitive. */
  @SuppressWarnings("unchecked")
  public <T extends Enum<T>> T asEnum(T defaultValue) {
    info.withEnum((Class<? extends Enum<?>>) defaultValue.getClass());
    setInfoOptional(defaultValue);

    var node = build();

    if (node.isMissingNode()) {
      return defaultValue;
    }
    return parseOptionalEnum(node.asText(), (Class<T>) defaultValue.getClass()).orElse(
      defaultValue
    );
  }

  public <T extends Enum<T>> Set<T> asEnumSet(Class<T> enumClass) {
    info.withOptional().withEnumSet(enumClass);
    List<Optional<T>> optionalList = buildAndListSimpleArrayElements(List.of(), it ->
      parseOptionalEnum(it.asText(), enumClass)
    );
    List<T> result = optionalList.stream().filter(Optional::isPresent).map(Optional::get).toList();
    // Set is immutable
    return result.isEmpty() ? Set.of() : Set.copyOf(result);
  }

  public <T extends Enum<T>> Set<T> asEnumSet(Class<T> enumClass, Collection<T> defaultValues) {
    List<T> dft = (defaultValues instanceof List<T>)
      ? (List<T>) defaultValues
      : List.copyOf(defaultValues);
    info.withOptional(dft.toString()).withEnumSet(enumClass);
    List<Optional<T>> optionalList = buildAndListSimpleArrayElements(List.of(), it ->
      parseOptionalEnum(it.asText(), enumClass)
    );
    List<T> result = optionalList.stream().filter(Optional::isPresent).map(Optional::get).toList();
    // Set is immutable
    return result.isEmpty() ? Set.copyOf(dft) : Set.copyOf(result);
  }

  /**
   * Get a map of enum values listed in the config like this: (This example has Boolean values)
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
    info.withOptional().withEnumMap(enumType, elementType);

    var mapNode = buildObject();

    if (mapNode.isEmpty()) {
      return Map.of();
    }
    EnumMap<E, T> result = new EnumMap<>(enumType);

    Iterator<String> it = mapNode.listExistingChildNodes();
    while (it.hasNext()) {
      String name = it.next();
      Optional<E> key = parseOptionalEnum(name, enumType);
      if (key.isPresent()) {
        var child = mapNode.rawNode(name);
        result.put(key.get(), parseConfigType(elementType, child));
      }
    }
    return result;
  }

  /**
   * Add a map of a custom type with an enum as the key. You must provide a mapper for the
   * custom type.
   *
   * @param <E>  The enum type
   * @param <T>  The map value type.
   * @return a map of T by enum values as keys, or an empty map if not set.
   */
  public <T, E extends Enum<E>> Map<E, T> asEnumMap(
    Class<E> enumType,
    Function<NodeAdapter, T> typeMapper,
    Map<E, T> defaultValue
  ) {
    info.withOptional().withEnumMap(enumType, OBJECT);

    var mapNode = buildObject();

    if (mapNode.isEmpty()) {
      return defaultValue;
    }
    EnumMap<E, T> result = new EnumMap<>(enumType);

    Iterator<String> it = mapNode.listExistingChildNodes();
    while (it.hasNext()) {
      String name = it.next();
      Optional<E> key = parseOptionalEnum(name, enumType);
      if (key.isPresent()) {
        var child = mapNode.pathUndocumentedChild(name, info.since());
        result.put(key.get(), typeMapper.apply(child));
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
  public <T> T asCustomStringType(
    T defaultValue,
    String defaultValueAsString,
    Function<String, T> mapper
  ) {
    return ofOptional(
      STRING,
      defaultValue,
      node -> mapper.apply(node.asText()),
      it -> defaultValueAsString
    );
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
   * Accepts both a string-formatted duration or a number of seconds as a number.
   * In the documentation it will claim that it only accepts durations as the number is only for
   * backwards compatibility.
   */
  public Duration asDurationOrSeconds(Duration defaultValue) {
    info.withType(DURATION);
    setInfoOptional(defaultValue.toString());
    var node = build();
    if (node.isTextual()) {
      return asDuration(defaultValue);
    } else {
      return Duration.ofSeconds((long) asDouble(defaultValue.toSeconds()));
    }
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

  /** Required URI, OTP support a limited set of URIs. */
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
    return ofOptional(TIME_ZONE, defaultValue, n -> parseZoneId(n.asText()));
  }

  /* Custom OTP types */

  public FeedScopedId asFeedScopedId(FeedScopedId defaultValue) {
    return exist() ? FeedScopedId.parse(ofType(FEED_SCOPED_ID).asText()) : defaultValue;
  }

  public List<FeedScopedId> asFeedScopedIds(List<FeedScopedId> defaultValues) {
    setInfoOptional(defaultValues);
    info.withArray(FEED_SCOPED_ID);
    return buildAndListSimpleArrayElements(defaultValues, it -> FeedScopedId.parse(it.asText()));
  }

  public CostLinearFunction asCostLinearFunction(CostLinearFunction defaultValue) {
    return ofOptional(COST_LINEAR_FUNCTION, defaultValue, n -> CostLinearFunction.of(n.asText()));
  }

  public TimePenalty asTimePenalty(TimePenalty defaultValue) {
    return ofOptional(TIME_PENALTY, defaultValue, n -> TimePenalty.of(n.asText()));
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

  private <T> T ofOptional(
    ConfigType type,
    T defaultValue,
    Function<JsonNode, T> body,
    Function<T, String> toText
  ) {
    setInfoOptional(defaultValue == null ? null : toText.apply(defaultValue));
    info.withType(type);
    // Do not inline the build() call, if not called the metadata is not saved.
    var node = build();
    return exist() ? body.apply(node) : defaultValue;
  }

  private <T> T ofOptional(ConfigType type, T defaultValue, Function<JsonNode, T> body) {
    return ofOptional(type, defaultValue, body, String::valueOf);
  }

  private <T> T ofOptionalString(
    ConfigType type,
    String defaultValueAsString,
    Function<String, T> mapper
  ) {
    setInfoOptional(defaultValueAsString);
    info.withType(type);
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
    setInfoOptional(defaultValue);
    info.withArray(elementType);
    return buildAndListSimpleArrayElements(defaultValue, mapper);
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
    this.docDefaultValue = null;
    return target.addAndValidateParameterNode(info.build());
  }

  private NodeAdapter buildObject() {
    JsonNode node = build();
    return target.path(paramName(), node);
  }

  /**
   * Build node info for "simple" element types(JSON leafs) and list all values. Use
   * {@link #buildAndListComplexArrayElements(List, Function)} for building array with complex
   * elements.
   */
  private <T> List<T> buildAndListSimpleArrayElements(
    List<T> defaultValues,
    Function<JsonNode, T> mapper
  ) {
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

  /**
   * Build node info for "complex" element types and list all values. Use
   * {@link #buildAndListSimpleArrayElements(List, Function)} for building an array with simple
   * elements.
   */
  private <T> List<T> buildAndListComplexArrayElements(
    List<T> defaultValues,
    Function<NodeAdapter, T> parse
  ) {
    var array = build();

    if (array.isMissingNode()) {
      return defaultValues;
    }
    if (!array.isArray()) {
      throw error("The parameter is not a JSON array as expected.");
    }
    List<T> values = new ArrayList<>();
    int i = 0;

    var arrayAdaptor = target.path(paramName(), array);

    for (JsonNode node : array) {
      var element = arrayAdaptor.path("[" + i + "]", node);
      values.add(parse.apply(element));
      ++i;
    }
    return values;
  }

  private void setInfoOptional(Object realDefaultValue) {
    info.withOptional(getDocDefaultValue(realDefaultValue));
  }

  private String getDocDefaultValue(Object realDefaultValue) {
    Object value = docDefaultValue == UNDEFINED ? realDefaultValue : docDefaultValue;

    if (value == null) {
      return null;
    }
    if (value instanceof List list && list.isEmpty()) {
      return "[]";
    }
    if (value instanceof Enum enumValue) {
      return EnumMapper.toString(enumValue);
    }
    return String.valueOf(value);
  }

  /* parse/map from string to a specific type, handle parse error */

  private <T> T parseConfigType(ConfigType elementType, JsonNode child) {
    try {
      return elementType.valueOf(child);
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

  private <E extends Enum<E>> E parseRequiredEnum(String value, Class<E> ofType) {
    return EnumMapper.mapToEnum(value, ofType).orElseThrow(() -> {
      throw error(
        "The parameter value '%s' is not legal. Expected one of %s.".formatted(
            value,
            List.of(ofType.getEnumConstants())
          )
      );
    });
  }

  private <E extends Enum<E>> Optional<E> parseOptionalEnum(String value, Class<E> ofType) {
    Optional<E> enumValue = EnumMapper.mapToEnum(value, ofType);
    if (enumValue.isEmpty()) {
      warning(
        "The enum value '%s' is not legal. Expected one of %s.".formatted(
            value,
            List.of(ofType.getEnumConstants())
          )
      );
      return Optional.empty();
    }
    return enumValue;
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

  /**
   * Somehow Java do not provide a parse method for parsing Locale on the standard form
   * {@code <Language>[_<country>[_<variant>]]}, so this little utility method does that.
   * The separator used should be underscore({@code '_'}), space({@code ' '}) or hyphen({@code '-'}).
   */
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

  private void warning(String message) {
    target.addWarning(message, paramName());
  }
}
