package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLUtils;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLDepartureRowImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLDepartureRow {

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
          LegacyGraphQLUtils.getTimeOrNow(args.getLegacyGraphQLStartTime()),
          Duration.ofSeconds(args.getLegacyGraphQLTimeRange()),
          args.getLegacyGraphQLNumberOfDepartures(),
          args.getLegacyGraphQLOmitNonPickups()
            ? ArrivalDeparture.DEPARTURES
            : ArrivalDeparture.BOTH
        );
    };
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getTransitService();
  }

  private PatternAtStop getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
