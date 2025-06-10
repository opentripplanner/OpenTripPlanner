package org.opentripplanner.apis.gtfs.mapping;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.geometry.WgsCoordinate;

public class CoordinateMapper {

  public static Optional<WgsCoordinate> mapCoordinate(
    @Nullable GraphQLTypes.GraphQLPlanCoordinateInput coordinate
  ) {
    if (
      coordinate == null ||
      coordinate.getGraphQLLatitude() == null ||
      coordinate.getGraphQLLongitude() == null
    ) {
      return Optional.empty();
    }
    return Optional.of(
      new WgsCoordinate(coordinate.getGraphQLLatitude(), coordinate.getGraphQLLongitude())
    );
  }
}
