package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLVerticalDirection;
import org.opentripplanner.apis.gtfs.mapping.VerticalDirectionMapper;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.StairsUse;
import org.opentripplanner.service.streetdetails.model.Level;

public class StairsUseImpl implements GraphQLDataFetchers.GraphQLStairsUse {

  @Override
  public DataFetcher<Level> from() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return stairsUse.from();
    };
  }

  @Override
  public DataFetcher<Level> to() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return stairsUse.to();
    };
  }

  @Override
  public DataFetcher<GraphQLVerticalDirection> verticalDirection() {
    return environment -> {
      StairsUse stairsUse = environment.getSource();
      return VerticalDirectionMapper.map(stairsUse.verticalDirection());
    };
  }
}
