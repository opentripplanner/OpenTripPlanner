package org.opentripplanner.service.streetdecorator.model;

public record EdgeLevelInfo(VertexLevelInfo lowerVertexInfo, VertexLevelInfo upperVertexInfo) {
  public boolean matchesNodes(long firstOsmVertexId, long secondOsmVertexId) {
    return (
      (lowerVertexInfo.osmVertexId() == firstOsmVertexId &&
        upperVertexInfo.osmVertexId() == secondOsmVertexId) ||
      (lowerVertexInfo.osmVertexId() == secondOsmVertexId &&
        upperVertexInfo.osmVertexId() == firstOsmVertexId)
    );
  }
}
