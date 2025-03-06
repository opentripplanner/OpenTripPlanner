package org.opentripplanner.apis.transmodel.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.ViaLocationDeprecated;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * This class maps a GraphQL viaTrip query into a {@link RouteViaRequest}
 */
public class ViaRequestMapper {

  /**
   * Create a RouteViaRequest from the input fields of the viaTrip query arguments.
   */
  public static RouteViaRequest createRouteViaRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    OtpServerRequestContext serverContext = context.getServerContext();
    RouteRequest request = serverContext.defaultRouteRequest();

    List<Map<String, Object>> viaInput = environment.getArgument("via");
    List<ViaLocationDeprecated> vias = viaInput
      .stream()
      .map(ViaLocationDeprecatedMapper::mapViaLocation)
      .toList();

    List<JourneyRequest> requests;
    if (environment.containsArgument("segments")) {
      List<Map<String, Object>> segments = environment.getArgument("segments");
      requests = segments
        .stream()
        .map(viaRequest -> ViaSegmentMapper.mapViaSegment(request, viaRequest))
        .toList();
    } else {
      requests = Collections.nCopies(vias.size() + 1, request.journey());
    }

    return RouteViaRequest.of(vias, requests)
      .withDateTime(
        Instant.ofEpochMilli(
          environment.getArgumentOrDefault("dateTime", request.dateTime().toEpochMilli())
        )
      )
      .withSearchWindow(environment.getArgumentOrDefault("searchWindow", request.searchWindow()))
      .withFrom(GenericLocationMapper.toGenericLocation(environment.getArgument("from")))
      .withTo(GenericLocationMapper.toGenericLocation(environment.getArgument("to")))
      .withNumItineraries(
        environment.getArgumentOrDefault("numTripPatterns", request.numItineraries())
      )
      .withWheelchair(
        environment.getArgumentOrDefault("wheelchairAccessible", request.wheelchair())
      )
      .withLocale(GraphQLUtils.getLocale(environment))
      .build();
  }
}
