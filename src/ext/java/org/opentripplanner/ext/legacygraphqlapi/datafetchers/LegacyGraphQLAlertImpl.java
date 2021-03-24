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
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.util.TranslatedString;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class LegacyGraphQLAlertImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLAlert {
  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId(
        "Alert",
        getSource(environment).getId()
    );
  }

  // TODO
  @Override
  public DataFetcher<Integer> alertHash() {
    return environment -> null;
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

  // TODO
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
      return text instanceof TranslatedString
          ? ((TranslatedString) text).getTranslations()
          : Collections.emptyList();
    };
  }

  @Override
  public DataFetcher<String> alertDescriptionText() {
    return environment -> getSource(environment).alertDescriptionText.toString(
        environment.getLocale());
  }

  //TODO
  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertDescriptionTextTranslations() {
    return environment -> null;
  }

  @Override
  public DataFetcher<String> alertUrl() {
    return environment -> getSource(environment).alertUrl.toString(
        environment.getLocale());
  }

  //TODO
  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertUrlTranslations() {
    return environment -> null;
  }

  //TODO
  @Override
  public DataFetcher<String> alertEffect() {
    return environment -> null;
  }

  //TODO
  @Override
  public DataFetcher<String> alertCause() {
    return environment -> null;
  }

  @Override
  public DataFetcher<String> alertSeverityLevel() {
    return environment -> getSource(environment).severity;
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

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TransitAlert getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
