package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLCauseMapper.getLegacyGraphQLCause;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLEffectMapper.getLegacyGraphQLEffect;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLSeverityMapper.getLegacyGraphQLSeverity;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteTypeModel;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnRouteModel;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnTripModel;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLUnknownModel;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.EntitySelector.DirectionAndRoute;
import org.opentripplanner.routing.alertpatch.EntitySelector.RouteType;
import org.opentripplanner.routing.alertpatch.EntitySelector.StopAndRouteOrTripKey;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.TranslatedString;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class LegacyGraphQLAlertImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLAlert {
  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId(
        "Alert",
        getSource(environment).getId()
    );
  }

  @Override
  public DataFetcher<Integer> alertHash() {
    return environment -> {
      TransitAlert alert = getSource(environment);
      return Objects.hash(
              alert.alertDescriptionText, alert.alertHeaderText, alert.alertUrl, alert.cause,
              alert.effect, alert.severity
      );
    };
  }

  @Override
  public DataFetcher<String> feed() {
    return environment -> getSource(environment).getFeedId();
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment)
        .getEntities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Agency)
        .findAny()
        .map(EntitySelector.Agency.class::cast)
        .map(entitySelector -> getRoutingService(environment).getAgencyForId(entitySelector.agencyId))
        .orElse(null);
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment)
        .getEntities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
        .findAny()
        .map(EntitySelector.Route.class::cast)
        .map(entitySelector -> getRoutingService(environment).getRouteForId(entitySelector.routeId))
        .orElse(null);
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment)
        .getEntities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Trip)
        .findAny()
        .map(EntitySelector.Trip.class::cast)
        .map(entitySelector -> getRoutingService(environment).getTripForId().get(entitySelector.tripId))
        .orElse(null);
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment)
        .getEntities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
        .findAny()
        .map(EntitySelector.Stop.class::cast)
        .map(entitySelector -> getRoutingService(environment).getStopForId(entitySelector.stopId))
        .orElse(null);
  }

  // This is deprecated
  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> Collections.emptyList();
  }

  @Override
  public DataFetcher<String> alertHeaderText() {
    return environment -> getSource(environment).alertHeaderText.toString(
        environment.getLocale());
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertHeaderTextTranslations() {
    return environment -> {
      var text = getSource(environment).alertHeaderText;
      return getTranslations(text);
    };
  }

  @Override
  public DataFetcher<String> alertDescriptionText() {
    return environment -> getSource(environment).alertDescriptionText.toString(
        environment.getLocale());
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertDescriptionTextTranslations() {
    return environment -> {
      var text = getSource(environment).alertDescriptionText;
      return getTranslations(text);
    };
  }

  @Override
  public DataFetcher<String> alertUrl() {
    return environment -> {
      var alertUrl = getSource(environment).alertUrl;
      return alertUrl == null ? null : alertUrl.toString(environment.getLocale());
    };
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertUrlTranslations() {
    return environment -> {
      var url = getSource(environment).alertUrl;
      return getTranslations(url);
    };
  }

  @Override
  public DataFetcher<String> alertEffect() {
    return environment -> getLegacyGraphQLEffect(getSource(environment).effect);
  }

  @Override
  public DataFetcher<String> alertCause() {
    return environment -> getLegacyGraphQLCause(getSource(environment).cause);
  }

  @Override
  public DataFetcher<String> alertSeverityLevel() {
    return environment -> getLegacyGraphQLSeverity(getSource(environment).severity);
  }

  @Override
  public DataFetcher<Long> effectiveStartDate() {
    return environment -> {
      Date effectiveStartDate = getSource(environment).getEffectiveStartDate();
      if (effectiveStartDate == null) { return null; }
      return effectiveStartDate.getTime() / 1000;
    };
  }

  @Override
  public DataFetcher<Long> effectiveEndDate() {
    return environment -> {
      Date effectiveEndDate = getSource(environment).getEffectiveEndDate();
      if (effectiveEndDate == null) { return null; }
      return effectiveEndDate.getTime() / 1000;
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> entities() {
    return environment -> getSource(environment).getEntities().stream()
            .map(entitySelector -> {
              if (entitySelector instanceof EntitySelector.Stop) {
                FeedScopedId id = ((EntitySelector.Stop) entitySelector).stopId;
                StopLocation stop = getRoutingService(environment).getStopForId(id);
                return List.of(getAlertEntityOrUnknown(stop, id.toString(), "stop"));
              }
              if (entitySelector instanceof EntitySelector.Agency) {
                FeedScopedId id = ((EntitySelector.Agency) entitySelector).agencyId;
                Agency agency = getRoutingService(environment).getAgencyForId(id);
                return List.of(getAlertEntityOrUnknown(agency, id.toString(), "agency"));
              }
              if (entitySelector instanceof EntitySelector.Route) {
                FeedScopedId id = ((EntitySelector.Route) entitySelector).routeId;
                Route route = getRoutingService(environment).getRouteForId(id);
                return List.of(getAlertEntityOrUnknown(route, id.toString(), "route"));
              }
              if (entitySelector instanceof EntitySelector.Trip) {
                FeedScopedId id = ((EntitySelector.Trip) entitySelector).tripId;
                Trip trip = getRoutingService(environment).getTripForId().get(id);
                return List.of(getAlertEntityOrUnknown(trip, id.toString(), "trip"));
              }
              if (entitySelector instanceof EntitySelector.StopAndRoute) {
                StopAndRouteOrTripKey stopAndRouteKey =
                        ((EntitySelector.StopAndRoute) entitySelector).stopAndRoute;
                FeedScopedId stopId = stopAndRouteKey.stop;
                FeedScopedId routeId = stopAndRouteKey.routeOrTrip;
                StopLocation stop = getRoutingService(environment).getStopForId(stopId);
                Route route = getRoutingService(environment).getRouteForId(routeId);
                return List.of(stop != null && route != null
                        ? new LegacyGraphQLStopOnRouteModel(
                        stop, route)
                        : getUnknownForAlertEntityPair(stop, route, stopId.toString(),
                                routeId.toString(), "stop", "route"
                        ));
              }
              if (entitySelector instanceof EntitySelector.StopAndTrip) {
                StopAndRouteOrTripKey stopAndTripKey =
                        ((EntitySelector.StopAndTrip) entitySelector).stopAndTrip;
                FeedScopedId stopId = stopAndTripKey.stop;
                FeedScopedId tripId = stopAndTripKey.routeOrTrip;
                StopLocation stop = getRoutingService(environment).getStopForId(stopId);
                Trip trip = getRoutingService(environment).getTripForId().get(tripId);
                return List.of(stop != null && trip != null
                        ? new LegacyGraphQLStopOnTripModel(stop, trip)
                        : getUnknownForAlertEntityPair(stop, trip, stopId.toString(),
                                tripId.toString(), "stop", "trip"
                        ));
              }
              if (entitySelector instanceof EntitySelector.RouteTypeAndAgency) {
                FeedScopedId agencyId =
                        ((EntitySelector.RouteTypeAndAgency) entitySelector).agencyId;
                int routeType = ((EntitySelector.RouteTypeAndAgency) entitySelector).routeType;
                Agency agency = getRoutingService(environment).getAgencyForId(agencyId);
                return List.of(agency != null
                        ? new LegacyGraphQLRouteTypeModel(agency, routeType, agency.getId().getFeedId())
                        : getUnknownForAlertEntityPair(agency, routeType, agency.toString(),
                                Integer.toString(routeType), "agency", "route type"
                        ));
              }
              if (entitySelector instanceof EntitySelector.RouteType) {
                int routeType = ((EntitySelector.RouteType) entitySelector).routeType;
                String feedId = ((EntitySelector.RouteType) entitySelector).feedId;
                return List.of(new LegacyGraphQLRouteTypeModel(null, routeType, feedId));
              }
              if (entitySelector instanceof EntitySelector.DirectionAndRoute) {
                int directionId = ((DirectionAndRoute) entitySelector).directionId;
                FeedScopedId routeId = ((EntitySelector.DirectionAndRoute) entitySelector).routeId;
                Route route = getRoutingService(environment).getRouteForId(routeId);
                return route != null
                        ? getRoutingService(environment).getPatternsForRoute().get(route).stream().filter(pattern -> pattern.getDirection().gtfsCode == directionId).collect(Collectors.toList())
                        : List.of(getUnknownForAlertEntityPair(route, directionId, route.toString(),
                                Integer.toString(directionId), "route", "direction"
                        ));
              }
              if (entitySelector instanceof EntitySelector.Unknown) {
                return List.of(new LegacyGraphQLUnknownModel(
                        ((EntitySelector.Unknown) entitySelector).description));
              }
              return List.of();
            })
            .flatMap(list -> list.stream())
            .map(Object.class::cast)
            .collect(Collectors.toList());
  }

  private Object getAlertEntityOrUnknown(Object entity, String id, String type) {
    if (entity != null) {
      return entity;
    }
    return new LegacyGraphQLUnknownModel(
            String.format("Alert's entity selector was %s with id %s but the %s doesn't exist.",
                    type, id, type
            ));
  }

  private Object getUnknownForAlertEntityPair(
          Object entityA,
          Object entityB,
          String idA,
          String idB,
          String typeA,
          String typeB
  ) {
    if (entityA == null && entityB == null) {
      return new LegacyGraphQLUnknownModel(String.format(
              "Alert's entity selector was %s with id %s and %s with id %s but the %s and %s don't exist.",
              typeA, idA, typeB, idB, typeA, typeB
      ));
    }
    if (entityA == null) {
      return new LegacyGraphQLUnknownModel(String.format(
              "Alert's entity selector was %s with id %s and %s with id %s but the %s doesn't exist.",
              typeA, idA, typeB, idB, typeA
      ));
    }
    return new LegacyGraphQLUnknownModel(String.format(
            "Alert's entity selector was %s with id %s and %s with id %s but the %s doesn't exist.",
            typeA, idA, typeB, idB, typeB
    ));
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TransitAlert getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  private Iterable<Map.Entry<String, String>> getTranslations(I18NString text) {
    return text instanceof TranslatedString
            ? ((TranslatedString) text).getTranslations()
            : Collections.emptyList();
  }
}
