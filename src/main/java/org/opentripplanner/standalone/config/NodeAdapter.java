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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public class NodeAdapter {

    private final JsonNode json;

    /**
     * The source should should be a unique identifier for this adapter. The
     * normal way to do it is to name the source using the configuration "source"
     * (could be the filename, "DEFAULT", or serialized part of the graph) and the path
     * of keys in the config, like this:
     * {@code build-config.json/transit/dynamicSearchWindow}
     */
    private final String source;

    /**
     * This parameter is used internally in this class to be able to produce a
     * list of parameters witch is NOT requested.
     */
    private final Set<String> parameterNames = new HashSet<>();

    /**
     * The collection of children is used to be able to produce a list of unused parameters
     * for all children.
     */
    private final Set<NodeAdapter> children = new HashSet<>();

    public NodeAdapter(@NotNull JsonNode node, String source) {
        json = node;
        this.source = source;
    }

    JsonNode asRawNode(String paramName) {
        return param(paramName);
    }

    public boolean isEmpty() {
        return json.isMissingNode();
    }

    public NodeAdapter path(String paramName) {
        NodeAdapter child = new NodeAdapter(json.path(paramName), source + "/" + paramName);
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
            throw new OtpAppException("The '" + source + "' parameter '" + paramName
                    + "' : '" + valueAsString + "' is not in legal. Expected one of " + legalValues + ".");
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
     *               The secon argument to the function is the enum NAME(String).
     */
    public <T, E extends Enum<E>> Map<E, T> asEnumMap(
            String paramName, Class<E> enumClass, BiFunction<NodeAdapter, String, T> mapper
    ) {
        NodeAdapter node = path(paramName);

        if(node.isEmpty()) { return Map.of(); }

        Map<E, T> result = new HashMap<>();

        for (E v : enumClass.getEnumConstants()) {
            if(node.exist(v.name())) {
                result.put(v, mapper.apply(node, v.name()));
            }
        }
        return result;
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
                        throw new OtpAppException("The '" + source + "' parameter '" + paramName
                                + "' : '" + value + "' is not an enum value of "
                                + enumClass.getSimpleName() + "."
                        );
                    }
            }
        }
        return result;
    }

    public FeedScopedId asFeedScopedId(String paramName, FeedScopedId defaultValue) {
        if(!exist(paramName)) { return defaultValue; }
        return FeedScopedId.convertFromString(asText(paramName));
    }

    public Locale asLocale(String paramName, Locale defaultValue) {
        if(!exist(paramName)) { return defaultValue; }
        String[] parts = asText(paramName).split("[-_ ]+");
        if(parts.length == 1) { return new Locale(parts[0]); }
        if(parts.length == 2) { return new Locale(parts[0], parts[1]); }
        if(parts.length == 3) { return new Locale(parts[0], parts[1], parts[2]); }
        throw new OtpAppException("The '" + source + "' parameter: '" + paramName
                + "' is not recognized as a valid Locale. Use: <Language>[_<country>[_<variant>]].");
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
            throw new OtpAppException("The " + source + " parameter: '" + paramName
                    + "' : '" + text + "' is not a Period or LocalDate: " +
                    e.getLocalizedMessage()
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

    public void logUnusedParameters(Logger log) {
        for (String p : unusedParams()) {
            log.warn(
                    "Unexpected config parameter: '{}'. The parameter is unknown,"
                    + " is the spelling correct?", p
            );
        }
    }

    /**
     * Unused parameters should be logged for each config file read. This method list all
     * unused parameters, also nested ones. It uses recursion to get child nodes.
     */
    private List<String> unusedParams() {
        List<String> unusedParams = new ArrayList<>();
        Iterator<String> it = json.fieldNames();
        while (it.hasNext()) {
            String fieldName = it.next();
            if(!parameterNames.contains(fieldName)) {
                unusedParams.add(source + "/" + fieldName);
            }
        }
        for (NodeAdapter c : children) {
            // Recursive call to get child unused parameters
            unusedParams.addAll(c.unusedParams());
        }
        return unusedParams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        NodeAdapter adapter = (NodeAdapter) o;
        return source.equals(adapter.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }


    /* private methods */

    private JsonNode param(String paramName) {
        parameterNames.add(paramName);
        return json.path(paramName);
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
                    "Unable to parse '" + source + "' parameter: "
                            + "\n\tActual: \"" + paramName + "\" : \"" + text + "\""
                            + "\n\tExpected valid URI, it should be parsable by java.net.URI class.");
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
        return new OtpAppException("Required parameter '" + paramName + "' not found in '" + source + "'.");
    }

    private void assertIsArray(String paramName, JsonNode array) {
        if(!array.isArray()) {
            throw new OtpAppException(
                    "Unable to parse '" + source +"' parameter: "
                            + "\n\tActual: \"" + paramName + "\" : \"" + array.asText() + "\""
                            + "\n\tExpected an ARRAY."
            );
        }
    }
}
