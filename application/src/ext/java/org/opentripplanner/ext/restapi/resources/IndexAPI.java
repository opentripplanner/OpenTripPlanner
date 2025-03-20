package org.opentripplanner.ext.restapi.resources;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.apis.support.SemanticHash;
import org.opentripplanner.ext.restapi.mapping.AgencyMapper;
import org.opentripplanner.ext.restapi.mapping.AlertMapper;
import org.opentripplanner.ext.restapi.mapping.FeedInfoMapper;
import org.opentripplanner.ext.restapi.mapping.FeedScopedIdMapper;
import org.opentripplanner.ext.restapi.mapping.RouteMapper;
import org.opentripplanner.ext.restapi.mapping.StopMapper;
import org.opentripplanner.ext.restapi.mapping.StopTimesInPatternMapper;
import org.opentripplanner.ext.restapi.mapping.TransferMapper;
import org.opentripplanner.ext.restapi.mapping.TripMapper;
import org.opentripplanner.ext.restapi.mapping.TripPatternMapper;
import org.opentripplanner.ext.restapi.mapping.TripTimeMapper;
import org.opentripplanner.ext.restapi.model.ApiAgency;
import org.opentripplanner.ext.restapi.model.ApiAlert;
import org.opentripplanner.ext.restapi.model.ApiFeedInfo;
import org.opentripplanner.ext.restapi.model.ApiPatternShort;
import org.opentripplanner.ext.restapi.model.ApiRoute;
import org.opentripplanner.ext.restapi.model.ApiRouteShort;
import org.opentripplanner.ext.restapi.model.ApiStop;
import org.opentripplanner.ext.restapi.model.ApiStopShort;
import org.opentripplanner.ext.restapi.model.ApiStopTimesInPattern;
import org.opentripplanner.ext.restapi.model.ApiTransfer;
import org.opentripplanner.ext.restapi.model.ApiTrip;
import org.opentripplanner.ext.restapi.model.ApiTripShort;
import org.opentripplanner.ext.restapi.model.ApiTripTimeShort;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.DirectGraphFinder;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{ignoreRouterId}/index") // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class IndexAPI {

  private static final double MAX_STOP_SEARCH_RADIUS = 5000;

  private final OtpServerRequestContext serverContext;

  /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
  @Context
  UriInfo uriInfo;

  public IndexAPI(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
  }

  @GET
  @Path("/feeds")
  public Collection<String> getFeeds() {
    return transitService().listFeedIds();
  }

  @GET
  @Path("/feeds/{feedId}")
  public ApiFeedInfo getFeedInfo(@PathParam("feedId") String feedId) {
    var feedInfo = FeedInfoMapper.mapToApi(transitService().getFeedInfo(feedId));
    return validateExist("FeedInfo", feedInfo, "feedId", feedId);
  }

  /** Return a list of all agencies in the graph. */
  @GET
  @Path("/agencies/{feedId}")
  public Collection<ApiAgency> getAgencies(@PathParam("feedId") String feedId) {
    Collection<Agency> agencies = transitService()
      .listAgencies()
      .stream()
      .filter(agency -> agency.getId().getFeedId().equals(feedId))
      .collect(Collectors.toList());
    validateExist("Agency", agencies, "feedId", feedId);
    return AgencyMapper.mapToApi(agencies);
  }

  /** Return specific agency in the graph, by ID. */
  @GET
  @Path("/agencies/{feedId}/{agencyId}")
  public ApiAgency httpGgetAgency(
    @PathParam("feedId") String feedId,
    @PathParam("agencyId") String agencyId
  ) {
    return AgencyMapper.mapToApi(agency(feedId, agencyId));
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
    var agency = agency(feedId, agencyId);

    Collection<Route> routes = transitService()
      .listRoutes()
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
    var alertMapper = new AlertMapper(null); // TODO: Add locale
    var id = new FeedScopedId(feedId, agencyId);
    return alertMapper.mapToApi(transitService().getTransitAlertService().getAgencyAlerts(id));
  }

  /** Return specific transit stop in the graph, by ID. */
  @GET
  @Path("/stops/{stopId}")
  public ApiStop getStop(@PathParam("stopId") String stopIdString) {
    return StopMapper.mapToApi(stop(stopIdString));
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
      return StopMapper.mapToApiShort(transitService().listStopLocations());
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

      return new DirectGraphFinder(serverContext.transitService()::findRegularStopsByBoundingBox)
        .findClosestStops(new Coordinate(lon, lat), radius)
        .stream()
        .map(it -> StopMapper.mapToApiShort(it.stop, it.distance))
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

      Envelope envelope = new Envelope(
        new Coordinate(minLon, minLat),
        new Coordinate(maxLon, maxLat)
      );

      var stops = transitService().findRegularStopsByBoundingBox(envelope);
      return stops.stream().map(StopMapper::mapToApiShort).toList();
    }
  }

  @GET
  @Path("/stops/{stopId}/routes")
  public List<ApiRouteShort> getRoutesForStop(@PathParam("stopId") String stopId) {
    var stop = stop(stopId);
    return transitService()
      .findPatterns(stop)
      .stream()
      .map(TripPattern::getRoute)
      .map(RouteMapper::mapToApiShort)
      .collect(Collectors.toList());
  }

  @GET
  @Path("/stops/{stopId}/patterns")
  public List<ApiPatternShort> getPatternsForStop(@PathParam("stopId") String stopId) {
    var stop = stop(stopId);
    return transitService()
      .findPatterns(stop)
      .stream()
      .map(TripPatternMapper::mapToApiShort)
      .collect(Collectors.toList());
  }

  /**
   * Return upcoming vehicle arrival/departure times at the given stop.
   *
   * @param stopIdString       Stop ID in Agency:Stop ID format
   * @param startTimeSeconds          Start time for the search. Seconds from UNIX epoch
   * @param timeRange          Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   */
  @GET
  @Path("/stops/{stopId}/stoptimes")
  public Collection<ApiStopTimesInPattern> getStopTimesForStop(
    @PathParam("stopId") String stopIdString,
    @QueryParam("startTime") long startTimeSeconds,
    @QueryParam("timeRange") @DefaultValue("86400") int timeRange,
    @QueryParam("numberOfDepartures") @DefaultValue("2") int numberOfDepartures,
    @QueryParam("omitNonPickups") boolean omitNonPickups
  ) {
    Instant startTime = startTimeSeconds == 0
      ? Instant.now()
      : Instant.ofEpochSecond(startTimeSeconds);

    return transitService()
      .findStopTimesInPattern(
        stop(stopIdString),
        startTime,
        Duration.ofSeconds(timeRange),
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
    var stop = stop(stopId);
    var serviceDate = parseServiceDate("date", date);
    List<StopTimesInPattern> stopTimes = transitService()
      .findStopTimesInPattern(
        stop,
        serviceDate,
        omitNonPickups ? ArrivalDeparture.DEPARTURES : ArrivalDeparture.BOTH,
        true
      );
    return StopTimesInPatternMapper.mapToApi(stopTimes);
  }

  /**
   * Return the generated transfers a stop in the graph, by stop ID
   */
  @GET
  @Path("/stops/{stopId}/transfers")
  public Collection<ApiTransfer> getTransfers(@PathParam("stopId") String stopId) {
    var stop = stop(stopId);

    // get the transfers for the stop
    return transitService()
      .findPathTransfers(stop)
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
    var alertMapper = new AlertMapper(null); // TODO: Add locale
    var id = createId("stopId", stopId);
    return alertMapper.mapToApi(transitService().getTransitAlertService().getStopAlerts(id));
  }

  /** Return a list of all routes in the graph. */
  // with repeated hasStop parameters, replaces old routesBetweenStops
  @GET
  @Path("/routes")
  public List<ApiRouteShort> getRoutes(@QueryParam("hasStop") List<String> stopIds) {
    Collection<Route> routes = transitService().listRoutes();
    // Filter routes to include only those that pass through all given stops
    if (stopIds != null) {
      // Protective copy, we are going to calculate the intersection destructively
      routes = new ArrayList<>(routes);
      for (String stopId : stopIds) {
        var stop = stop(stopId);
        Set<Route> routesHere = new HashSet<>();
        for (TripPattern pattern : transitService().findPatterns(stop)) {
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
    return RouteMapper.mapToApi(route(routeId));
  }

  /** Return all stop patterns used by trips on the given route. */
  @GET
  @Path("/routes/{routeId}/patterns")
  public List<ApiPatternShort> getPatternsForRoute(@PathParam("routeId") String routeId) {
    Collection<TripPattern> patterns = transitService().findPatterns(route(routeId));
    return TripPatternMapper.mapToApiShort(patterns);
  }

  /** Return all stops in any pattern on a given route. */
  @GET
  @Path("/routes/{routeId}/stops")
  public List<ApiStopShort> getStopsForRoute(@PathParam("routeId") String routeId) {
    var route = route(routeId);

    Set<StopLocation> stops = new HashSet<>();
    Collection<TripPattern> patterns = transitService().findPatterns(route);
    for (TripPattern pattern : patterns) {
      stops.addAll(pattern.getStops());
    }
    return StopMapper.mapToApiShort(stops);
  }

  /** Return all trips in any pattern on the given route. */
  @GET
  @Path("/routes/{routeId}/trips")
  public List<ApiTripShort> getTripsForRoute(@PathParam("routeId") String routeId) {
    var route = route(routeId);

    var patterns = transitService().findPatterns(route);
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
    var alertMapper = new AlertMapper(null); // TODO: Add locale
    var id = createId("routeId", routeId);
    return alertMapper.mapToApi(transitService().getTransitAlertService().getRouteAlerts(id));
  }

  // Not implemented, results would be too voluminous.
  // @Path("/trips")

  @GET
  @Path("/trips/{tripId}")
  public ApiTrip getTrip(@PathParam("tripId") String tripId) {
    return TripMapper.mapToApi(trip(tripId));
  }

  @GET
  @Path("/trips/{tripId}/stops")
  public List<ApiStopShort> getStopsForTrip(@PathParam("tripId") String tripId) {
    Collection<StopLocation> stops = tripPatternForTripId(tripId).getStops();
    return StopMapper.mapToApiShort(stops);
  }

  @GET
  @Path("/trips/{tripId}/semanticHash")
  public String getSemanticHashForTrip(@PathParam("tripId") String tripId) {
    var trip = trip(tripId);
    return SemanticHash.forTripPattern(tripPattern(trip), trip);
  }

  @GET
  @Path("/trips/{tripId}/stoptimes")
  public List<ApiTripTimeShort> getStoptimesForTrip(@PathParam("tripId") String tripId) {
    var trip = trip(tripId);
    var pattern = tripPattern(trip);
    // Note, we need the updated timetable not the scheduled one (which contains no real-time updates).
    var table = transitService()
      .findTimetable(pattern, LocalDate.now(transitService().getTimeZone()));
    var tripTimesOnDate = TripTimeOnDate.fromTripTimes(table, trip);
    return TripTimeMapper.mapToApi(tripTimesOnDate);
  }

  /** Return geometry for the trip as a packed coordinate sequence */
  @GET
  @Path("/trips/{tripId}/geometry")
  public EncodedPolyline getGeometryForTrip(@PathParam("tripId") String tripId) {
    var pattern = tripPatternForTripId(tripId);
    return EncodedPolyline.encode(pattern.getGeometry());
  }

  /**
   * Return all alerts for a trip
   */
  @GET
  @Path("/trips/{tripId}/alerts")
  public Collection<ApiAlert> getAlertsForTrip(@PathParam("tripId") String tripId) {
    var alertMapper = new AlertMapper(null); // TODO: Add locale
    var id = createId("tripId", tripId);
    return alertMapper.mapToApi(transitService().getTransitAlertService().getTripAlerts(id));
  }

  @GET
  @Path("/patterns")
  public List<ApiPatternShort> getPatterns() {
    Collection<TripPattern> patterns = transitService().listTripPatterns();
    return TripPatternMapper.mapToApiShort(patterns);
  }

  @GET
  @Path("/patterns/{patternId}")
  public ApiPatternShort getPattern(@PathParam("patternId") String patternId) {
    var pattern = tripPattern(patternId);
    return TripPatternMapper.mapToApiDetailed(pattern);
  }

  @GET
  @Path("/patterns/{patternId}/trips")
  public List<ApiTripShort> getTripsForPattern(@PathParam("patternId") String patternId) {
    var trips = tripPattern(patternId).scheduledTripsAsStream();
    return TripMapper.mapToApiShort(trips);
  }

  @GET
  @Path("/patterns/{patternId}/stops")
  public List<ApiStopShort> getStopsForPattern(@PathParam("patternId") String patternId) {
    var stops = tripPattern(patternId).getStops();
    return StopMapper.mapToApiShort(stops);
  }

  @GET
  @Path("/patterns/{patternId}/semanticHash")
  public String getSemanticHashForPattern(@PathParam("patternId") String patternId) {
    var tripPattern = tripPattern(patternId);
    return SemanticHash.forTripPattern(tripPattern, null);
  }

  /** Return geometry for the pattern as a packed coordinate sequence */
  @GET
  @Path("/patterns/{patternId}/geometry")
  public EncodedPolyline getGeometryForPattern(@PathParam("patternId") String patternId) {
    var line = tripPattern(patternId).getGeometry();
    return EncodedPolyline.encode(line);
  }

  /**
   * Return all alerts for a pattern
   */
  @GET
  @Path("/patterns/{patternId}/alerts")
  public Collection<ApiAlert> getAlertsForPattern(@PathParam("patternId") String patternId) {
    var alertMapper = new AlertMapper(null); // TODO: Add locale
    var pattern = tripPattern(patternId);
    return alertMapper.mapToApi(
      transitService()
        .getTransitAlertService()
        .getDirectionAndRouteAlerts(pattern.getDirection(), pattern.getRoute().getId())
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
  private static LocalDate parseServiceDate(String label, String date) {
    try {
      return ServiceDateUtils.parseString(date);
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

  private Agency agency(String feedId, String agencyId) {
    var agency = transitService().getAgency(new FeedScopedId(feedId, agencyId));
    if (agency == null) {
      throw notFoundException("Agency", "feedId: " + feedId + ", agencyId: " + agencyId);
    }
    return agency;
  }

  private StopLocation stop(String stopId) {
    var stop = transitService().getRegularStop(createId("stopId", stopId));
    return validateExist("Stop", stop, "stopId", stop);
  }

  private Route route(String routeId) {
    var route = transitService().getRoute(createId("routeId", routeId));
    return validateExist("Route", route, "routeId", routeId);
  }

  private Trip trip(String tripId) {
    var trip = transitService().getTrip(createId("tripId", tripId));
    return validateExist("Trip", trip, "tripId", tripId);
  }

  private TripPattern tripPattern(String tripPatternId) {
    var id = createId("patternId", tripPatternId);
    var pattern = transitService().getTripPattern(id);
    return validateExist("TripPattern", pattern, "patternId", tripPatternId);
  }

  private TripPattern tripPatternForTripId(String tripId) {
    return tripPattern(trip(tripId));
  }

  private TripPattern tripPattern(Trip trip) {
    var pattern = transitService().findPattern(trip);
    return validateExist("TripPattern", pattern, "trip", trip.getId());
  }

  private TransitService transitService() {
    return serverContext.transitService();
  }
}
