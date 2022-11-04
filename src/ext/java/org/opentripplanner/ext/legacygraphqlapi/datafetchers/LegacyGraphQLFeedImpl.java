package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLFeedImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLFeed {

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> getAgencies(environment);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getTransitService(environment).getTransitAlertService();
      var args = new LegacyGraphQLTypes.LegacyGraphQLFeedAlertsArgs(environment.getArguments());
      Iterable<LegacyGraphQLTypes.LegacyGraphQLFeedAlertType> types = args.getLegacyGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case AGENCIES:
              List<Agency> agencies = getAgencies(environment);
              agencies.forEach(agency -> alerts.addAll(alertService.getAgencyAlerts(agency.getId()))
              );
              break;
            case ROUTE_TYPES:
              alertService
                .getAllAlerts()
                .stream()
                .filter(alert ->
                  alert
                    .getEntities()
                    .stream()
                    .filter(EntitySelector.RouteType.class::isInstance)
                    .map(EntitySelector.RouteType.class::cast)
                    .anyMatch(entity -> entity.feedId().equals(getSource(environment)))
                )
                .forEach(alerts::add);
              break;
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      }
      return null;
    };
  }

  @Override
  public DataFetcher<String> feedId() {
    return this::getSource;
  }

  private List<Agency> getAgencies(DataFetchingEnvironment environment) {
    String id = getSource(environment);
    return getTransitService(environment)
      .getAgencies()
      .stream()
      .filter(agency -> agency.getId().getFeedId().equals(id))
      .collect(Collectors.toList());
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getTransitService();
  }

  private String getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
