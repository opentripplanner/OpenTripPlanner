package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiVertexType;

@Deprecated
class VertexTypeMapper {

  public static ApiVertexType mapVertexType(VertexType domain) {
    if (domain == null) {
      return null;
    }
    switch (domain) {
      case NORMAL:
        return ApiVertexType.NORMAL;
      case VEHICLEPARKING:
        return ApiVertexType.BIKEPARK;
      case VEHICLERENTAL:
        return ApiVertexType.BIKESHARE;
      case TRANSIT:
        return ApiVertexType.TRANSIT;
      default:
        throw new IllegalArgumentException(domain.toString());
    }
  }
}
