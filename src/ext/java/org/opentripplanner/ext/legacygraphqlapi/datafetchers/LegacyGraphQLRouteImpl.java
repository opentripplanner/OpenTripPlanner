package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.TransitAlert;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyGraphQLRouteImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLRoute {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("Route",
        getSource(environment).getId().toString()
    );
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
  }

  @Override
  public DataFetcher<String> shortName() {
    return environment -> getSource(environment).getShortName();
  }

  @Override
  public DataFetcher<String> longName() {
    return environment -> getSource(environment).getLongName();
  }

  @Override
  public DataFetcher<String> mode() {
    return environment -> getSource(environment).getMode().name();
  }

  @Override
  public DataFetcher<Integer> type() {
    return environment -> getSource(environment).getType();
  }

  @Override
  public DataFetcher<String> desc() {
    return environment -> getSource(environment).getDesc();
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> getSource(environment).getUrl();
  }

  @Override
  public DataFetcher<String> color() {
    return environment -> getSource(environment).getColor();
  }

  @Override
  public DataFetcher<String> textColor() {
    return environment -> getSource(environment).getTextColor();
  }

  @Override
  public DataFetcher<String> bikesAllowed() {
    return environment -> {
      switch (getSource(environment).getBikesAllowed()) {
        case UNKNOWN: return "NO_INFORMATION";
        case ALLOWED: return "POSSIBLE";
        case NOT_ALLOWED: return "NOT_POSSIBLE";
        default: return null;
      }
    };
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> getRoutingService(environment)
        .getPatternsForRoute()
        .get(getSource(environment));
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> getRoutingService(environment)
        .getPatternsForRoute()
        .get(getSource(environment))
        .stream()
        .map(TripPattern::getStops)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return environment -> getRoutingService(environment)
        .getPatternsForRoute()
        .get(getSource(environment))
        .stream()
        .map(TripPattern::getTrips)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  //TODO
  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> List.of();
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private Route getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
