package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLInclineType;
import org.opentripplanner.apis.gtfs.mapping.InclineTypeMapper;
import org.opentripplanner.model.plan.walkstep.VerticalTransportationUse;

public class VerticalTransportationUseImpl
  implements GraphQLDataFetchers.GraphQLVerticalTransportationUse {

  @Override
  public DataFetcher<Double> fromLevel() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.fromLevel();
    };
  }

  @Override
  public DataFetcher<String> fromLevelName() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.fromLevelName();
    };
  }

  @Override
  public DataFetcher<GraphQLInclineType> inclineType() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return InclineTypeMapper.map(verticalTransportationUse.inclineType());
    };
  }

  @Override
  public DataFetcher<Double> toLevel() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.toLevel();
    };
  }

  @Override
  public DataFetcher<String> toLevelName() {
    return environment -> {
      VerticalTransportationUse verticalTransportationUse = environment.getSource();
      return verticalTransportationUse.toLevelName();
    };
  }
}
