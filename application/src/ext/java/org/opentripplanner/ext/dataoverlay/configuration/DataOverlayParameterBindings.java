package org.opentripplanner.ext.dataoverlay.configuration;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class DataOverlayParameterBindings implements Serializable {

  private final List<ParameterBinding> parameters;

  DataOverlayParameterBindings(List<ParameterBinding> parameters) {
    this.parameters = parameters;
  }

  public Optional<ParameterBinding> getParameterBinding(ParameterName name) {
    return parameters.stream().filter(it -> it.getName() == name).findFirst();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DataOverlayParameterBindings.class)
      .addCol("parameters", parameters)
      .toString();
  }
}
