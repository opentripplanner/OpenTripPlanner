package org.opentripplanner.service.streetdecorator.model;

public interface EdgeInformation {
  boolean isLowerVertex(long osmVertexId);

  default Float getLowerVertexFloorNumber() {
    return null;
  }

  default String getLowerVertexName() {
    return null;
  }

  boolean isUpperVertex(long osmVertexId);

  default Float getUpperVertexFloorNumber() {
    return null;
  }

  default String getUpperVertexName() {
    return null;
  }
}
