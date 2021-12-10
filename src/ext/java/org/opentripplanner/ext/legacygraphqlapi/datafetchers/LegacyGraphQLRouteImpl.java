package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;

import java.util.Collection;
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
    return environment -> getStops(environment);
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return environment -> getTrips(environment);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getRoutingService(environment).getTransitAlertService();
      var args = new LegacyGraphQLTypes.LegacyGraphQLRouteAlertsArgs(
              environment.getArguments());
      Iterable<LegacyGraphQLTypes.LegacyGraphQLRouteAlertType> types = args.getLegacyGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.Route)) {
            alerts.addAll(alertService.getRouteAlerts(getSource(environment).getId()));
          }
          else if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.RouteType)) {
            alerts.addAll(alertService.getRouteTypeAlerts(
                    getSource(environment).getType(),
                    getSource(environment).getId()
                            .getFeedId()
            ));
            alerts.addAll(alertService.getRouteTypeAndAgencyAlerts(
                    getSource(environment).getType(),
                    getSource(environment).getAgency().getId()
            ));
          }
          else if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.Agency)) {
            alerts.addAll(alertService.getAgencyAlerts(getSource(environment).getAgency().getId()));
          }
          else if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.Trips)) {
            getTrips(environment).forEach(
                    trip -> alerts.addAll(alertService.getTripAlerts(trip.getId(), null)));
          }
          else if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.StopsOnRoute)) {
            getStops(environment).forEach(stop -> {
              alerts.addAll(alertService.getStopAlerts(((StopLocation) stop).getId()));
              alerts.addAll(alertService.getStopAndRouteAlerts(
                      ((StopLocation) stop).getId(),
                      getSource(environment).getId()
              ));
            });
          }
          else if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.StopsOnTrips)) {
            Iterable<Trip> trips = getTrips(environment);
            getStops(environment).forEach(stop -> {
              trips.forEach(trip -> {
                alerts.addAll(alertService.getStopAndTripAlerts(((StopLocation) stop).getId(),
                        trip.getId(), null
                ));
              });
            });
          }
          else if (type.equals(LegacyGraphQLTypes.LegacyGraphQLRouteAlertType.DirectionOnRoute)) {
            alerts.addAll(
                    alertService.getDirectionAndRouteAlerts(0, getSource(environment).getId()));
            alerts.addAll(
                    alertService.getDirectionAndRouteAlerts(1, getSource(environment).getId()));
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      }
      else {
        return getRoutingService(environment).getTransitAlertService()
                .getRouteAlerts(getSource(environment).getId());
      }
    };
  }

  private Iterable<Object> getStops(DataFetchingEnvironment environment) {
    return getRoutingService(environment)
            .getPatternsForRoute()
            .get(getSource(environment))
            .stream()
            .map(TripPattern::getStops)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
  }

  private Iterable<Trip> getTrips(DataFetchingEnvironment environment) {
    return getRoutingService(environment)
            .getPatternsForRoute()
            .get(getSource(environment))
            .stream()
            .map(TripPattern::getTrips)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
  }

  private TransitAlertService getAlertService(DataFetchingEnvironment environment) {
    return getRoutingService(environment).getTransitAlertService();
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private Route getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
