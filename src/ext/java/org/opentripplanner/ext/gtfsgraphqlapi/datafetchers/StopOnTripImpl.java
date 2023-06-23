package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.model.LegacyGraphQLStopOnTripModel;
import org.opentripplanner.transit.model.timetable.Trip;

public class StopOnTripImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStopOnTrip {

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment).getTrip();
  }

  private LegacyGraphQLStopOnTripModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
