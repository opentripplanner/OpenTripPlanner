package org.opentripplanner.ext.ojp.service;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.OJPTripRequestStructure;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.PlaceRefStructure;
import de.vdv.ojp20.siri.StopPointRefStructure;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.ojp.mapping.RouteRequestMapper;
import org.opentripplanner.ext.ojp.mapping.StopEventParamsMapper;
import org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper;
import org.opentripplanner.ext.ojp.mapping.TripResponseMapper;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Takes raw OJP requests, extracts information and forwards it to the underlying services.
 */
public class OjpService {

  private final CallAtStopService callAtStopService;
  private final RoutingService routingService;
  private final FeedScopedIdMapper idMapper;
  private final ZoneId zoneId;

  public OjpService(
    CallAtStopService callAtStopService,
    RoutingService routingService,
    FeedScopedIdMapper idMapper,
    ZoneId zoneId
  ) {
    this.callAtStopService = callAtStopService;
    this.routingService = routingService;
    this.idMapper = idMapper;
    this.zoneId = zoneId;
  }

  public OJP handleStopEventRequest(OJPStopEventRequestStructure ser) {
    var stopId = stopPointRef(ser);
    var coordinate = coordinate(ser);

    var seMapper = new StopEventParamsMapper(zoneId, idMapper);
    var params = seMapper.extractStopEventParams(ser);

    List<CallAtStop> callsAtStop = List.of();
    if (stopId.isPresent()) {
      callsAtStop = callAtStopService.findCallsAtStop(stopId.get(), params);
    } else if (coordinate.isPresent()) {
      callsAtStop = callAtStopService.findCallsAtStop(coordinate.get(), params);
    }
    var optional = StopEventParamsMapper.mapOptionalFeatures(ser.getParams());
    var mapper = new StopEventResponseMapper(
      optional,
      zoneId,
      idMapper,
      callAtStopService::resolveLanguage
    );
    return mapper.mapCalls(callsAtStop, ZonedDateTime.now());
  }

  public OJP handleTripRequest(OJPTripRequestStructure tr, RouteRequest routeRequest) {
    var optionalFeatures = RouteRequestMapper.optionalFeatures(tr);
    var rr = new RouteRequestMapper(idMapper, routeRequest).map(tr);
    var tripPlan = routingService.route(rr);
    var mapper = new TripResponseMapper(idMapper, optionalFeatures);
    return mapper.mapTripPlan(tripPlan, ZonedDateTime.now());
  }

  private Optional<FeedScopedId> stopPointRef(OJPStopEventRequestStructure ser) {
    return placeRefStructure(ser)
      .map(PlaceRefStructure::getStopPointRef)
      .map(StopPointRefStructure::getValue)
      .map(idMapper::parse);
  }

  private Optional<WgsCoordinate> coordinate(OJPStopEventRequestStructure ser) {
    return placeRefStructure(ser)
      .map(PlaceRefStructure::getGeoPosition)
      .map(c -> new WgsCoordinate(c.getLatitude(), c.getLongitude()));
  }

  private static Optional<PlaceRefStructure> placeRefStructure(OJPStopEventRequestStructure ser) {
    return Optional.ofNullable(ser.getLocation()).map(PlaceContextStructure::getPlaceRef);
  }
}
