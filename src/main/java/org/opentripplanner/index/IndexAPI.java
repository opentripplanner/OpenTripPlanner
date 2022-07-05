package org.opentripplanner.index;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.api.mapping.AgencyMapper;
import org.opentripplanner.api.mapping.AlertMapper;
import org.opentripplanner.api.mapping.FeedInfoMapper;
import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.api.mapping.RouteMapper;
import org.opentripplanner.api.mapping.StopMapper;
import org.opentripplanner.api.mapping.StopTimesInPatternMapper;
import org.opentripplanner.api.mapping.TransferMapper;
import org.opentripplanner.api.mapping.TripMapper;
import org.opentripplanner.api.mapping.TripPatternMapper;
import org.opentripplanner.api.mapping.TripTimeMapper;
import org.opentripplanner.api.model.ApiAgency;
import org.opentripplanner.api.model.ApiAlert;
import org.opentripplanner.api.model.ApiFeedInfo;
import org.opentripplanner.api.model.ApiPatternShort;
import org.opentripplanner.api.model.ApiRoute;
import org.opentripplanner.api.model.ApiRouteShort;
import org.opentripplanner.api.model.ApiStop;
import org.opentripplanner.api.model.ApiStopShort;
import org.opentripplanner.api.model.ApiStopTimesInPattern;
import org.opentripplanner.api.model.ApiTransfer;
import org.opentripplanner.api.model.ApiTrip;
import org.opentripplanner.api.model.ApiTripShort;
import org.opentripplanner.api.model.ApiTripTimeShort;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolyline;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{ignoreRouterId}/index") // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class IndexAPI {

  private static final double MAX_STOP_SEARCH_RADIUS = 5000;

  private final OTPServer otpServer;

  /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
  @Context
  UriInfo uriInfo;

  public IndexAPI(
    @Context OTPServer otpServer,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.otpServer = otpServer;
  }

  @GET
  @Path("/feeds")
  public Collection<String> getFeeds() {
    return createTransitService().getFeedIds();
  }

  @GET
  @Path("/feeds/{feedId}")
  public ApiFeedInfo getFeedInfo(@PathParam("feedId") String feedId) {
    ApiFeedInfo feedInfo = FeedInfoMapper.mapToApi(createTransitService().getFeedInfo(feedId));
    return validateExist("FeedInfo", feedInfo, "feedId", feedId);
  }

  /** Return a list of all agencies in the graph. */
  @GET
  @Path("/agencies/{feedId}")
  public Collection<ApiAgency> getAgencies(@PathParam("feedId") String feedId) {
    Collection<Agency> agencies = createTransitService()
      .getAgencies()
      .stream()
      .filter(agency -> agency.getId().getFeedId().equals(feedId))
      .collect(Collectors.toList());
    validateExist("Agency", agencies, "feedId", feedId);
    return AgencyMapper.mapToApi(agencies);
  }

  /** Return specific agency in the graph, by ID. */
  @GET
  @Path("/agencies/{feedId}/{agencyId}")
  public ApiAgency getAgency(
    @PathParam("feedId") String feedId,
    @PathParam("agencyId") String agencyId
  ) {
    Agency agency = getAgency(createTransitService(), feedId, agencyId);
    return AgencyMapper.mapToApi(agency);
  }

  /** Return all routes for the specific agency. */
  @GET
  @Path("/agencies/{feedId}/{agencyId}/routes")
  public Response getAgencyRoutes(
    @PathParam("feedId") String feedId,
    @PathParam("agencyId") String agencyId,
    /** Choose short or long form of results. */
    @QueryParam("detail") @DefaultValue("false") Boolean detail
  ) {
    TransitService transitService = createTransitService();
    Agency agency = getAgency(transitService, feedId, agencyId);

    Collection<Route> routes = transitService
      .getAllRoutes()
      .stream()
      .filter(r -> r.getAgency() == agency)
      .collect(Collectors.toList());

    if (detail) {
      return Response.status(Status.OK).entity(RouteMapper.mapToApi(routes)).build();
    } else {
      return Response.status(Status.OK).entity(RouteMapper.mapToApiShort(routes)).build();
    }
  }

  /**
   * Return all alerts for an agency
   */
  @GET
  @Path("/agencies/{feedId}/{agencyId}/alerts")
  public Collection<ApiAlert> getAlertsForTrip(
    @PathParam("feedId") String feedId,
    @PathParam("agencyId") String agencyId
  ) {
    TransitService transitService = createTransitService();
    AlertMapper alertMapper = new AlertMapper(null); // TODO: Add locale
    FeedScopedId id = new FeedScopedId(feedId, agencyId);
    return alertMapper.mapToApi(transitService.getTransitAlertService().getAgencyAlerts(id));
  }

  /** Return specific transit stop in the graph, by ID. */
  @GET
  @Path("/stops/{stopId}")
  public ApiStop getStop(@PathParam("stopId") String stopIdString) {
    var stop = getStop(createTransitService(), stopIdString);
    return StopMapper.mapToApi(stop);
  }

  /** Return a list of all stops within a circle around the given coordinate. */
  @SuppressWarnings("ConstantConditions")
  @GET
  @Path("/stops")
  public List<ApiStopShort> getStopsInRadius(
    @QueryParam("minLat") Double minLat,
    @QueryParam("minLon") Double minLon,
    @QueryParam("maxLat") Double maxLat,
    @QueryParam("maxLon") Double maxLon,
    @QueryParam("lat") Double lat,
    @QueryParam("lon") Double lon,
    @QueryParam("radius") Double radius
  ) {
    /* When no parameters are supplied, return all stops. */
    if (uriInfo.getQueryParameters().isEmpty()) {
      return StopMapper.mapToApiShort(createTransitService().getAllStops());
    }

    /* If any of the circle parameters are specified, expect a circle not a box. */
    boolean expectCircle = (lat != null || lon != null || radius != null);
    if (expectCircle) {
      verifyParams()
        .withinBounds("lat", lat, -90.0, 90.0)
        .withinBounds("lon", lon, -180, 180)
        .positiveOrZero("radius", radius)
        .validate();

      radius = Math.min(radius, MAX_STOP_SEARCH_RADIUS);

      return createRoutingService()
        .getStopsInRadius(new WgsCoordinate(lat, lon), radius)
        .stream()
        .map(it -> StopMapper.mapToApiShort(it.first, it.second.intValue()))
        .collect(Collectors.toList());
    } else {
      /* We're not circle mode, we must be in box mode. */
      verifyParams()
        .withinBounds("minLat", minLat, -90.0, 90.0)
        .withinBounds("maxLat", maxLat, -90.0, 90.0)
        .withinBounds("minLon", minLon, -180.0, 180.0)
        .withinBounds("maxLon", maxLon, -180.0, 180.0)
        .lessThan("minLat", minLat, "maxLat", maxLat)
        .lessThan("minLon", minLon, "maxLon", maxLon)
        .validate();
      var stops = createRoutingService().getStopsByBoundingBox(minLat, minLon, maxLat, maxLon);
      return StopMapper.mapToApiShort(stops);
    }
  }

  @GET
  @Path("/stops/{stopId}/routes")
  public List<ApiRouteShort> getRoutesForStop(@PathParam("stopId") String stopId) {
    TransitService transitService = createTransitService();
    var stop = getStop(transitService, stopId);
    return transitService
      .getPatternsForStop(stop)
      .stream()
      .map(TripPattern::getRoute)
      .map(RouteMapper::mapToApiShort)
      .collect(Collectors.toList());
  }

  @GET
  @Path("/stops/{stopId}/patterns")
  public List<ApiPatternShort> getPatternsForStop(@PathParam("stopId") String stopId) {
    TransitService transitService = createTransitService();
    var stop = getStop(transitService, stopId);
    return transitService
      .getPatternsForStop(stop)
      .stream()
      .map(TripPatternMapper::mapToApiShort)
      .collect(Collectors.toList());
  }

  /**
   * Return upcoming vehicle arrival/departure times at the given stop.
   *
   * @param stopIdString       Stop ID in Agency:Stop ID format
   * @param startTime          Start time for the search. Seconds from UNIX epoch
   * @param timeRange          Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   */
  @GET
  @Path("/stops/{stopId}/stoptimes")
  public Collection<ApiStopTimesInPattern> getStopTimesForStop(
    @PathParam("stopId") String stopIdString,
    @QueryParam("startTime") long startTime,
    @QueryParam("timeRange") @DefaultValue("86400") int timeRange,
    @QueryParam("numberOfDepartures") @DefaultValue("2") int numberOfDepartures,
    @QueryParam("omitNonPickups") boolean omitNonPickups
  ) {
    TransitService transitService = createTransitService();
    var stop = getStop(transitService, stopIdString);

    return transitService
      .stopTimesForStop(
        stop,
        startTime,
        timeRange,
        numberOfDepartures,
        omitNonPickups ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
        false
      )
      .stream()
      .map(StopTimesInPatternMapper::mapToApi)
      .collect(Collectors.toList());
  }

  /**
   * Return upcoming vehicle arrival/departure times at the given stop.
   *
   * @param date in YYYYMMDD or YYYY-MM-DD format
   */
  @GET
  @Path("/stops/{stopId}/stoptimes/{date}")
  public List<ApiStopTimesInPattern> getStoptimesForStopAndDate(
    @PathParam("stopId") String stopId,
    @PathParam("date") String date,
    @QueryParam("omitNonPickups") boolean omitNonPickups
  ) {
    TransitService transitService = createTransitService();
    var stop = getStop(transitService, stopId);
    ServiceDate serviceDate = parseServiceDate("date", date);
    List<StopTimesInPattern> stopTimes = transitService.getStopTimesForStop(
      stop,
      serviceDate,
      omitNonPickups ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH
    );
    return StopTimesInPatternMapper.mapToApi(stopTimes);
  }

  /**
   * Return the generated transfers a stop in the graph, by stop ID
   */
  @GET
  @Path("/stops/{stopId}/transfers")
  public Collection<ApiTransfer> getTransfers(@PathParam("stopId") String stopId) {
    TransitService transitService = createTransitService();
    var stop = getStop(transitService, stopId);

    // get the transfers for the stop
    return transitService
      .getTransfersByStop(stop)
      .stream()
      .map(TransferMapper::mapToApi)
      .collect(Collectors.toList());
  }

  /**
   * Return all alerts for a stop
   */
  @GET
  @Path("/stops/{stopId}/alerts")
  public Collection<ApiAlert> getAlertsForStop(@PathParam("stopId") String stopId) {
    TransitService transitService = createTransitService();
    AlertMapper alertMapper = new AlertMapper(null); // TODO: Add locale
    FeedScopedId id = createId("stopId", stopId);
    return alertMapper.mapToApi(transitService.getTransitAlertService().getStopAlerts(id));
  }

  /** Return a list of all routes in the graph. */
  // with repeated hasStop parameters, replaces old routesBetweenStops
  @GET
  @Path("/routes")
  public List<ApiRouteShort> getRoutes(@QueryParam("hasStop") List<String> stopIds) {
    TransitService transitService = createTransitService();
    Collection<Route> routes = transitService.getAllRoutes();
    // Filter routes to include only those that pass through all given stops
    if (stopIds != null) {
      // Protective copy, we are going to calculate the intersection destructively
      routes = new ArrayList<>(routes);
      for (String stopId : stopIds) {
        var stop = getStop(transitService, stopId);
        Set<Route> routesHere = new HashSet<>();
        for (TripPattern pattern : transitService.getPatternsForStop(stop)) {
          routesHere.add(pattern.getRoute());
        }
        routes.retainAll(routesHere);
      }
    }
    return RouteMapper.mapToApiShort(routes);
  }

  /** Return specific route in the graph, for the given ID. */
  @GET
  @Path("/routes/{routeId}")
  public ApiRoute getRoute(@PathParam("routeId") String routeId) {
    Route route = getRoute(createTransitService(), routeId);
    return RouteMapper.mapToApi(route);
  }

  /** Return all stop patterns used by trips on the given route. */
  @GET
  @Path("/routes/{routeId}/patterns")
  public List<ApiPatternShort> getPatternsForRoute(@PathParam("routeId") String routeId) {
    TransitService transitService = createTransitService();
    Collection<TripPattern> patterns = transitService
      .getPatternsForRoute()
      .get(getRoute(transitService, routeId));
    return TripPatternMapper.mapToApiShort(patterns);
  }

  /** Return all stops in any pattern on a given route. */
  @GET
  @Path("/routes/{routeId}/stops")
  public List<ApiStopShort> getStopsForRoute(@PathParam("routeId") String routeId) {
    TransitService transitService = createTransitService();
    Route route = getRoute(transitService, routeId);

    Set<StopLocation> stops = new HashSet<>();
    Collection<TripPattern> patterns = transitService.getPatternsForRoute().get(route);
    for (TripPattern pattern : patterns) {
      stops.addAll(pattern.getStops());
    }
    return StopMapper.mapToApiShort(stops);
  }

  /** Return all trips in any pattern on the given route. */
  @GET
  @Path("/routes/{routeId}/trips")
  public List<ApiTripShort> getTripsForRoute(@PathParam("routeId") String routeId) {
    TransitService transitService = createTransitService();
    Route route = getRoute(transitService, routeId);

    var patterns = transitService.getPatternsForRoute().get(route);
    return patterns
      .stream()
      .flatMap(TripPattern::scheduledTripsAsStream)
      .map(TripMapper::mapToApiShort)
      .collect(Collectors.toList());
  }

  /**
   * Return all alerts for a route
   */
  @GET
  @Path("/routes/{routeId}/alerts")
  public Collection<ApiAlert> getAlertsForRoute(@PathParam("routeId") String routeId) {
    TransitService transitService = createTransitService();
    AlertMapper alertMapper = new AlertMapper(null); // TODO: Add locale
    FeedScopedId id = createId("routeId", routeId);
    return alertMapper.mapToApi(transitService.getTransitAlertService().getRouteAlerts(id));
  }

  // Not implemented, results would be too voluminous.
  // @Path("/trips")

  @GET
  @Path("/trips/{tripId}")
  public ApiTrip getTrip(@PathParam("tripId") String tripId) {
    Trip trip = getTrip(createTransitService(), tripId);
    return TripMapper.mapToApi(trip);
  }

  @GET
  @Path("/trips/{tripId}/stops")
  public List<ApiStopShort> getStopsForTrip(@PathParam("tripId") String tripId) {
    Collection<StopLocation> stops = getTripPatternForTripId(createTransitService(), tripId)
      .getStops();
    return StopMapper.mapToApiShort(stops);
  }

  @GET
  @Path("/trips/{tripId}/semanticHash")
  public String getSemanticHashForTrip(@PathParam("tripId") String tripId) {
    TransitService transitService = createTransitService();
    Trip trip = getTrip(transitService, tripId);
    TripPattern pattern = getTripPattern(transitService, trip);
    return pattern.semanticHashString(trip);
  }

  @GET
  @Path("/trips/{tripId}/stoptimes")
  public List<ApiTripTimeShort> getStoptimesForTrip(@PathParam("tripId") String tripId) {
    TransitService transitService = createTransitService();
    Trip trip = getTrip(transitService, tripId);
    TripPattern pattern = getTripPattern(transitService, trip);
    // Note, we need the updated timetable not the scheduled one (which contains no real-time updates).
    Timetable table = transitService.getTimetableForTripPattern(pattern, null);
    var tripTimesOnDate = TripTimeOnDate.fromTripTimes(table, trip);
    return TripTimeMapper.mapToApi(tripTimesOnDate);
  }

  /** Return geometry for the trip as a packed coordinate sequence */
  @GET
  @Path("/trips/{tripId}/geometry")
  public EncodedPolyline getGeometryForTrip(@PathParam("tripId") String tripId) {
    TripPattern pattern = getTripPatternForTripId(createTransitService(), tripId);
    return PolylineEncoder.encodeGeometry(pattern.getGeometry());
  }

  /**
   * Return all alerts for a trip
   */
  @GET
  @Path("/trips/{tripId}/alerts")
  public Collection<ApiAlert> getAlertsForTrip(@PathParam("tripId") String tripId) {
    TransitService transitService = createTransitService();
    AlertMapper alertMapper = new AlertMapper(null); // TODO: Add locale
    FeedScopedId id = createId("tripId", tripId);
    return alertMapper.mapToApi(transitService.getTransitAlertService().getTripAlerts(id, null));
  }

  @GET
  @Path("/patterns")
  public List<ApiPatternShort> getPatterns() {
    Collection<TripPattern> patterns = createTransitService().getTripPatterns();
    return TripPatternMapper.mapToApiShort(patterns);
  }

  @GET
  @Path("/patterns/{patternId}")
  public ApiPatternShort getPattern(@PathParam("patternId") String patternId) {
    TripPattern pattern = getTripPattern(createTransitService(), patternId);
    return TripPatternMapper.mapToApiDetailed(pattern);
  }

  @GET
  @Path("/patterns/{patternId}/trips")
  public List<ApiTripShort> getTripsForPattern(@PathParam("patternId") String patternId) {
    var trips = getTripPattern(createTransitService(), patternId).scheduledTripsAsStream();
    return TripMapper.mapToApiShort(trips);
  }

  @GET
  @Path("/patterns/{patternId}/stops")
  public List<ApiStopShort> getStopsForPattern(@PathParam("patternId") String patternId) {
    var stops = getTripPattern(createTransitService(), patternId).getStops();
    return StopMapper.mapToApiShort(stops);
  }

  @GET
  @Path("/patterns/{patternId}/semanticHash")
  public String getSemanticHashForPattern(@PathParam("patternId") String patternId) {
    TripPattern tripPattern = getTripPattern(createTransitService(), patternId);
    return tripPattern.semanticHashString(null);
  }

  /** Return geometry for the pattern as a packed coordinate sequence */
  @GET
  @Path("/patterns/{patternId}/geometry")
  public EncodedPolyline getGeometryForPattern(@PathParam("patternId") String patternId) {
    LineString line = getTripPattern(createTransitService(), patternId).getGeometry();
    return PolylineEncoder.encodeGeometry(line);
  }

  /**
   * Return all alerts for a pattern
   */
  @GET
  @Path("/patterns/{patternId}/alerts")
  public Collection<ApiAlert> getAlertsForPattern(@PathParam("patternId") String patternId) {
    TransitService transitService = createTransitService();
    AlertMapper alertMapper = new AlertMapper(null); // TODO: Add locale
    TripPattern pattern = getTripPattern(createTransitService(), patternId);
    return alertMapper.mapToApi(
      transitService
        .getTransitAlertService()
        .getDirectionAndRouteAlerts(pattern.getDirection().gtfsCode, pattern.getRoute().getId())
    );
  }

  // TODO include pattern ID for each trip in responses

  /**
   * List basic information about all service IDs. This is a placeholder endpoint and is not
   * implemented yet.
   */
  @GET
  @Path("/services")
  public Response getServices() {
    // TODO complete: index.serviceForId.values();
    return Response.status(Status.OK).entity("NONE").build();
  }

  /**
   * List details about a specific service ID including which dates it runs on. Replaces the old
   * /calendar. This is a placeholder endpoint and is not implemented yet.
   */
  @GET
  @Path("/services/{serviceId}")
  public Response getServices(@PathParam("serviceId") String serviceId) {
    // TODO complete: index.serviceForId.get(serviceId);
    return Response.status(Status.OK).entity("NONE").build();
  }

  /* PRIVATE METHODS */

  private static FeedScopedId createId(String name, String value) {
    return FeedScopedIdMapper.mapToDomain(name, value);
  }

  @SuppressWarnings("SameParameterValue")
  private static ServiceDate parseServiceDate(String label, String date) {
    try {
      return ServiceDate.parseString(date);
    } catch (ParseException e) {
      throw new BadRequestException(
        "Unable to parse date, not on format: YYYY-MM-DD. " + label + ": '" + date + "'"
      );
    }
  }

  private static ValidateParameters verifyParams() {
    return new ValidateParameters();
  }

  private static <T> T validateExist(String eName, T entity, String keyLabel, Object key) {
    if (entity != null) {
      return entity;
    } else {
      throw notFoundException(eName, keyLabel, key);
    }
  }

  private static NotFoundException notFoundException(String eName, String keyLbl, Object key) {
    return notFoundException(eName, keyLbl + ": " + key);
  }

  private static NotFoundException notFoundException(String entity, String details) {
    return new NotFoundException(entity + " not found. " + details);
  }

  private static Agency getAgency(TransitService transitService, String feedId, String agencyId) {
    Agency agency = transitService.getAgencyForId(new FeedScopedId(feedId, agencyId));
    if (agency == null) {
      throw notFoundException("Agency", "feedId: " + feedId + ", agencyId: " + agencyId);
    }
    return agency;
  }

  private static StopLocation getStop(TransitService transitService, String stopId) {
    var stop = transitService.getStopForId(createId("stopId", stopId));
    return validateExist("Stop", stop, "stopId", stop);
  }

  private static Route getRoute(TransitService transitService, String routeId) {
    Route route = transitService.getRouteForId(createId("routeId", routeId));
    return validateExist("Route", route, "routeId", routeId);
  }

  private static Trip getTrip(TransitService transitService, String tripId) {
    Trip trip = transitService.getTripForId().get(createId("tripId", tripId));
    return validateExist("Trip", trip, "tripId", tripId);
  }

  private static TripPattern getTripPattern(TransitService transitService, String tripPatternId) {
    FeedScopedId id = createId("patternId", tripPatternId);
    TripPattern pattern = transitService.getTripPatternForId(id);
    return validateExist("TripPattern", pattern, "patternId", tripPatternId);
  }

  private static TripPattern getTripPatternForTripId(TransitService transitService, String tripId) {
    return getTripPattern(transitService, getTrip(transitService, tripId));
  }

  private static TripPattern getTripPattern(TransitService transitService, Trip trip) {
    TripPattern pattern = transitService.getPatternForTrip().get(trip);
    return validateExist("TripPattern", pattern, "trip", trip.getId());
  }

  private RoutingService createRoutingService() {
    return otpServer.createRoutingRequestService();
  }

  private TransitService createTransitService() {
    return otpServer.createTransitRequestService();
  }
}
