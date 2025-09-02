package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.VerticalTransportationTypeMapper;
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
  public DataFetcher<String> name() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
        verticalTransportationUse.name(),
        environment
      );
    };
  }

  @Override
  public DataFetcher<Double> fromLevel() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.fromLevel();
    };
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLVerticalTransportationType> type() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return VerticalTransportationTypeMapper.map(verticalTransportationUse.type());
    };
  }
}
