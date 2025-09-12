package org.opentripplanner.service.streetdecorator.model;

public class EdgeLevelInformation implements EdgeInformation {

  private VertexLevelInformation lowerVertexInformation;
  private VertexLevelInformation upperVertexInformation;

  public EdgeLevelInformation(
    VertexLevelInformation lowerVertexInformation,
    VertexLevelInformation upperVertexInformation
  ) {
    this.lowerVertexInformation = lowerVertexInformation;
    this.upperVertexInformation = upperVertexInformation;
  }

  @Override
  public boolean isLowerVertex(long osmVertexId) {
    return this.lowerVertexInformation.osmVertexId() == osmVertexId;
  }

  @Override
  public Float getLowerVertexFloorNumber() {
    return this.lowerVertexInformation.floorNumber();
  }

  @Override
  public String getLowerVertexName() {
    return this.lowerVertexInformation.name();
  }

  @Override
  public boolean isUpperVertex(long osmVertexId) {
    return this.upperVertexInformation.osmVertexId() == osmVertexId;
  }

  @Override
  public Float getUpperVertexFloorNumber() {
    return this.upperVertexInformation.floorNumber();
  }

  @Override
  public String getUpperVertexName() {
    return this.upperVertexInformation.name();
  }
}
