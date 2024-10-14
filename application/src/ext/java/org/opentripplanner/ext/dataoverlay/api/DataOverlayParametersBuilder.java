package org.opentripplanner.ext.dataoverlay.api;

import static org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters.resolveKey;
import static org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters.resolveValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to create a new instance of {@link DataOverlayParameters}.
 */
public class DataOverlayParametersBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DataOverlayParameters.class);

  private final Map<Parameter, Double> values = new HashMap<>();

  /**
   * Parse the input {@code params} and create a new {@link DataOverlayParameters} instance. All
   * unknown parameters are ignored, so are missing values. This method is created to receive all
   * input and filter out the data-overlay parameters.
   */
  public static DataOverlayParameters parseQueryParams(Map<String, List<String>> params) {
    var result = new DataOverlayParametersBuilder();
    for (String key : params.keySet()) {
      var name = resolveKey(key);
      if (name != null) {
        List<String> values = params.get(key);
        if (values == null || values.isEmpty()) {
          LOG.warn("The data-overlay parameter value is missing. Parameter: {}", key);
          continue;
        }
        var value = resolveValue(values.get(0));
        if (value == null) {
          LOG.warn("The data-overlay parameter value is null. Parameter: {}", key);
          continue;
        }
        result.add(name, value);
      }
    }
    return result.build();
  }

  public DataOverlayParametersBuilder add(String param, Double value) {
    return add(resolveKey(param), value);
  }

  public DataOverlayParametersBuilder add(ParameterName name, ParameterType type, Double value) {
    return add(new Parameter(name, type), value);
  }

  private DataOverlayParametersBuilder add(Parameter param, Double value) {
    if (param != null) {
      values.put(param, value);
    }
    return this;
  }

  public DataOverlayParameters build() {
    return new DataOverlayParameters(values);
  }
}
