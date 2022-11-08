package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
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
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLAgencyImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLAgency {

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getTransitService(environment).getTransitAlertService();
      var args = new LegacyGraphQLTypes.LegacyGraphQLAgencyAlertsArgs(environment.getArguments());
      Iterable<LegacyGraphQLTypes.LegacyGraphQLAgencyAlertType> types = args.getLegacyGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case AGENCY:
              alerts.addAll(alertService.getAgencyAlerts(getSource(environment).getId()));
              break;
            case ROUTE_TYPES:
              alertService
                .getAllAlerts()
                .stream()
                .filter(alert ->
                  alert
                    .getEntities()
                    .stream()
                    .filter(EntitySelector.RouteTypeAndAgency.class::isInstance)
                    .map(EntitySelector.RouteTypeAndAgency.class::cast)
                    .anyMatch(entity -> entity.agencyId().equals(getSource(environment).getId()))
                )
                .forEach(alerts::add);
              break;
            case ROUTES:
              getRoutes(environment)
                .forEach(route -> alerts.addAll(alertService.getRouteAlerts(route.getId())));
              break;
          }
        });
        return alerts.stream().distinct().collect(Collectors.toList());
      } else {
        return alertService.getAgencyAlerts(getSource(environment).getId());
      }
    };
  }

  @Override
  public DataFetcher<String> fareUrl() {
    return environment -> getSource(environment).getFareUrl();
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("Agency", getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<String> lang() {
    return environment -> getSource(environment).getLang();
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).getName();
  }

  @Override
  public DataFetcher<String> phone() {
    return environment -> getSource(environment).getPhone();
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> getRoutes(environment);
  }

  @Override
  public DataFetcher<String> timezone() {
    return environment -> getSource(environment).getTimezone().getId();
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> getSource(environment).getUrl();
  }

  private List<Route> getRoutes(DataFetchingEnvironment environment) {
    return getTransitService(environment)
      .getAllRoutes()
      .stream()
      .filter(route -> route.getAgency().equals(getSource(environment)))
      .collect(Collectors.toList());
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getTransitService();
  }

  private Agency getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
