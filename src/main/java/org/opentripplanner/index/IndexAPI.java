package org.opentripplanner.index;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Collections2;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.mapping.AgencyMapper;
import org.opentripplanner.api.mapping.FeedInfoMapper;
import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.api.mapping.RouteMapper;
import org.opentripplanner.api.mapping.StopMapper;
import org.opentripplanner.api.mapping.StopTimesInPatternMapper;
import org.opentripplanner.api.mapping.TripMapper;
import org.opentripplanner.api.mapping.TripPatternMapper;
import org.opentripplanner.api.model.ApiStop;
import org.opentripplanner.api.model.ApiStopTimesInPattern;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.index.model.ApiRouteShort;
import org.opentripplanner.index.model.ApiStopShort;
import org.opentripplanner.index.model.ApiTransfer;
import org.opentripplanner.index.model.ApiTripShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{routerId}/index")    // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class IndexAPI {

    private static final double MAX_STOP_SEARCH_RADIUS = 5000;
    private static final String MSG_404 = "FOUR ZERO FOUR";
    private static final String MSG_400 = "FOUR HUNDRED";

    /** Choose short or long form of results. */
    @QueryParam("detail") private boolean detail = false;

    /** Include GTFS entities referenced by ID in the result. */
    @QueryParam("refs") private boolean refs = false;

    private final Graph graph;
    private final StreetVertexIndex streetIndex;

    public IndexAPI (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        graph = router.graph;
        streetIndex = router.graph.streetIndex;
    }

    /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
    @Context UriInfo uriInfo;

    @GET
    @Path("/feeds")
    public Response getFeeds() {
        return Response.status(Status.OK).entity(getRoutingService().getAgenciesForFeedId().keySet()).build();
    }

    @GET
    @Path("/feeds/{feedId}")
    public Response getFeedInfo(@PathParam("feedId") String feedId) {
        FeedInfo feedInfo = getRoutingService().getFeedInfoForId().get(feedId);
        if (feedInfo == null) {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        } else {
            return Response.status(Status.OK).entity(FeedInfoMapper.mapToApi(feedInfo)).build();
        }
    }

   /** Return a list of all agencies in the graph. */
   @GET
   @Path("/agencies/{feedId}")
   public Response getAgencies (@PathParam("feedId") String feedId) {
       return Response.status(Status.OK).entity(
               getRoutingService().getAgenciesForFeedId().getOrDefault(feedId, new HashMap<>()).values()).build();
   }

   /** Return specific agency in the graph, by ID. */
   @GET
   @Path("/agencies/{feedId}/{agencyId}")
   public Response getAgency (@PathParam("feedId") String feedId, @PathParam("agencyId") String agencyId) {
       for (Agency agency : getRoutingService().getAgenciesForFeedId().get(feedId).values()) {
           if (agency.getId().equals(agencyId)) {
               return Response.status(Status.OK).entity(AgencyMapper.mapToApi(agency)).build();
           }
       }
       return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
   }

    /** Return all routes for the specific agency. */
    @GET
    @Path("/agencies/{feedId}/{agencyId}/routes")
    public Response getAgencyRoutes (@PathParam("feedId") String feedId, @PathParam("agencyId") String agencyId) {
        RoutingService routingService = getRoutingService();
        Collection<Route> routes = routingService.getRouteForId().values();
        Agency agency = routingService.getAgenciesForFeedId().get(feedId).get(agencyId);
        if (agency == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        Collection<Route> agencyRoutes = new ArrayList<>();
        for (Route route: routes) {
            if (route.getAgency() == agency) {
                agencyRoutes.add(route);
            }
        }
        routes = agencyRoutes;
        if (detail){
            return Response.status(Status.OK).entity(RouteMapper.mapToApi(routes)).build();
        }
        else {
            return Response.status(Status.OK).entity(ApiRouteShort.list(routes)).build();
        }
    }
   
   /** Return specific transit stop in the graph, by ID. */
   @GET
   @Path("/stops/{stopId}")
   public Response getStop (@PathParam("stopId") String stopIdString) {
       FeedScopedId stopId = createId("stopId", stopIdString);
       Stop stop = getRoutingService().getStopForId().get(stopId);
       if (stop != null) {
           ApiStop apiStop = StopMapper.mapToApi(stop);
           return Response.status(Status.OK).entity(apiStop).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }
   
   /** Return a list of all stops within a circle around the given coordinate. */
   @GET
   @Path("/stops")
   public Response getStopsInRadius (
           @QueryParam("minLat") Double minLat,
           @QueryParam("minLon") Double minLon,
           @QueryParam("maxLat") Double maxLat,
           @QueryParam("maxLon") Double maxLon,
           @QueryParam("lat")    Double lat,
           @QueryParam("lon")    Double lon,
           @QueryParam("radius") Double radius) {

       /* When no parameters are supplied, return all stops. */
       if (uriInfo.getQueryParameters().isEmpty()) {
           Collection<Stop> stops = getRoutingService().getStopForId().values();
           return Response.status(Status.OK).entity(ApiStopShort.list(stops)).build();
       }
       /* If any of the circle parameters are specified, expect a circle not a box. */
       boolean expectCircle = (lat != null || lon != null || radius != null);
       if (expectCircle) {
           if (lat == null || lon == null || radius == null || radius < 0) {
               return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
           }
           if (radius > MAX_STOP_SEARCH_RADIUS){
               radius = MAX_STOP_SEARCH_RADIUS;
           }
           List<ApiStopShort> stops = Lists.newArrayList();
           Coordinate coord = new Coordinate(lon, lat);
           for (TransitStopVertex stopVertex : streetIndex.getNearbyTransitStops(
                    new Coordinate(lon, lat), radius)) {
               double distance = SphericalDistanceLibrary.fastDistance(stopVertex.getCoordinate(), coord);
               if (distance < radius) {
                   stops.add(new ApiStopShort(stopVertex.getStop(), (int) distance));
               }
           }
           return Response.status(Status.OK).entity(stops).build();
       } else {
           /* We're not circle mode, we must be in box mode. */
           if (minLat == null || minLon == null || maxLat == null || maxLon == null) {
               return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
           }
           if (maxLat <= minLat || maxLon <= minLon) {
               return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
           }
           List<ApiStopShort> stops = Lists.newArrayList();
           Envelope envelope = new Envelope(new Coordinate(minLon, minLat), new Coordinate(maxLon, maxLat));
           for (TransitStopVertex stopVertex : streetIndex.getTransitStopForEnvelope(envelope)) {
               stops.add(new ApiStopShort(stopVertex.getStop()));
           }
           return Response.status(Status.OK).entity(stops).build();           
       }
   }

   @GET
   @Path("/stops/{stopId}/routes")
   public Response getRoutesForStop (@PathParam("stopId") String stopId) {
       RoutingService routingService = getRoutingService();
       Stop stop = routingService.getStopForId().get(createId("stopId", stopId));
       if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       Set<Route> routes = Sets.newHashSet();
       for (TripPattern pattern : routingService.getPatternsForStop().get(stop)) {
           routes.add(pattern.route);
       }
       return Response.status(Status.OK).entity(ApiRouteShort.list(routes)).build();
   }

   @GET
   @Path("/stops/{stopId}/patterns")
   public Response getPatternsForStop (@PathParam("stopId") String stopIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId id = createId("stopId", stopIdString);
       Stop stop = routingService.getStopForId().get(id);
       if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       Collection<TripPattern> patterns = routingService.getPatternsForStop().get(stop);
       return Response.status(Status.OK).entity(TripPatternMapper.mapToApiShort(patterns)).build();
   }

    /** Return upcoming vehicle arrival/departure times at the given stop.
     *
     * @param stopIdString Stop ID in Agency:Stop ID format
     * @param startTime Start time for the search. Seconds from UNIX epoch
     * @param timeRange Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     */
    @GET
    @Path("/stops/{stopId}/stoptimes")
    public Response getStoptimesForStop (@PathParam("stopId") String stopIdString,
                                         @QueryParam("startTime") long startTime,
                                         @QueryParam("timeRange") @DefaultValue("86400") int timeRange,
                                         @QueryParam("numberOfDepartures") @DefaultValue("2") int numberOfDepartures,
                                         @QueryParam("omitNonPickups") boolean omitNonPickups) {
        RoutingService routingService = getRoutingService();
        Stop stop = routingService.getStopForId().get(createId("stopId", stopIdString));
        if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();

        List<ApiStopTimesInPattern> stopTimesInPatterns =
            routingService
                .stopTimesForStop(stop, startTime, timeRange, numberOfDepartures, omitNonPickups )
                .stream().map(StopTimesInPatternMapper::mapToApi)
                .collect(Collectors.toList());

        return Response.status(Status.OK).entity(stopTimesInPatterns).build();
    }

    /**
     * Return upcoming vehicle arrival/departure times at the given stop.
     * @param date in YYYYMMDD format
     */
    @GET
    @Path("/stops/{stopId}/stoptimes/{date}")
    public Response getStoptimesForStopAndDate (@PathParam("stopId") String stopIdString,
                                                @PathParam("date") String date,
                                                @QueryParam("omitNonPickups") boolean omitNonPickups) {
        RoutingService routingService = getRoutingService();
        Stop stop = routingService.getStopForId().get(createId("stopId", stopIdString));
        if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        ServiceDate sd;
        try {
            sd = ServiceDate.parseString(date);
        }
        catch (ParseException e){
            return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
        }

        List<StopTimesInPattern> ret = routingService.getStopTimesForStop(stop, sd, omitNonPickups);
        return Response.status(Status.OK).entity(ret).build();
    }
    
    /**
     * Return the generated transfers a stop in the graph, by stop ID
     */
    @GET
    @Path("/stops/{stopId}/transfers")
    public Response getTransfers(@PathParam("stopId") String stopIdString) {
        RoutingService routingService = getRoutingService();
        Stop stop = routingService.getStopForId().get(createId("stopId", stopIdString));
        
        if (stop != null) {
            // get the transfers for the stop
            Collection<SimpleTransfer> transfers = routingService.getTransfersByStop().get(stop);
            
            Collection<ApiTransfer> out = Collections2.transform(transfers, ApiTransfer::new);
            
            return Response.status(Status.OK).entity(out).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }
    
   /** Return a list of all routes in the graph. */
   // with repeated hasStop parameters, replaces old routesBetweenStops
   @GET
   @Path("/routes")
   public Response getRoutes (@QueryParam("hasStop") List<String> stopIds) {
       RoutingService routingService = getRoutingService();
       Collection<Route> routes = routingService.getRouteForId().values();
       // Filter routes to include only those that pass through all given stops
       if (stopIds != null) {
           // Protective copy, we are going to calculate the intersection destructively
           routes = Lists.newArrayList(routes);
           for (String stopId : stopIds) {
               Stop stop = routingService.getStopForId().get(createId("stopId", stopId));
               if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
               Set<Route> routesHere = Sets.newHashSet();
               for (TripPattern pattern : routingService.getPatternsForStop().get(stop)) {
                   routesHere.add(pattern.route);
               }
               routes.retainAll(routesHere);
           }
       }
       return Response.status(Status.OK).entity(ApiRouteShort.list(routes)).build();
   }

   /** Return specific route in the graph, for the given ID. */
   @GET
   @Path("/routes/{routeId}")
   public Response getRoute (@PathParam("routeId") String routeIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId routeId = createId("routeId", routeIdString);
       Route route = routingService.getRouteForId().get(routeId);
       if (route != null) {
           return Response.status(Status.OK).entity(RouteMapper.mapToApi(route)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all stop patterns used by trips on the given route. */
   @GET
   @Path("/routes/{routeId}/patterns")
   public Response getPatternsForRoute (@PathParam("routeId") String routeIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId routeId = createId("routeId", routeIdString);
       Route route = routingService.getRouteForId().get(routeId);
       if (route != null) {
           Collection<TripPattern> patterns = routingService.getPatternsForRoute().get(route);
           return Response.status(Status.OK).entity(TripPatternMapper.mapToApiShort(patterns)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all stops in any pattern on a given route. */
   @GET
   @Path("/routes/{routeId}/stops")
   public Response getStopsForRoute (@PathParam("routeId") String routeIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId routeId = createId("routeId", routeIdString);
       Route route = routingService.getRouteForId().get(routeId);
       if (route != null) {
           Set<Stop> stops = Sets.newHashSet();
           Collection<TripPattern> patterns = routingService.getPatternsForRoute().get(route);
           for (TripPattern pattern : patterns) {
               stops.addAll(pattern.getStops());
           }
           return Response.status(Status.OK).entity(ApiStopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all trips in any pattern on the given route. */
   @GET
   @Path("/routes/{routeId}/trips")
   public Response getTripsForRoute (@PathParam("routeId") String routeIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId routeId = createId("routeId", routeIdString);
       Route route = routingService.getRouteForId().get(routeId);
       if (route != null) {
           List<Trip> trips = Lists.newArrayList();
           Collection<TripPattern> patterns = routingService.getPatternsForRoute().get(route);
           for (TripPattern pattern : patterns) {
               trips.addAll(pattern.getTrips());
           }
           return Response.status(Status.OK).entity(ApiTripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }
   
   
    // Not implemented, results would be too voluminous.
    // @Path("/trips")

   @GET
   @Path("/trips/{tripId}")
   public Response getTrip (@PathParam("tripId") String tripIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId tripId = createId("tripId", tripIdString);
       Trip trip = routingService.getTripForId().get(tripId);
       if (trip != null) {
           return Response.status(Status.OK).entity(TripMapper.mapToApi(trip)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/trips/{tripId}/stops")
   public Response getStopsForTrip (@PathParam("tripId") String tripIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId tripId = createId("tripId", tripIdString);
       Trip trip = routingService.getTripForId().get(tripId);
       if (trip != null) {
           TripPattern pattern = routingService.getPatternForTrip().get(trip);
           Collection<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(ApiStopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

    @GET
    @Path("/trips/{tripId}/semanticHash")
    public Response getSemanticHashForTrip (@PathParam("tripId") String tripIdString) {
        RoutingService routingService = getRoutingService();
        FeedScopedId tripId = createId("tripId", tripIdString);
        Trip trip = routingService.getTripForId().get(tripId);
        if (trip != null) {
            TripPattern pattern = routingService.getPatternForTrip().get(trip);
            String hashString = pattern.semanticHashString(trip);
            return Response.status(Status.OK).entity(hashString).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    @GET
   @Path("/trips/{tripId}/stoptimes")
   public Response getStoptimesForTrip (@PathParam("tripId") String tripIdString) {
       RoutingService routingService = getRoutingService();
       FeedScopedId tripId = createId("tripId", tripIdString);
       Trip trip = routingService.getTripForId().get(tripId);
       if (trip != null) {
           TripPattern pattern = routingService.getPatternForTrip().get(trip);
           // Note, we need the updated timetable not the scheduled one (which contains no real-time updates).
           Timetable table = routingService.getTimetableForTripPattern(pattern);
           return Response.status(Status.OK).entity(TripTimeShort.fromTripTimes(table, trip)).build();
       } else {
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

    /** Return geometry for the trip as a packed coordinate sequence */
    @GET
    @Path("/trips/{tripId}/geometry")
    public Response getGeometryForTrip (@PathParam("tripId") String tripIdString) {
        RoutingService routingService = getRoutingService();
        FeedScopedId tripId = createId("tripId", tripIdString);
        Trip trip = routingService.getTripForId().get(tripId);
        if (trip != null) {
            TripPattern tripPattern = routingService.getPatternForTrip().get(trip);
            EncodedPolylineBean geometry = PolylineEncoder.createEncodings(tripPattern.getGeometry());
            return Response.status(Status.OK).entity(geometry).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

   @GET
   @Path("/patterns")
   public Response getPatterns () {
       RoutingService routingService = getRoutingService();
       Collection<TripPattern> patterns = routingService.getTripPatterns();
       return Response.status(Status.OK).entity(TripPatternMapper.mapToApiShort(patterns)).build();
   }

   @GET
   @Path("/patterns/{patternId}")
   public Response getPattern (@PathParam("patternId") String patternIdString) {
        FeedScopedId patternId = FeedScopedIdMapper.mapToDomain("patternId", patternIdString);
       RoutingService routingService = getRoutingService();
       TripPattern pattern = routingService.getTripPatternForId(patternId);
       if (pattern != null) {
           return Response.status(Status.OK).entity(TripPatternMapper.mapToApiShort(pattern)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns/{patternId}/trips")
   public Response getTripsForPattern (@PathParam("patternId") String patternIdString) {
       FeedScopedId patternId = FeedScopedIdMapper.mapToDomain("patternId", patternIdString);
       RoutingService routingService = getRoutingService();
       TripPattern pattern = routingService.getTripPatternForId(patternId);
       if (pattern != null) {
           List<Trip> trips = pattern.getTrips();
           return Response.status(Status.OK).entity(ApiTripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns/{patternId}/stops")
   public Response getStopsForPattern (@PathParam("patternId") String patternIdString) {
       FeedScopedId patternId = FeedScopedIdMapper.mapToDomain("patternId", patternIdString);
       RoutingService routingService = getRoutingService();
       // Pattern names are graph-unique because we made them that way (did not read them from GTFS).
       TripPattern pattern = routingService.getTripPatternForId(patternId);
       if (pattern != null) {
           List<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(ApiStopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

    @GET
    @Path("/patterns/{patternId}/semanticHash")
    public Response getSemanticHashForPattern (@PathParam("patternId") String patternIdString) {
        FeedScopedId patternId = FeedScopedIdMapper.mapToDomain("patternId", patternIdString);
        RoutingService routingService = getRoutingService();
        // Pattern names are graph-unique because we made them that way (did not read them from GTFS).
        TripPattern pattern = routingService.getTripPatternForId(patternId);
        if (pattern != null) {
            String semanticHash = pattern.semanticHashString(null);
            return Response.status(Status.OK).entity(semanticHash).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    /** Return geometry for the pattern as a packed coordinate sequence */
    @GET
    @Path("/patterns/{patternId}/geometry")
    public Response getGeometryForPattern (@PathParam("patternId") String patternIdString) {
        FeedScopedId patternId = FeedScopedIdMapper.mapToDomain("patternId", patternIdString);
        RoutingService routingService = getRoutingService();
        TripPattern pattern = routingService.getTripPatternForId(patternId);
        if (pattern != null) {
            EncodedPolylineBean geometry = PolylineEncoder.createEncodings(pattern.getGeometry());
            return Response.status(Status.OK).entity(geometry).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }    

    // TODO include pattern ID for each trip in responses

    /**
     * List basic information about all service IDs.
     * This is a placeholder endpoint and is not implemented yet.
     */
    @GET
    @Path("/services")
    public Response getServices() {
        // TODO complete: index.serviceForId.values();
        return Response.status(Status.OK).entity("NONE").build();
    }

    /**
     * List details about a specific service ID including which dates it runs on. Replaces the old /calendar.
     * This is a placeholder endpoint and is not implemented yet.
     */
    @GET
    @Path("/services/{serviceId}")
    public Response getServices(@PathParam("serviceId") String serviceId) {
        // TODO complete: index.serviceForId.get(serviceId);
        return Response.status(Status.OK).entity("NONE").build();
    }

    /**
     * Create a new service for each request - as required by the service.
     */
    private RoutingService getRoutingService() {
        return new RoutingService(graph);
    }

    private static FeedScopedId createId(String name, String value) {
        return FeedScopedIdMapper.mapToDomain(name, value);
    }
}
