package org.opentripplanner.ext.dataoverlay.routing;

import static org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters.toStringKey;
import static org.opentripplanner.ext.dataoverlay.api.ParameterType.PENALTY;
import static org.opentripplanner.ext.dataoverlay.api.ParameterType.THRESHOLD;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.api.ParameterType;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataOverlayContext {

  private static final Logger LOG = LoggerFactory.getLogger(DataOverlayContext.class);

  private final List<Parameter> parameters = new ArrayList<>();

  public DataOverlayContext(
    DataOverlayParameterBindings parameterBindings,
    DataOverlayParameters requestParameters
  ) {
    for (ParameterName paramName : requestParameters.listParameterNames()) {
      addParameter(paramName, parameterBindings, requestParameters);
    }
  }

  public Iterable<? extends Parameter> getParameters() {
    return parameters;
  }

  private void addParameter(
    ParameterName paramName,
    DataOverlayParameterBindings parameterBindings,
    DataOverlayParameters requestParameters
  ) {
    var penalty = requestParameters.get(paramName, PENALTY);

    if (isParameterValueInvalid(penalty, paramName, PENALTY)) {
      return;
    }

    var threshold = requestParameters.get(paramName, THRESHOLD);

    if (isParameterValueInvalid(threshold, paramName, THRESHOLD)) {
      return;
    }

    var binding = parameterBindings.getParameterBinding(paramName);

    if (binding.isEmpty()) {
      LOG.warn("Request parameter config not found. Parameter: {}", paramName);
      return;
    }

    this.parameters.add(new Parameter(binding.get(), threshold, penalty));
  }

  private boolean isParameterValueInvalid(Double value, ParameterName name, ParameterType type) {
    if (value == null || value < 0d) {
      LOG.warn("Request parameter required. Parameter: {}", toStringKey(name, type));
      return true;
    } else {
      return false;
    }
  }
}
