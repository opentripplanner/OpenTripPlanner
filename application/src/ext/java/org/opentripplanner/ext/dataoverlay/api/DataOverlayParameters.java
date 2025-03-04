package org.opentripplanner.ext.dataoverlay.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The purpose of this class is to hold all parameters and their value in a map. It also contains
 * logic for mapping between the data overlay domain and the API. The purpose is to provide a
 * standardized naming scheme for the parameters. The data overlay can be configured to use any kind
 * of parameters, so if you do not find the parameter you want (e.g. snow_depth) in the {@link
 * ParameterName}, then request your parameter to be added.
 * <p>
 * This class contain helper logic to convert parameters to and from a string representation used by
 * the APIs(the REST API is the only supported API). The parameter string format is:
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
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE
 */
public class DataOverlayParameters implements Serializable {

  private final Map<Parameter, Double> values;

  public DataOverlayParameters(Map<Parameter, Double> values) {
    // Make a defensive copy to protect the map entries, this makes the class immutable
    // and thread safe
    this.values = Map.copyOf(values);
  }

  /**
   * Parse the input {@code params} and create a new {@link DataOverlayParameters} instance. All
   * unknown parameters are ignored, so are missing values. This method is created to receive all
   * input and filter out the data-overlay parameters.
   */
  public static DataOverlayParameters parseQueryParams(Map<String, List<String>> params) {
    return DataOverlayParametersBuilder.parseQueryParams(params);
  }

  /**
   * List all parameters supported as stings. The format is:
   * <p>
   * [{@link ParameterName}] {@code + ' ' +} [{@link ParameterType}]   (lower case is used)  <p>
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

  public static String toStringKey(ParameterName name, ParameterType type) {
    return (name + "_" + type).toLowerCase();
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public Double get(String param) {
    return get(resolveKey(param));
  }

  public Double get(ParameterName name, ParameterType type) {
    return values.get(new Parameter(name, type));
  }

  public Iterable<ParameterName> listParameterNames() {
    return values.keySet().stream().map(Parameter::name).collect(Collectors.toSet());
  }

  @Nullable
  static Parameter resolveKey(String parameter) {
    try {
      int pos = parameter.lastIndexOf('_');
      if (pos < 0 || pos > parameter.length() - 2) {
        return null;
      }
      var name = ParameterName.valueOf(parameter.substring(0, pos).toUpperCase());
      var type = ParameterType.valueOf(parameter.substring(pos + 1).toUpperCase());
      return new Parameter(name, type);
    } catch (IllegalArgumentException ignore) {
      return null;
    }
  }

  @Nullable
  static Double resolveValue(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException | NullPointerException ignore) {
      return null;
    }
  }

  private Double get(Parameter param) {
    return values.get(param);
  }

  @Override
  public String toString() {
    var buf = ToStringBuilder.of(DataOverlayParameters.class);
    // Map keys to String and sort to make the toSting() deterministic
    var keys = values.keySet().stream().map(Parameter::keyString).sorted().toList();
    for (String key : keys) {
      buf.addObj(key, get(key));
    }
    return buf.toString();
  }
}
