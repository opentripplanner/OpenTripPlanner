package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.model.StepFeature;
import org.opentripplanner.transit.model.site.Entrance;

public class EntranceImpl implements GraphQLDataFetchers.GraphQLEntrance {

  @Override
  public DataFetcher<String> code() {
    return environment -> {
      StepFeature feature = environment.getSource();
      Entrance entrance = (Entrance) feature.getFeature();
      return entrance.getCode();
    };
  }

  @Override
  public DataFetcher<String> entranceId() {
    return environment -> {
      StepFeature feature = environment.getSource();
      Entrance entrance = (Entrance) feature.getFeature();
      return entrance.getId().toString();
    };
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> {
      StepFeature feature = environment.getSource();
      Entrance entrance = (Entrance) feature.getFeature();
      return org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(
        entrance.getName(),
        environment
      );
    };
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLWheelchairBoarding> wheelchairAccessible() {
    return environment -> {
      StepFeature feature = environment.getSource();
      Entrance entrance = (Entrance) feature.getFeature();
      return GraphQLUtils.toGraphQL(entrance.getWheelchairAccessibility());
    };
  }
}
