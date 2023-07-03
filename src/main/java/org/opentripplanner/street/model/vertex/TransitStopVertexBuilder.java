package org.opentripplanner.street.model.vertex;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

public class TransitStopVertexBuilder {

  private RegularStop stop;
  private Set<TransitMode> modes;

  public TransitStopVertexBuilder withStop(RegularStop stop) {
    this.stop = stop;
    return this;
  }

  public TransitStopVertexBuilder withModes(Set<TransitMode> modes) {
    this.modes = modes;
    return this;
  }

  public TransitStopVertex build() {
    Objects.requireNonNull(stop);
    return new TransitStopVertex(stop, modes);
  }
}
