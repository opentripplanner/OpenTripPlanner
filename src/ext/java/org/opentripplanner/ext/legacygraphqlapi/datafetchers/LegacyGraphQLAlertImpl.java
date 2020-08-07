package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.AlertPatch;
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
    return environment -> getRoutingService(environment)
        .getAgencyForId(getSource(environment).getAgency());
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getRoutingService(environment)
        .getRouteForId(getSource(environment).getRoute());
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getRoutingService(environment)
        .getTripForId()
        .get(getSource(environment).getTrip());
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getRoutingService(environment)
        .getStopForId(getSource(environment).getStop());
  }

  // TODO
  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> Collections.emptyList();
  }

  @Override
  public DataFetcher<String> alertHeaderText() {
    return environment -> getSource(environment).getAlert().alertHeaderText.toString(
        environment.getLocale());
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertHeaderTextTranslations() {
    return environment -> {
      var text = getSource(environment).getAlert().alertHeaderText;
      return text instanceof TranslatedString
          ? ((TranslatedString) text).getTranslations()
          : Collections.emptyList();
    };
  }

  @Override
  public DataFetcher<String> alertDescriptionText() {
    return environment -> getSource(environment).getAlert().alertDescriptionText.toString(
        environment.getLocale());
  }

  //TODO
  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertDescriptionTextTranslations() {
    return environment -> null;
  }

  @Override
  public DataFetcher<String> alertUrl() {
    return environment -> getSource(environment).getAlert().alertUrl.toString(
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
    return environment -> getSource(environment).getAlert().severity;
  }

  @Override
  public DataFetcher<Long> effectiveStartDate() {
    return environment -> {
      Date effectiveStartDate = getSource(environment).getAlert().effectiveStartDate;
      if (effectiveStartDate == null) return null;
      return effectiveStartDate.getTime() / 1000;
    };
  }

  @Override
  public DataFetcher<Long> effectiveEndDate() {
    return environment -> {
      Date effectiveEndDate = getSource(environment).getAlert().effectiveEndDate;
      if (effectiveEndDate == null) return null;
      return effectiveEndDate.getTime() / 1000;
    };
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private AlertPatch getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
