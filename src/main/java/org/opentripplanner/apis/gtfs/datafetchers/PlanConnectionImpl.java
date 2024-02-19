package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultEdge;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.PlanLabeledLocation;
import org.opentripplanner.apis.gtfs.model.PlanPageInfo;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.transit.service.TransitService;

public class PlanConnectionImpl implements GraphQLDataFetchers.GraphQLPlanConnection {

  @Override
  public DataFetcher<OffsetDateTime> searchDateTime() {
    return environment -> {
      var transitService = getTransitService(environment);
      var instant = getSource(environment).getTripPlan().date;
      return instant.atOffset(transitService.getTimeZone().getRules().getOffset(instant));
    };
  }

  @Override
  public DataFetcher<PlanLabeledLocation> origin() {
    return environment -> getLocation(getSource(environment).getTripPlan().from);
  }

  @Override
  public DataFetcher<Iterable<DefaultEdge<Itinerary>>> edges() {
    return environment ->
      getSource(environment)
        .getTripPlan()
        .itineraries.stream()
        .map(itinerary -> new DefaultEdge<>(itinerary, new DefaultConnectionCursor("NoCursor")))
        .toList();
  }

  @Override
  public DataFetcher<Iterable<RoutingError>> routingErrors() {
    return environment -> getSource(environment).getRoutingErrors();
  }

  @Override
  public DataFetcher<PlanLabeledLocation> destination() {
    return environment -> getLocation(getSource(environment).getTripPlan().to);
  }

  @Override
  public DataFetcher<PlanPageInfo> pageInfo() {
    return environment -> {
      var startCursor = getSource(environment).getNextPageCursor() != null
        ? getSource(environment).getPreviousPageCursor().encode()
        : null;
      ConnectionCursor startConnectionCursor = null;
      if (startCursor != null) {
        startConnectionCursor = new DefaultConnectionCursor(startCursor);
      }
      var endCursor = getSource(environment).getPreviousPageCursor() != null
        ? getSource(environment).getNextPageCursor().encode()
        : null;
      ConnectionCursor endConnectionCursor = null;
      if (endCursor != null) {
        endConnectionCursor = new DefaultConnectionCursor(endCursor);
      }
      Duration searchWindowUsed = null;
      var metadata = getSource(environment).getMetadata();
      if (metadata != null) {
        searchWindowUsed = metadata.searchWindowUsed;
      }
      return new PlanPageInfo(
        startConnectionCursor,
        endConnectionCursor,
        startCursor != null,
        endCursor != null,
        searchWindowUsed
      );
    };
  }

  private PlanLabeledLocation getLocation(Place place) {
    Object location = null;
    var stop = place.stop;
    var coordinate = place.coordinate;
    if (stop != null) {
      location = stop;
    } else if (coordinate != null) {
      location = coordinate;
    }
    // TODO make label field that is only the label
    return new PlanLabeledLocation(location, place.name.toString());
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private RoutingResponse getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
