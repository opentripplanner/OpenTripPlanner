package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.util.OtpAppException;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class NodeAdapter {

    private final JsonNode json;
    private final String source;

    public NodeAdapter(@NotNull JsonNode node, String source) {
        json = node;
        this.source = source;
    }

    public JsonNode asRawNode() {
        return json;
    }

    JsonNode asRawNode(String path) {
        return json.path(path);
    }

    public boolean isEmpty() {
        return json.isMissingNode();
    }

    public NodeAdapter path(String path) {
        return new NodeAdapter(json.path(path), source + "/" + path);
    }

    public Boolean asBoolean(String path, boolean defaultValue) {
        return json.path(path).asBoolean(defaultValue);
    }

    public double asDouble(String path, double defaultValue) {
        return json.path(path).asDouble(defaultValue);
    }

    public int asInt(String path, int defaultValue) {
        return json.path(path).asInt(defaultValue);
    }

    public String asText(String path, String defaultValue) {
        return json.path(path).asText(defaultValue);
    }

    /**
     * @throws OtpAppException if path doe not exist.
     */
    public String asText(String path) {
        return required(path, asText(path, null));
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T asEnum(String propertyName, T defaultValue) {
        String valueAsString = asText(propertyName, defaultValue.name());
        try {
            return Enum.valueOf((Class<T>) defaultValue.getClass(), valueAsString);
        }
        catch (IllegalArgumentException ignore) {
            List<T> legalValues = (List<T>) Arrays.asList(defaultValue.getClass().getEnumConstants());
            throw new OtpAppException("The '" + source + "' parameter '" + propertyName
                    + "' : '" + valueAsString + "' is not in legal. Expected one of " + legalValues + ".");
        }
    }

    public LocalDate asDateOrRelativePeriod(String propertyName, String defaultValue) {
        String text = asText(propertyName, defaultValue);
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
            throw new OtpAppException("The " + source + " parameter: '" + propertyName
                    + "' : '" + text + "' is not a Period or LocalDate: " +
                    e.getLocalizedMessage()
            );
        }
    }

    public Pattern asPattern(String path, String defaultValue) {
        return Pattern.compile(asText(path, defaultValue));
    }

    public List<URI> asUris(String name) {
        List<URI> uris = new ArrayList<>();
        JsonNode array = json.path(name);

        if(array.isMissingNode()) {
            return uris;
        }
        assertIsArray(name, array);

        for (JsonNode it : array) {
            uris.add(uriFromString(name, it.asText()));
        }
        return uris;
    }

    public URI asUri(String name, String defaultValue) {
        return uriFromString(name, asText(name, defaultValue));
    }


    /* private methods */
    private URI uriFromString(String name, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new URI(text);
        }
        catch (URISyntaxException e) {
            throw new OtpAppException(
                    "Unable to parse '" + source + "' parameter: "
                            + "\n\tActual: \"" + name + "\" : \"" + text + "\""
                            + "\n\tExpected valid URI, it should be parsable by java.net.URI class.");
        }
    }

    private <T> T required(String name, T value) {
        if(value == null) {
            throw new OtpAppException("Required parameter '" + name + "' not found in '" + source + "'.");
        }
        return value;
    }

    private void assertIsArray(String name, JsonNode array) {
        if(!array.isArray()) {
            throw new OtpAppException(
                    "Unable to parse '" + source +"' parameter: "
                            + "\n\tActual: \"" + name + "\" : \"" + array.asText() + "\""
                            + "\n\tExpected ARRAY of URIs: [ \"<uri>\", .. ]."
            );
        }
    }
}
