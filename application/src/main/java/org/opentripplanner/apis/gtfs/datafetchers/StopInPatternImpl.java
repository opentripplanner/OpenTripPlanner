package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.PickDropMapper;
import org.opentripplanner.apis.gtfs.model.StopInPatternModel;
import org.opentripplanner.transit.model.network.TripPattern;

public class StopInPatternImpl implements GraphQLDataFetchers.GraphQLStopInPattern {

  @Override
  public DataFetcher<GraphQLTypes.GraphQLPickupDropoffType> dropoffType() {
    return environment -> PickDropMapper.map(getSource(environment).dropoffType());
  }

  @Override
  public DataFetcher<Integer> indexInPattern() {
    return environment -> getSource(environment).indexInPattern();
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return environment -> getSource(environment).pattern();
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLPickupDropoffType> pickupType() {
    return environment -> PickDropMapper.map(getSource(environment).pickupType());
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).stop();
  }

  private StopInPatternModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
