package org.opentripplanner.service.streetdecorator.model;

public interface EdgeInformation {
  boolean isLowerVertex(long osmVertexId);

  default Integer getLowerVertexFloorIndex() {
    return null;
  }

  default String getLowerVertexName() {
    return null;
  }

  boolean isUpperVertex(long osmVertexId);

  default Integer getUpperVertexFloorIndex() {
    return null;
  }

  default String getUpperVertexName() {
    return null;
  }
}
