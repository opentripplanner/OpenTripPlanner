package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLCauseMapper.getLegacyGraphQLCause;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLEffectMapper.getLegacyGraphQLEffect;
import static org.opentripplanner.ext.legacygraphqlapi.mapping.LegacyGraphQLSeverityMapper.getLegacyGraphQLSeverity;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnRoute;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnTrip;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLUnknown;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.EntitySelector;
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
                return getRoutingService(environment).getStopForId(((EntitySelector.Stop) entitySelector).stopId);
              }
              if (entitySelector instanceof EntitySelector.Agency) {
                return getRoutingService(environment).getAgencyForId(((EntitySelector.Agency) entitySelector).agencyId);
              }
              if (entitySelector instanceof EntitySelector.Route) {
                return getRoutingService(environment).getRouteForId(((EntitySelector.Route) entitySelector).routeId);
              }
              if (entitySelector instanceof EntitySelector.Trip) {
                return getRoutingService(environment).getTripForId().get(((EntitySelector.Trip) entitySelector).tripId);
              }
              if (entitySelector instanceof EntitySelector.TripPattern) {
                return getRoutingService(environment).getTripPatternForId(((EntitySelector.TripPattern) entitySelector).tripPatternId);
              }
              if (entitySelector instanceof EntitySelector.StopAndRoute) {
                StopAndRouteOrTripKey stopAndRouteKey = ((EntitySelector.StopAndRoute) entitySelector).stopAndRoute;
                Stop stop = stopAndRouteKey == null ? null : getRoutingService(environment).getStopForId(stopAndRouteKey.stop);
                Route route = stopAndRouteKey == null ? null : getRoutingService(environment).getRouteForId(stopAndRouteKey.routeOrTrip);
                return new LegacyGraphQLStopOnRoute(stop, route);
              }
              if (entitySelector instanceof EntitySelector.StopAndTrip) {
                StopAndRouteOrTripKey stopAndTripKey = ((EntitySelector.StopAndTrip) entitySelector).stopAndTrip;
                Stop stop = stopAndTripKey == null ? null : getRoutingService(environment).getStopForId(stopAndTripKey.stop);
                Trip trip = stopAndTripKey == null ? null : getRoutingService(environment).getTripForId().get(stopAndTripKey.routeOrTrip);
                return new LegacyGraphQLStopOnTrip(stop, trip);
              }
              if (entitySelector instanceof EntitySelector.Unknown) {
                return new LegacyGraphQLUnknown(
                        ((EntitySelector.Unknown) entitySelector).description);
              }
              return null;
            })
            .map(Object.class::cast)
            .collect(Collectors.toList());
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
