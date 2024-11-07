package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.site.Entrance;

public class EntranceImpl implements GraphQLDataFetchers.GraphQLEntrance {

  @Override
  public DataFetcher<String> code() {
    return environment -> {
      Entrance entrance = getEntrance(environment);
      return entrance != null && entrance.getCode() != null ? entrance.getCode() : null;
    };
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> {
      Entrance entrance = getEntrance(environment);
      return entrance != null && entrance.getId() != null ? entrance.getId().toString() : null;
    };
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> {
      Entrance entrance = getEntrance(environment);
      return entrance != null && entrance.getName() != null ? entrance.getName().toString() : null;
    };
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLWheelchairBoarding> wheelchairAccessible() {
    return environment -> {
      Entrance entrance = getEntrance(environment);
      return entrance != null
        ? GraphQLUtils.toGraphQL(entrance.getWheelchairAccessibility())
        : null;
    };
  }

  /**
   * Helper method to retrieve the Entrance object from the DataFetchingEnvironment.
   */
  private Entrance getEntrance(DataFetchingEnvironment environment) {
    Object source = environment.getSource();
    return source instanceof Entrance ? (Entrance) source : null;
  }
}
