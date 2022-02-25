package org.opentripplanner.ext.dataoverlay.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.common.model.T2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to hold all parameters and their value in a map. It also contains
 * logic for mapping between the data overlay domain and the API. The purpose is to provide a
 * standardized naming scheme for the parameters. The data overlay can be configured to use any
 * kind of parameters, so if you do not find the parameter you want (e.g. snow_depth) in the
 * {@link ParameterName}, then request your parameter to be added.
 * <p>
 * This class contain helper logic to convert parameters to and from a string representation used
 * by the APIs(the REST API is the only supported API). The parameter string format is:
 * <p>
 * [{@link ParameterName}] {@code + ' ' +} [{@link ParameterType}]   (lower case is used)
 * <p>
 * When parameters are parsed the case is ignored.
 * <p>
 * Example:
 * <pre>
 * carbon_monoxide_threshold
 * carbon_monoxide_penalty
 * </pre>
 */
public class DataOverlayParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DataOverlayParameters.class);

    private final Map<T2<ParameterName, ParameterType>, Double> values = new HashMap<>();

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Parse the input {@code params} and create a new {@link DataOverlayParameters}
     * instance. All unknown parameters are ignored, so are missing values. This method is
     * created to receive all input and filter out the data-overlay parameters.
     */
    public static DataOverlayParameters parseQueryParams(Map<String, List<String>> params) {
        var result = new DataOverlayParameters();
        for (String key : params.keySet()) {
            var name = resolveKey(key);
            if (name != null) {
                List<String> values = params.get(key);
                if(values == null || values.isEmpty()) {
                    LOG.warn("The data-overlay parameter value is missing. Parameter: {}", key);
                    continue;
                }
                var value = resolveValue(values.get(0));
                if(value == null) {
                    LOG.warn("The data-overlay parameter value is null. Parameter: {}", key);
                    continue;
                }
                result.put(name, value);
            }
        }
        return result;
    }

    /**
     * List all parameters supported as stings. The format is:
     * <p>
     *  [{@link ParameterName}] {@code + ' ' +} [{@link ParameterType}]   (lower case is used)  <p>
     * Example:
     * <pre>
     * carbon_monoxide_threshold
     * carbon_monoxide_penalty
     * </pre>
     */
    public static List<String> parametersAsString() {
        var list = new ArrayList<String>();
        for (ParameterName name : ParameterName.values()) {
            for (ParameterType type : ParameterType.values()) {
                list.add((name + "_" + type).toLowerCase());
            }
        }
        return list;
    }

    public Double get(String param) {
        return get(resolveKey(param));
    }

    public Double get(ParameterName name, ParameterType type) {
        return values.get(new T2<>(name, type));
    }

    private Double get( T2<ParameterName, ParameterType> param) {
        return values.get(param);
    }

    public void put(String param, Double value) {
        put(resolveKey(param), value);
    }

    public void put(ParameterName name, ParameterType type, Double value) {
        put(new T2<>(name, type), value);
    }

    private void put(T2<ParameterName, ParameterType> param, Double value) {
        if(param == null) { return; }
        values.put(param, value);
    }

    public static String toStringKey(ParameterName name, ParameterType type) {
        return (name + "_" + type).toLowerCase();
    }

    public Iterable<ParameterName> listParameterNames() {
        return values.keySet().stream().map(it -> it.first).collect(Collectors.toSet());
    }

    @Nullable
    static T2<ParameterName, ParameterType> resolveKey(String parameter) {
        try {
            int pos = parameter.lastIndexOf('_');
            if (pos < 0 || pos > parameter.length() - 2) {return null;}
            var name = ParameterName.valueOf(parameter.substring(0, pos).toUpperCase());
            var type = ParameterType.valueOf(parameter.substring(pos + 1).toUpperCase());
            return new T2<>(name, type);
        }
        catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Nullable
    static Double resolveValue(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException | NullPointerException ignore) { return null; }
    }
}
