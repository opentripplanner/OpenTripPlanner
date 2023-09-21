package org.opentripplanner.ext.gtfsgraphqlapi.mapping;

import javax.annotation.Nonnull;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes;
import org.opentripplanner.transit.model.network.BikeAccess;

public class BikeAccessMapper {
  @Nonnull
  public static GraphQLTypes.GraphQLBikesAllowed map(BikeAccess bikesAllowed) {
    return switch (bikesAllowed) {
      case UNKNOWN -> GraphQLTypes.GraphQLBikesAllowed.NO_INFORMATION;
      case ALLOWED -> GraphQLTypes.GraphQLBikesAllowed.ALLOWED;
      case NOT_ALLOWED -> GraphQLTypes.GraphQLBikesAllowed.NOT_ALLOWED;
    };
  }
}
