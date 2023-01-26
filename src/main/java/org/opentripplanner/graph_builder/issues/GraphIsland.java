package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.Vertex;

public class GraphIsland implements DataImportIssue {

  public static final String FMT =
    "Pruned %s subgraph %s containing vertex '%s' at (%f, %f) of %d street vertices and %d stops. Edge changes: %d to nothru, %d to no traversal, %d erased";
  public static final String HTMLFMT =
    "Pruned %s <a href='http://www.openstreetmap.org/node/%s'>subgraph %s</a> of %d street vertices %d stops. Edge changes: %d to nothru, %d to no traversal, %d erased";

  final Vertex vertex;
  final int streetSize;
  final int stopSize;
  final int nothru;
  final int restricted;
  final int removed;
  final String traversalMode;

  public GraphIsland(
    Vertex vertex,
    int streetSize,
    int stopSize,
    int nothru,
    int restricted,
    int removed,
    String traversalMode
  ) {
    this.vertex = vertex;
    this.streetSize = streetSize;
    this.stopSize = stopSize;
    this.nothru = nothru;
    this.restricted = restricted;
    this.removed = removed;
    this.traversalMode = traversalMode;
  }

  @Override
  public String getMessage() {
    return String.format(
      FMT,
      traversalMode,
      vertex.getLabel(),
      vertex,
      vertex.getCoordinate().x,
      vertex.getCoordinate().y,
      this.streetSize,
      this.stopSize,
      this.nothru,
      this.restricted,
      this.removed
    );
  }

  @Override
  public String getHTMLMessage() {
    String label = vertex.getLabel();
    if (label.startsWith("osm:")) {
      String osmNodeId = label.split(":")[2];
      return String.format(
        HTMLFMT,
        traversalMode,
        osmNodeId,
        osmNodeId,
        this.streetSize,
        this.stopSize,
        this.nothru,
        this.restricted,
        this.removed
      );
    } else {
      return this.getMessage();
    }
  }

  @Override
  public int getPriority() {
    return stopSize + streetSize;
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }
}
