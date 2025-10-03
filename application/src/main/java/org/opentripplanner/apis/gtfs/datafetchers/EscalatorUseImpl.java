package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLInclineType;
import org.opentripplanner.apis.gtfs.mapping.InclineTypeMapper;
import org.opentripplanner.model.plan.walkstep.verticaltransportationuse.EscalatorUse;

public class EscalatorUseImpl implements GraphQLDataFetchers.GraphQLEscalatorUse {

  @Override
  public DataFetcher<Double> fromLevel() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return escalatorUse.fromLevel();
    };
  }

  @Override
  public DataFetcher<String> fromLevelName() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return escalatorUse.fromLevelName();
    };
  }

  @Override
  public DataFetcher<GraphQLInclineType> inclineType() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return InclineTypeMapper.map(escalatorUse.inclineType());
    };
  }

  @Override
  public DataFetcher<Double> toLevel() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return escalatorUse.toLevel();
    };
  }

  @Override
  public DataFetcher<String> toLevelName() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return escalatorUse.toLevelName();
    };
  }
}
