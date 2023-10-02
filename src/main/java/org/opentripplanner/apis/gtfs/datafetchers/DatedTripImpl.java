package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.mapping.LocalDateMapper;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.timetable.DatedTrip;
import org.opentripplanner.transit.model.timetable.Trip;

public class DatedTripImpl implements GraphQLDataFetchers.GraphQLDatedTrip {

  @Override
  public DataFetcher<String> date() {
    return env -> LocalDateMapper.mapToApi(getSource(env).serviceDate());
  }

  @Override
  public DataFetcher<Trip> trip() {
    return env -> getSource(env).trip();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return env ->
      new Relay.ResolvedGlobalId(
        "DatedTrip",
        getSource(env).trip().getId().toString() +
        ';' +
        LocalDateMapper.mapToApi(getSource(env).serviceDate())
      );
  }

  private DatedTrip getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
