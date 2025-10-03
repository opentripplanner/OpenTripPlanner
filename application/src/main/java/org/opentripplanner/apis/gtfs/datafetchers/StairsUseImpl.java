package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLInclineType;
import org.opentripplanner.apis.gtfs.mapping.InclineTypeMapper;
import org.opentripplanner.model.plan.walkstep.verticaltransportationuse.StairsUse;

public class StairsUseImpl implements GraphQLDataFetchers.GraphQLStairsUse {

  @Override
  public DataFetcher<Double> fromLevel() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return stairsUse.fromLevel();
    };
  }

  @Override
  public DataFetcher<String> fromLevelName() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return stairsUse.fromLevelName();
    };
  }

  @Override
  public DataFetcher<GraphQLInclineType> inclineType() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return InclineTypeMapper.map(stairsUse.inclineType());
    };
  }

  @Override
  public DataFetcher<Double> toLevel() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return stairsUse.toLevel();
    };
  }

  @Override
  public DataFetcher<String> toLevelName() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return stairsUse.toLevelName();
    };
  }
}
