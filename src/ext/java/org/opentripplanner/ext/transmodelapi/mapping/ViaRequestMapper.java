package org.opentripplanner.ext.transmodelapi.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.transmodelapi.TransmodelGraphQLUtils;
import org.opentripplanner.ext.transmodelapi.TransmodelRequestContext;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.ViaLocation;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public class ViaRequestMapper {

  /**
   * Create a RouteViaRequest from the input fields of the viaTrip query arguments.
   */
  public static RouteViaRequest createRouteViaRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    OtpServerRequestContext serverContext = context.getServerContext();
    RouteRequest request = serverContext.defaultRouteRequest();

    List<ViaLocation> vias =
      ((List<Map<String, Object>>) environment.getArgument("via")).stream()
        .map(viaLocation ->
          new ViaLocation(
            GenericLocationMapper.toGenericLocation(viaLocation),
            false,
            (Duration) viaLocation.get("minSlack"),
            (Duration) viaLocation.get("maxSlack")
          )
        )
        .toList();

    List<JourneyRequest> requests = environment.containsArgument("requests")
      ? ((List<Map<String, Object>>) environment.getArgument("requests")).stream()
        .map(viaRequest -> {
          JourneyRequest journey = request.journey().clone();
          if (viaRequest.containsKey("modes")) {
            Map<String, Object> modesInput = (Map<String, Object>) viaRequest.get("modes");
            journey.setModes(RequestModesMapper.mapRequestModes(modesInput));
          }
          if (viaRequest.containsKey("filters")) {
            List<Map<String, ?>> filters = (List<Map<String, ?>>) viaRequest.get("filters");
            journey.transit().setFilters(FilterMapper.mapFilterNewWay(filters));
          }
          return journey;
        })
        .toList()
      : Collections.nCopies(vias.size() + 1, request.journey());

    return RouteViaRequest
      .of(vias, requests)
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
      .withLocale(TransmodelGraphQLUtils.getLocale(environment))
      .build();
  }
}
