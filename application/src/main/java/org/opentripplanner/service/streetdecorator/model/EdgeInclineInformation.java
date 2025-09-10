package org.opentripplanner.service.streetdecorator.model;

public class EdgeInclineInformation implements EdgeInformation {

  private long lowerVertexId;
  private long upperVertexId;

  public EdgeInclineInformation(long lowerVertexId, long upperVertexId) {
    this.lowerVertexId = lowerVertexId;
    this.upperVertexId = upperVertexId;
  }

  @Override
  public boolean isLowerVertex(long osmVertexId) {
    return this.lowerVertexId == osmVertexId;
  }

  @Override
  public boolean isUpperVertex(long osmVertexId) {
    return this.upperVertexId == osmVertexId;
  }
}
