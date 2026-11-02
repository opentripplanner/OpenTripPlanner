package org.opentripplanner.apis.gtfs.mapping;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.model.PickDrop;

public final class PickDropMapper {

  public static GraphQLTypes.@Nullable GraphQLPickupDropoffType map(PickDrop pickDrop) {
    return switch (pickDrop) {
      case SCHEDULED -> GraphQLTypes.GraphQLPickupDropoffType.SCHEDULED;
      case NONE -> GraphQLTypes.GraphQLPickupDropoffType.NONE;
      case CALL_AGENCY -> GraphQLTypes.GraphQLPickupDropoffType.CALL_AGENCY;
      case COORDINATE_WITH_DRIVER -> GraphQLTypes.GraphQLPickupDropoffType.COORDINATE_WITH_DRIVER;
      case CANCELLED -> null;
    };
  }
}
