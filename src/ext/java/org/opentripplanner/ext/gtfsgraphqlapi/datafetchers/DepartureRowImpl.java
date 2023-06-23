package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import org.opentripplanner.ext.gtfsgraphqlapi.GraphQLRequestContext;
import org.opentripplanner.ext.gtfsgraphqlapi.GraphQLUtils;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitService;

public class DepartureRowImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLDepartureRow {

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
      LegacyGraphQLTypes.LegacyGraphQLDepartureRowStoptimesArgs args = new LegacyGraphQLTypes.LegacyGraphQLDepartureRowStoptimesArgs(
        environment.getArguments()
      );

      return getSource(environment)
        .getStoptimes(
          getTransitService(environment),
          GraphQLUtils.getTimeOrNow(args.getLegacyGraphQLStartTime()),
          Duration.ofSeconds(args.getLegacyGraphQLTimeRange()),
          args.getLegacyGraphQLNumberOfDepartures(),
          args.getLegacyGraphQLOmitNonPickups()
            ? ArrivalDeparture.DEPARTURES
            : ArrivalDeparture.BOTH
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
