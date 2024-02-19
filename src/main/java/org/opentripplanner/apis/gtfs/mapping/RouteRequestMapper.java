package org.opentripplanner.apis.gtfs.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import javax.annotation.Nonnull;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class RouteRequestMapper {

  @Nonnull
  public static RouteRequest toRouteRequest(
    DataFetchingEnvironment environment,
    GraphQLRequestContext context
  ) {
    RouteRequest request = context.defaultRouteRequest();

    var args = new GraphQLTypes.GraphQLQueryTypePlanConnectionArgs(environment.getArguments());
    var dateTime = args.getGraphQLDateTime();
    if (dateTime.getGraphQLEarliestDeparture() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLEarliestDeparture().toInstant());
    } else if (dateTime.getGraphQLLatestArrival() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLLatestArrival().toInstant());
    } else {
      request.setDateTime(Instant.now());
    }
    request.setFrom(parseGenericLocation(args.getGraphQLOrigin()));
    request.setTo(parseGenericLocation(args.getGraphQLDestination()));
    request.setLocale(GraphQLUtils.getLocale(environment, args.getGraphQLLocale()));
    if (args.getGraphQLSearchWindow() != null) {
      request.setSearchWindow(args.getGraphQLSearchWindow());
    }

    if (args.getGraphQLBefore() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLBefore());
      if (args.getGraphQLLast() != null) {
        request.setNumItineraries(args.getGraphQLLast());
      }
    } else if (args.getGraphQLAfter() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLAfter());
      if (args.getGraphQLFirst() != null) {
        request.setNumItineraries(args.getGraphQLFirst());
      }
    } else if (args.getGraphQLNumberOfItineraries() != null) {
      request.setNumItineraries(args.getGraphQLNumberOfItineraries());
    }

    return request;
  }

  private static GenericLocation parseGenericLocation(
    GraphQLTypes.GraphQLPlanLabeledLocationInput locationInput
  ) {
    var stopLocation = locationInput.getGraphQLLocation().getGraphQLStopLocation();
    if (stopLocation.getGraphQLStopLocationId() != null) {
      // TODO implement strict
      var stopId = stopLocation.getGraphQLStopLocationId();
      if (FeedScopedId.isValidString(stopId)) {
        // TODO make label field that is only the label
        return new GenericLocation(
          locationInput.getGraphQLLabel(),
          FeedScopedId.parse(stopId),
          null,
          null
        );
      } else {
        throw new IllegalArgumentException("Stop id %s is not of valid format.".formatted(stopId));
      }
    }

    var coordinate = locationInput.getGraphQLLocation().getGraphQLCoordinate();
    return new GenericLocation(
      locationInput.getGraphQLLabel(),
      null,
      coordinate.getGraphQLLatitude(),
      coordinate.getGraphQLLongitude()
    );
  }
}
