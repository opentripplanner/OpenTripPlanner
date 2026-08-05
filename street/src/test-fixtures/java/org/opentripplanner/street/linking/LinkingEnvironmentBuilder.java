package org.opentripplanner.street.linking;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.street.model.vertex.Vertex;

public class LinkingEnvironmentBuilder {

  private final List<Vertex> vertices = new ArrayList<>();

  public LinkingEnvironmentBuilder addVertices(Vertex... vertices) {
    this.vertices.addAll(List.of(vertices));
    return this;
  }

  public LinkingEnvironment build() {
    return new LinkingEnvironment(vertices.toArray(Vertex[]::new));
  }
}
