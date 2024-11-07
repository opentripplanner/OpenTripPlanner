package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.site.Entrance;

public class EntranceImpl implements GraphQLDataFetchers.GraphQLEntrance {

  @Override
  public DataFetcher<String> code() {
    return environment -> {
      Entrance entrance = environment.getSource();
      return entrance.getCode();
    };
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> {
      Entrance entrance = environment.getSource();
      return entrance.getId() != null ? entrance.getId().toString() : null;
    };
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> {
      Entrance entrance = environment.getSource();
      return entrance.getName() != null
        ? org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
          entrance.getName(),
          environment
        )
        : null;
    };
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLWheelchairBoarding> wheelchairAccessible() {
    return environment -> {
      Entrance entrance = environment.getSource();
      return GraphQLUtils.toGraphQL(entrance.getWheelchairAccessibility());
    };
  }
}
