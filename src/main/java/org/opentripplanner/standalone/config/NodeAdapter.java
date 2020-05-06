package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * This class wrap a {@link JsonNode} and decorate it with type-safe parsing
 * of types used in OTP like enums, date, time, URIs and so on. By wrapping
 * the JsonNode we get consistent parsing rules and the possibility to log unused
 * parameters when the end of parsing a file. Also the configuration POJOs become
 * cleaner because they do not have any parsing logic in them any more.
 * <p>
 * This class have 100% test coverage - keep it that way, for the individual configuration
 * POJOs a smoke test is good enough.
 */
public class NodeAdapter {

    private final JsonNode json;

    /**
     * The source is the origin of the configuration. The source can be "DEFAULT", the name of the
     * JSON source files or the "SerializedGraph".
     */
    private final String source;

    /**
     * This class wrap a {@link JsonNode} witch might be a child of another node. We
     * keep the path string for logging and debugging purposes
     */
    private final String contextPath;

    /**
     * This parameter is used internally in this class to be able to produce a
     * list of parameters witch is NOT requested.
     */
    private final List<String> parameterNames = new ArrayList<>();

    /**
     * The collection of children is used to be able to produce a list of unused parameters
     * for all children.
     */
    private final List<NodeAdapter> children = new ArrayList<>();

    public NodeAdapter(@NotNull JsonNode node, String source) {
        this(node, source, null);
    }

    /**
     * Constructor for nested configuration nodes.
     */
    private NodeAdapter(@NotNull JsonNode node, String source, String contextPath) {
        this.json = node;
        this.source = source;
        this.contextPath = contextPath;
    }

    public String getSource() {
        return source;
    }

    JsonNode asRawNode(String paramName) {
        return param(paramName);
    }

    public boolean isEmpty() {
        return json.isMissingNode();
    }

    public NodeAdapter path(String paramName) {
        NodeAdapter child = new NodeAdapter(
                param(paramName),
                source,
                fullPath(paramName)
        );

        if(!child.isEmpty()) {
            parameterNames.add(paramName);
            children.add(child);
        }
        return child;
    }

    /** Delegates to {@link JsonNode#has(String)} */
    public boolean exist(String paramName) {
        return json.has(paramName);
    }

    public Boolean asBoolean(String paramName, boolean defaultValue) {
        return param(paramName).asBoolean(defaultValue);
    }

    /**
     * Get a required parameter as a boolean value.
     * @throws OtpAppException if parameter is missing.
     */
    public boolean asBoolean(String paramName) {
        assertRequiredFieldExist(paramName);
        return param(paramName).asBoolean();
    }

    public double asDouble(String paramName, double defaultValue) {
        return param(paramName).asDouble(defaultValue);
    }

    public double asDouble(String paramName) {
        assertRequiredFieldExist(paramName);
        return param(paramName).asDouble();
    }

    public List<Double> asDoubles(String paramName, List<Double> defaultValue) {
        if(!exist(paramName)) return defaultValue;
        return arrayAsList(paramName, JsonNode::asDouble);
    }

    public int asInt(String paramName, int defaultValue) {
        return param(paramName).asInt(defaultValue);
    }

    public int asInt(String paramName) {
        assertRequiredFieldExist(paramName);
        return param(paramName).asInt();
    }

    public long asLong(String paramName, long defaultValue) {
        return param(paramName).asLong(defaultValue);
    }

    public String asText(String paramName, String defaultValue) {
        return param(paramName).asText(defaultValue);
    }

