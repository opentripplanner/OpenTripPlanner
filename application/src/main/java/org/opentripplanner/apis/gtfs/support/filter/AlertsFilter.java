package org.opentripplanner.apis.gtfs.support.filter;

import static org.opentripplanner.apis.gtfs.mapping.AlertCauseMapper.getGraphQLCause;
import static org.opentripplanner.apis.gtfs.mapping.AlertEffectMapper.getGraphQLEffect;
import static org.opentripplanner.apis.gtfs.mapping.SeverityMapper.getGraphQLSeverity;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class AlertsFilter {

  public static List<TransitAlert> filterAlerts(
    Collection<TransitAlert> alerts,
    GraphQLTypes.GraphQLQueryTypeAlertsArgs args
  ) {
    var severities = args.getGraphQLSeverityLevel();
    var effects = args.getGraphQLEffect();
    var causes = args.getGraphQLCause();
    return alerts
      .stream()
      .filter(
        alert ->
          args.getGraphQLFeeds() == null ||
          args.getGraphQLFeeds().contains(alert.getId().getFeedId())
      )
      .filter(
        alert -> severities == null || severities.contains(getGraphQLSeverity(alert.severity()))
      )
      .filter(alert -> effects == null || effects.contains(getGraphQLEffect(alert.effect())))
      .filter(alert -> causes == null || causes.contains(getGraphQLCause(alert.cause())))
      .filter(
        alert ->
          args.getGraphQLRoute() == null ||
          alert
            .entities()
            .stream()
            .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
            .map(EntitySelector.Route.class::cast)
            .anyMatch(route -> args.getGraphQLRoute().contains(route.routeId().toString()))
      )
      .filter(
        alert ->
          args.getGraphQLStop() == null ||
          alert
            .entities()
            .stream()
            .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
            .map(EntitySelector.Stop.class::cast)
            .anyMatch(stop -> args.getGraphQLStop().contains(stop.stopId().toString()))
      )
      .toList();
  }
}
