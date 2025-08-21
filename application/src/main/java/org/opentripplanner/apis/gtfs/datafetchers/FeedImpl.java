package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.model.FeedPublisher;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TransitService;

public class FeedImpl implements GraphQLDataFetchers.GraphQLFeed {

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> getAgencies(environment);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> {
      TransitAlertService alertService = getTransitService(environment).getTransitAlertService();
      var args = new GraphQLTypes.GraphQLFeedAlertsArgs(environment.getArguments());
      List<GraphQLTypes.GraphQLFeedAlertType> types = args.getGraphQLTypes();
      if (types != null) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        types.forEach(type -> {
          switch (type) {
            case AGENCIES:
              List<Agency> agencies = getAgencies(environment);
              agencies.forEach(agency ->
                alerts.addAll(alertService.getAgencyAlerts(agency.getId()))
              );
              break;
            case ROUTE_TYPES:
              alertService
                .getAllAlerts()
                .stream()
                .filter(alert ->
                  alert
                    .entities()
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

  @Override
  public DataFetcher<FeedPublisher> publisher() {
    return environment -> {
      String id = getSource(environment);
      return new FeedPublisher(
        getTransitService(environment).getFeedInfo(id).getPublisherName(),
        getTransitService(environment).getFeedInfo(id).getPublisherUrl()
      );
    };
  }

  private List<Agency> getAgencies(DataFetchingEnvironment environment) {
    String id = getSource(environment);
    return getTransitService(environment)
      .listAgencies()
      .stream()
      .filter(agency -> agency.getId().getFeedId().equals(id))
      .collect(Collectors.toList());
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private String getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
