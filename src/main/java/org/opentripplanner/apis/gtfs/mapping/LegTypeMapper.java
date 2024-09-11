package org.opentripplanner.apis.gtfs.mapping;

import javax.annotation.Nonnull;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;

public class LegTypeMapper {


  @Nonnull
  public static GraphQLTypes.GraphQLLegType map(Leg source) {
    return switch (source) {
      case StreetLeg ignored -> GraphQLTypes.GraphQLLegType.STREET;
      case FlexibleTransitLeg ignored -> GraphQLTypes.GraphQLLegType.FLEX;
      case ScheduledTransitLeg ignored -> GraphQLTypes.GraphQLLegType.SCHEDULED_TRANSIT;
      default -> throw new IllegalStateException("Unhandled leg type: " + source);
    };
  }
}