    /**
     * Get a required parameter as a text String value.
     * @throws OtpAppException if parameter is missing.
     */
    public String asText(String paramName) {
        assertRequiredFieldExist(paramName);
        return param(paramName).asText();
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T asEnum(String paramName, T defaultValue) {
        String valueAsString = asText(paramName, defaultValue.name());
        try {
            return Enum.valueOf((Class<T>) defaultValue.getClass(), valueAsString);
        }
        catch (IllegalArgumentException ignore) {
            List<T> legalValues = (List<T>) Arrays.asList(defaultValue.getClass().getEnumConstants());
            throw new OtpAppException(
                    "The parameter '" + fullPath(paramName)
                    + "': '" + valueAsString + "' is not in legal. Expected one of " + legalValues
                    + ". Source: " + source + "."
            );
        }
    }

    /**
     * Get a map of enum values listed in the config like this:
     * (This example have Boolean values)
     * <pre>
     * key : {
     *   A : true,  // turned on
     *   B : false  // turned off
     *   // Commented out to use default value
     *   // C : true
     * }
     * </pre>
     *
     * @param <E> The enum type
     * @param <T> The map value type.
     * @param mapper The function to use to map a node in the JSON tree into a value of type T.
     *               The second argument to the function is the enum NAME(String).
     * @return a map of listed enum values as keys with value, or an empty map if not set.
     */
    public <T, E extends Enum<E>> Map<E, T> asEnumMap(
            String paramName,
            Class<E> enumClass,
            BiFunction<NodeAdapter, String, T> mapper
    ) {
        return localAsEnumMap(paramName, enumClass, mapper, false);
    }

    /**
     * Get a map of enum values listed in the config like the
     * {@link #asEnumMap(String, Class, BiFunction)}, but verify that all enum keys
     * are listed. This can be used for settings where there is appropriate no default
     * value. Note! This method return {@code null} if the given parameter is not present.
     */
    public <T, E extends Enum<E>> Map<E, T> asEnumMapAllKeysRequired(
        String paramName,
        Class<E> enumClass,
        BiFunction<NodeAdapter, String, T> mapper
    ) {
        Map<E, T> map = localAsEnumMap(paramName, enumClass, mapper, true);
        return map.isEmpty() ? null : map;
    }

    public <T extends Enum<T>> Set<T> asEnumSet(String paramName, Class<T> enumClass) {
        if(!exist(paramName)) { return Set.of(); }

        Set<T> result = EnumSet.noneOf(enumClass);

        JsonNode param = param(paramName);
        if(param.isArray()) {
            for (JsonNode it : param) {
                result.add(Enum.valueOf(enumClass, it.asText()));
            }

        }
        // Assume all values is concatenated in one string separated by ','
        else  {
            String[] values = asText(paramName).split("[,\\s]+");
            for (String value : values) {
                    if(value.isBlank()) { continue; }
                    try {
                        result.add(Enum.valueOf(enumClass, value));
                    }
                    catch (IllegalArgumentException e) {
                        throw new OtpAppException("The parameter '" + fullPath(paramName)
                                + "': '" + value + "' is not an enum value of "
                                + enumClass.getSimpleName() + ". Source: " + source + "."
                        );
                    }
            }
        }
        return result;
    }

    public FeedScopedId asFeedScopedId(String paramName, FeedScopedId defaultValue) {
        if(!exist(paramName)) { return defaultValue; }
        return FeedScopedId.parseId(asText(paramName));
    }

    public Locale asLocale(String paramName, Locale defaultValue) {
        if(!exist(paramName)) { return defaultValue; }
        String[] parts = asText(paramName).split("[-_ ]+");
        if(parts.length == 1) { return new Locale(parts[0]); }
        if(parts.length == 2) { return new Locale(parts[0], parts[1]); }
        if(parts.length == 3) { return new Locale(parts[0], parts[1], parts[2]); }
        throw new OtpAppException("The parameter: '" + fullPath(paramName)
                + "' is not recognized as a valid Locale. Use: <Language>[_<country>[_<variant>]]. "
                + "Source: " + source + "."
        );
    }

    public LocalDate asDateOrRelativePeriod(String paramName, String defaultValue) {
        String text = asText(paramName, defaultValue);
        try {
            if (text == null || text.isBlank()) {
                return null;
            }
            if (text.startsWith("-") || text.startsWith("P")) {
                return LocalDate.now().plus(Period.parse(text));
            }
            else {
                return LocalDate.parse(text);
            }
        }
        catch (DateTimeParseException e) {
            throw new OtpAppException("The parameter '" + fullPath(paramName)
                    + "': '" + text + "' is not a Period or LocalDate. "
                    + "Source: " + source + ". Details: " + e.getLocalizedMessage()
            );
        }
    }

    public Pattern asPattern(String paramName, String defaultValue) {
        return Pattern.compile(asText(paramName, defaultValue));
    }

    public List<URI> asUris(String paramName) {
        List<URI> uris = new ArrayList<>();
        JsonNode array = param(paramName);

        if(array.isMissingNode()) {
            return uris;
        }
        assertIsArray(paramName, array);

        for (JsonNode it : array) {
            uris.add(uriFromString(paramName, it.asText()));
        }
        return uris;
    }

    public URI asUri(String paramName, String defaultValue) {
        return uriFromString(paramName, asText(paramName, defaultValue));
    }

    /**
     * Log unused parameters for the entire configuration file/noe tree. Call this method for
     * thew root adapter for each config file read.
     */
    public void logAllUnusedParameters(Logger log) {
        for (String p : unusedParams()) {
            log.warn(
                    "Unexpected config parameter: '{}' in '{}'. Is the spelling correct?",
                    p, source
            );
        }
    }

    /**
     * This method list all unused parameters(full path), also nested ones.
     * It uses recursion to get child nodes.
     */
    private List<String> unusedParams() {
        List<String> unusedParams = new ArrayList<>();
        Iterator<String> it = json.fieldNames();

        while (it.hasNext()) {
            String fieldName = it.next();
            if(!parameterNames.contains(fieldName)) {
                unusedParams.add(fullPath(fieldName));
            }
        }

        for (NodeAdapter c : children) {
            // Recursive call to get child unused parameters
            unusedParams.addAll(c.unusedParams());
        }
        unusedParams.sort(String::compareTo);
        return unusedParams;
    }


    /* private methods */

    private JsonNode param(String paramName) {
        parameterNames.add(paramName);
        return json.path(paramName);
    }

    private String fullPath(String paramName) {
        return contextPath == null ? paramName : concatPath(contextPath, paramName);
    }

    private String concatPath(String a, String b) {
        return a + "." + b;
    }

    private URI uriFromString(String paramName, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new URI(text);
        }
        catch (URISyntaxException e) {
            throw new OtpAppException(
                    "Unable to parse parameter '" + fullPath(paramName) + "'. The value '" + text
                    + "' is is not a valid URI, it should be parsable by java.net.URI class. "
                    + "Source: " + source + "."
            );
        }
    }

