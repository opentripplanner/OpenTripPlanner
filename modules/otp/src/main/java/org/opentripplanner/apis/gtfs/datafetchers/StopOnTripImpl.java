package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.StopOnTripModel;
import org.opentripplanner.transit.model.timetable.Trip;

public class StopOnTripImpl implements GraphQLDataFetchers.GraphQLStopOnTrip {

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment).getTrip();
  }

  private StopOnTripModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
