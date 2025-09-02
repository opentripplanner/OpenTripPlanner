package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.plan.walkstep.VerticalTransportationUse;

public class VerticalTransportationUseImpl
  implements GraphQLDataFetchers.GraphQLVerticalTransportationUse {

  @Override
  public DataFetcher<Double> toLevel() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.toLevel();
    };
  }

  @Override
  public DataFetcher<Double> fromLevel() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.fromLevel();
    };
  }
}