    private  <T> List<T> arrayAsList(String paramName, Function<JsonNode, T> parse) {
        List<T> values = new ArrayList<>();
        for (JsonNode node : param(paramName)) {
            values.add(parse.apply(node));
        }
        return values;
    }

    private void assertRequiredFieldExist(String paramName) {
        if(!exist(paramName)) {
            throw requiredFieldMissingException(paramName);
        }
    }

    private OtpAppException requiredFieldMissingException(String paramName) {
        return new OtpAppException("Required parameter '" + fullPath(paramName) + "' not found in '" + source + "'.");
    }

    private void assertIsArray(String paramName, JsonNode array) {
        if(!array.isArray()) {
            throw new OtpAppException(
                    "Unable to parse parameter '" + fullPath(paramName) + "': '" + array.asText()
                            + "' expected an ARRAY. Source: " + source + "."
            );
        }
    }

    private <T, E extends Enum<E>> Map<E, T> localAsEnumMap(
        String paramName, Class<E> enumClass,
        BiFunction<NodeAdapter, String, T> mapper,
        boolean requireAllValues
    ) {
        NodeAdapter node = path(paramName);

        if(node.isEmpty()) { return Map.of(); }

        Map<E, T> result = new HashMap<>();

        for (E v : enumClass.getEnumConstants()) {
            if(node.exist(v.name())) {
                result.put(v, mapper.apply(node, v.name()));
            }
            else if(requireAllValues) {
                throw requiredFieldMissingException(concatPath(paramName, v.name()));
            }
        }
        return result;
    }

}
