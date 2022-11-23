package org.opentripplanner.street.model.vertex;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

public class TransitStopVertexBuilder {

  private RegularStop stop;
  private Graph graph;
  private Set<TransitMode> modes;

  public TransitStopVertexBuilder withStop(RegularStop stop) {
    this.stop = stop;
    return this;
  }

  public TransitStopVertexBuilder withGraph(Graph graph) {
    this.graph = graph;
    return this;
  }

  public TransitStopVertexBuilder withModes(Set<TransitMode> modes) {
    this.modes = modes;
    return this;
  }

  public TransitStopVertex build() {
    Objects.requireNonNull(graph);
    Objects.requireNonNull(stop);
    return new TransitStopVertex(graph, stop, modes);
  }
}
