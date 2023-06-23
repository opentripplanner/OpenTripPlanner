package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.model.StopOnTripModel;
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
