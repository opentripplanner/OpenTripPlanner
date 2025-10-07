package org.opentripplanner.service.streetdecorator.model;

import javax.annotation.Nullable;

public record VertexLevelInfo(
  @Nullable Double floorNumber,
  @Nullable String name,
  long osmVertexId
) {}
