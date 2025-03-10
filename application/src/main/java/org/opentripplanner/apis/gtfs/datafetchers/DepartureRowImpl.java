package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.GraphQLUtils;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;

public class DepartureRowImpl implements GraphQLDataFetchers.GraphQLDepartureRow {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("DepartureRow", getSource(environment).id);
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).stop.getLat();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).stop.getLon();
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return environment -> getSource(environment).pattern;
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).stop;
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimes() {
    return environment -> {
      GraphQLTypes.GraphQLDepartureRowStoptimesArgs args =
        new GraphQLTypes.GraphQLDepartureRowStoptimesArgs(environment.getArguments());

      return getSource(environment).getStoptimes(
        getTransitService(environment),
        GraphQLUtils.getTimeOrNow(args.getGraphQLStartTime()),
        Duration.ofSeconds(args.getGraphQLTimeRange()),
        args.getGraphQLNumberOfDepartures(),
        args.getGraphQLOmitNonPickups() ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH
      );
    };
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private PatternAtStop getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
