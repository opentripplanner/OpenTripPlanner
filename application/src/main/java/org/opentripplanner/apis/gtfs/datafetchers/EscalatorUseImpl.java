package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLVerticalDirection;
import org.opentripplanner.apis.gtfs.mapping.VerticalDirectionMapper;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.EscalatorUse;
import org.opentripplanner.service.streetdetails.model.Level;

public class EscalatorUseImpl implements GraphQLDataFetchers.GraphQLEscalatorUse {

  @Override
  public DataFetcher<Level> from() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return escalatorUse.from();
    };
  }

  @Override
  public DataFetcher<Level> to() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return escalatorUse.to();
    };
  }

  @Override
  public DataFetcher<GraphQLVerticalDirection> verticalDirection() {
    return environment -> {
      EscalatorUse escalatorUse = environment.getSource();
      return VerticalDirectionMapper.map(escalatorUse.verticalDirection());
    };
  }
}
