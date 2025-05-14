package org.opentripplanner.routing.algorithm.via;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.ViaLocationDeprecated;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.error.RoutingValidationException;

public class ViaRoutingWorker {

  private final RouteViaRequest viaRequest;
  private final Function<RouteRequest, RoutingResponse> routingWorker;
  // TODO: This constant already exists in RoutingRequestToFilterChainMapper, so when POC is done use only constant
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

  private RouteRequestBuilder currentSegmentRequest;

  public ViaRoutingWorker(
    RouteViaRequest viaRequest,
    Function<RouteRequest, RoutingResponse> routingWorker
  ) {
    // The request is cloned here and put in a field, as getRoutingResponse loops over all the
    // segments, modifying the request based on the result from each segment.
    this.currentSegmentRequest = viaRequest.routeRequestFirstSegment();
    this.viaRequest = viaRequest;
    this.routingWorker = routingWorker;
  }

  public ViaRoutingResponse route() {
    //Loop over Via, for each cycle change from/to and JourneyRequest.
    var result = viaRequest.viaSegment().stream().map(v -> routeSegment(v)).toList();

    return combineRoutingResponse(result);
  }

  /**
   * Set to point and search trips. Return result with itineraries and prepare request for next
   * search.
   */
  private RoutingResponse routeSegment(RouteViaRequest.ViaSegment v) {
    // If viaLocation is null then we are at last search
    if (v.viaLocation() != null) {
      this.currentSegmentRequest.withTo(v.viaLocation().point());
    } else {
      this.currentSegmentRequest.withTo(viaRequest.to());
    }

    this.currentSegmentRequest.withJourney(v.journeyRequest());

    var response = this.routingWorker.apply(this.currentSegmentRequest.buildRequest());

    if (v.viaLocation() == null) {
      return response;
    }

    // Prepare next search
    var firstArrival = firstArrival(response).orElseThrow(this::createRoutingException);
    var lastArrival = lastArrival(response).orElseThrow(this::createRoutingException);
    var maxSlack = v.viaLocation().maxSlack();
    var searchWindow = Duration.between(firstArrival, lastArrival).plus(maxSlack);

    this.currentSegmentRequest.withFrom(v.viaLocation().point());
    this.currentSegmentRequest.withDateTime(
        firstArrival.plus(v.viaLocation().minSlack()).toInstant()
      );
    this.currentSegmentRequest.withSearchWindow(searchWindow);
    this.currentSegmentRequest.withNumItineraries(MAX_NUMBER_OF_ITINERARIES);

    return response;
  }

  /**
   * For each itinerary from list element find itinerraries that can be combined to next list
   * element.
   */
  private ViaRoutingResponse combineRoutingResponse(List<RoutingResponse> routingResponses) {
    var res = new HashMap<Itinerary, List<Itinerary>>();
    var routingErrors = new ArrayList<RoutingError>();

    for (int i = 0; i < routingResponses.size() - 1; i++) {
      var errors = routingResponses.get(i).getRoutingErrors();

      if (errors != null) {
        routingErrors.addAll(errors);
      }

      for (Itinerary itinerary : routingResponses.get(i).getTripPlan().itineraries) {
        var filteredTransits = filterTransits(
          itinerary,
          routingResponses.get(i + 1).getTripPlan().itineraries,
          this.viaRequest.viaSegment().get(i).viaLocation()
        );

        if (!filteredTransits.isEmpty()) {
          res.put(itinerary, filteredTransits);
        }
      }
    }

    return new ViaRoutingResponse(res, routingResponses, routingErrors);
  }

  private List<Itinerary> filterTransits(
    Itinerary i,
    List<Itinerary> itineraries,
    ViaLocationDeprecated viaLocation
  ) {
    return itineraries.stream().filter(withinSlackTest(i, viaLocation)).toList();
  }

  /**
   * Only allow departures within min/max slack time.
   */
  private Predicate<Itinerary> withinSlackTest(Itinerary i, ViaLocationDeprecated v) {
    var earliestDeparturetime = i.endTime().plus(v.minSlack());
    var latestDeparturetime = i.endTime().plus(v.maxSlack());

    // Not before earlist and not after latest to include equal time
    return j ->
      !j.startTime().isBefore(earliestDeparturetime) && !j.startTime().isAfter(latestDeparturetime);
  }

  private Optional<ZonedDateTime> firstArrival(RoutingResponse response) {
    return Optional.ofNullable(response.getTripPlan())
      .map(t -> t.itineraries)
      .flatMap(i -> i.stream().min(Comparator.comparing(Itinerary::endTime)))
      .map(Itinerary::endTime);
  }

  private Optional<ZonedDateTime> lastArrival(RoutingResponse response) {
    return Optional.ofNullable(response.getTripPlan())
      .map(t -> t.itineraries)
      .flatMap(i -> i.stream().max(Comparator.comparing(Itinerary::endTime)))
      .map(Itinerary::endTime);
  }

  private RoutingValidationException createRoutingException() {
    return new RoutingValidationException(
      List.of(
        new RoutingError(
          RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW,
          InputField.INTERMEDIATE_PLACE
        )
      )
    );
  }
}
