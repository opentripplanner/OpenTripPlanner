package org.opentripplanner.service.streetdecorator.model;

import javax.annotation.Nullable;

public record VertexLevelInfo(
  @Nullable Float floorNumber,
  @Nullable String name,
  long osmVertexId
) {}
