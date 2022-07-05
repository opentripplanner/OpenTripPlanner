package org.opentripplanner.routing.vertextype;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.service.TransitModel;

public class TransitStopVertexBuilder {

  private Stop stop;
  private Graph graph;
  private TransitModel transitModel;
  private Set<TransitMode> modes;

  public TransitStopVertexBuilder withStop(Stop stop) {
    this.stop = stop;
    return this;
  }

  public TransitStopVertexBuilder withGraph(Graph graph) {
    this.graph = graph;
    return this;
  }

  public TransitStopVertexBuilder withTransitModel(TransitModel transitModel) {
    this.transitModel = transitModel;
    return this;
  }

  public TransitStopVertexBuilder withModes(Set<TransitMode> modes) {
    this.modes = modes;
    return this;
  }

  public TransitStopVertex build() {
    Objects.requireNonNull(graph);
    Objects.requireNonNull(transitModel);
    Objects.requireNonNull(stop);
    TransitStopVertex stopVertex = new TransitStopVertex(graph, stop, modes);
    transitModel.getStopModel().addTransitStopVertex(stopVertex.getStop().getId(), stopVertex);
    return stopVertex;
  }
}
