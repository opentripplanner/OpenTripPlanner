/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.index;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.PatternDetail;
import org.opentripplanner.index.model.PatternShort;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.StopClusterDetail;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.index.model.TripShort;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@Path("/routers/{routerId}/index")    // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class IndexAPI {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(IndexAPI.class);
    private static final double MAX_STOP_SEARCH_RADIUS = 5000;
    private static final DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();
    private static final String MSG_404 = "FOUR ZERO FOUR";
    private static final String MSG_400 = "FOUR HUNDRED";

    /** Choose short or long form of results. */
    @QueryParam("detail") private boolean detail = false;

    /** Include GTFS entities referenced by ID in the result. */
    @QueryParam("refs") private boolean refs = false;

    private final GraphIndex index;
    private final StreetVertexIndexService streetIndex;

    public IndexAPI (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        index = router.graph.index;
        streetIndex = router.graph.streetIndex;
    }

   /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
   @Context UriInfo uriInfo;

   /** Return a list of all agencies in the graph. */
   @GET
   @Path("/agencies")
   public Response getAgencies () {
       return Response.status(Status.OK).entity(index.agencyForId.values()).build();
   }

   /** Return specific agency in the graph, by ID. */
   @GET
   @Path("/agencies/{agencyId}")
   public Response getAgency (@PathParam("agencyId") String agencyId) {
       for (Agency agency : index.agencyForId.values()) {
           if (agency.getId().equals(agencyId)) {
               return Response.status(Status.OK).entity(agency).build();
           }
       }
       return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
   }
   
   /** Return specific transit stop in the graph, by ID. */
   @GET
   @Path("/stops/{stopId}")
   public Response getStop (@PathParam("stopId") String stopIdString) {
       AgencyAndId stopId = GtfsLibrary.convertIdFromString(stopIdString);
       Stop stop = index.stopForId.get(stopId);
       if (stop != null) {
           return Response.status(Status.OK).entity(stop).build();
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
           Collection<Stop> stops = index.stopForId.values();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
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
           List<StopShort> stops = Lists.newArrayList(); 
           Coordinate coord = new Coordinate(lon, lat);
           for (TransitStop stopVertex : streetIndex.getNearbyTransitStops(
                    new Coordinate(lon, lat), radius)) {
               double distance = distanceLibrary.fastDistance(stopVertex.getCoordinate(), coord);
               if (distance < radius) {
                   stops.add(new StopShort(stopVertex.getStop(), (int) distance));
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
           List<StopShort> stops = Lists.newArrayList();
           Envelope envelope = new Envelope(new Coordinate(minLon, minLat), new Coordinate(maxLon, maxLat));
           for (TransitStop stopVertex : streetIndex.getTransitStopForEnvelope(envelope)) {
               stops.add(new StopShort(stopVertex.getStop()));
           }
           return Response.status(Status.OK).entity(stops).build();           
       }
   }

   @GET
   @Path("/stops/{stopId}/routes")
   public Response getRoutesForStop (@PathParam("stopId") String stopId) {
       Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopId));
       if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       Set<Route> routes = Sets.newHashSet();
       for (TripPattern pattern : index.patternsForStop.get(stop)) {
           routes.add(pattern.route);
       }
       return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
   }

   @GET
   @Path("/stops/{stopId}/patterns")
   public Response getPatternsForStop (@PathParam("stopId") String stopIdString) {
       AgencyAndId id = GtfsLibrary.convertIdFromString(stopIdString);
       Stop stop = index.stopForId.get(id);
       if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       Collection<TripPattern> patterns = index.patternsForStop.get(stop);
       return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
   }

    /** Return upcoming vehicle arrival/departure times at the given stop. */
    @GET
    @Path("/stops/{stopId}/stoptimes")
    public Response getStoptimesForStop (@PathParam("stopId") String stopIdString) {
        Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopIdString));
        if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        return Response.status(Status.OK).entity(index.stopTimesForStop(stop)).build();
    }

   /** Return a list of all routes in the graph. */
   // with repeated hasStop parameters, replaces old routesBetweenStops
   @GET
   @Path("/routes")
   public Response getRoutes (@QueryParam("hasStop") List<String> stopIds) {
       Collection<Route> routes = index.routeForId.values();
       // Filter routes to include only those that pass through all given stops
       if (stopIds != null) {
           // Protective copy, we are going to calculate the intersection destructively
           routes = Lists.newArrayList(routes);
           for (String stopId : stopIds) {
               Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopId));
               if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
               Set<Route> routesHere = Sets.newHashSet();
               for (TripPattern pattern : index.patternsForStop.get(stop)) {
                   routesHere.add(pattern.route);
               }
               routes.retainAll(routesHere);
           }
       }
       return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
   }

   /** Return specific route in the graph, for the given ID. */
   @GET
   @Path("/routes/{routeId}")
   public Response getRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           return Response.status(Status.OK).entity(route).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all stop patterns used by trips on the given route. */
   @GET
   @Path("/routes/{routeId}/patterns")
   public Response getPatternsForRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           Collection<TripPattern> patterns = index.patternsForRoute.get(route);
           return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all stops in any pattern on a given route. */
   @GET
   @Path("/routes/{routeId}/stops")
   public Response getStopsForRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           Set<Stop> stops = Sets.newHashSet();
           Collection<TripPattern> patterns = index.patternsForRoute.get(route);
           for (TripPattern pattern : patterns) {
               stops.addAll(pattern.getStops());
           }
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all trips in any pattern on the given route. */
   @GET
   @Path("/routes/{routeId}/trips")
   public Response getTripsForRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           List<Trip> trips = Lists.newArrayList();
           Collection<TripPattern> patterns = index.patternsForRoute.get(route);
           for (TripPattern pattern : patterns) {
               trips.addAll(pattern.getTrips());
           }
           return Response.status(Status.OK).entity(TripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }
   
   
    // Not implemented, results would be too voluminous.
    // @Path("/trips")

   @GET
   @Path("/trips/{tripId}")
   public Response getTrip (@PathParam("tripId") String tripIdString) {
       AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
       Trip trip = index.tripForId.get(tripId);
       if (trip != null) {
           return Response.status(Status.OK).entity(trip).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/trips/{tripId}/stops")
   public Response getStopsForTrip (@PathParam("tripId") String tripIdString) {
       AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
       Trip trip = index.tripForId.get(tripId);
       if (trip != null) {
           TripPattern pattern = index.patternForTrip.get(trip);
           Collection<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

    @GET
    @Path("/trips/{tripId}/semanticHash")
    public Response getSemanticHashForTrip (@PathParam("tripId") String tripIdString) {
        AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
        Trip trip = index.tripForId.get(tripId);
        if (trip != null) {
            TripPattern pattern = index.patternForTrip.get(trip);
            String hashString = pattern.semanticHashString(trip);
            return Response.status(Status.OK).entity(hashString).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    @GET
   @Path("/trips/{tripId}/stoptimes")
   public Response getStoptimesForTrip (@PathParam("tripId") String tripIdString) {
       AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
       Trip trip = index.tripForId.get(tripId);
       if (trip != null) {
           TripPattern pattern = index.patternForTrip.get(trip);
           Timetable table = pattern.scheduledTimetable;
           return Response.status(Status.OK).entity(TripTimeShort.fromTripTimes(table, trip)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns")
   public Response getPatterns () {
       Collection<TripPattern> patterns = index.patternForId.values();
       return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
   }

   @GET
   @Path("/patterns/{patternId}")
   public Response getPattern (@PathParam("patternId") String patternIdString) {
       TripPattern pattern = index.patternForId.get(patternIdString);
       if (pattern != null) {
           return Response.status(Status.OK).entity(new PatternDetail(pattern)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns/{patternId}/trips")
   public Response getTripsForPattern (@PathParam("patternId") String patternIdString) {
       TripPattern pattern = index.patternForId.get(patternIdString);
       if (pattern != null) {
           List<Trip> trips = pattern.getTrips();
           return Response.status(Status.OK).entity(TripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns/{patternId}/stops")
   public Response getStopsForPattern (@PathParam("patternId") String patternIdString) {
       // Pattern names are graph-unique because we made them that way (did not read them from GTFS).
       TripPattern pattern = index.patternForId.get(patternIdString);
       if (pattern != null) {
           List<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

    @GET
    @Path("/patterns/{patternId}/semanticHash")
    public Response getSemanticHashForPattern (@PathParam("patternId") String patternIdString) {
        // Pattern names are graph-unique because we made them that way (did not read them from GTFS).
        TripPattern pattern = index.patternForId.get(patternIdString);
        if (pattern != null) {
            String semanticHash = pattern.semanticHashString(null);
            return Response.status(Status.OK).entity(semanticHash).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    // TODO include pattern ID for each trip in responses

    /** List basic information about all service IDs. */
    @GET
    @Path("/services")
    public Response getServices() {
        index.serviceForId.values(); // TODO complete
        return Response.status(Status.OK).entity("NONE").build();
    }

    /** List details about a specific service ID including which dates it runs on. Replaces the old /calendar. */
    @GET
    @Path("/services/{serviceId}")
    public Response getServices(@PathParam("serviceId") String serviceId) {
        index.serviceForId.get(serviceId); // TODO complete
        return Response.status(Status.OK).entity("NONE").build();
    }

    /** Return all clusters of stops. */
    @GET
    @Path("/clusters")
    public Response getAllStopClusters () {
        // use 'detail' field common to all API methods in this class
        List<StopClusterDetail> scl = StopClusterDetail.list(index.stopClusterForId.values(), detail);
        return Response.status(Status.OK).entity(scl).build();
    }

    /** Return a cluster of stops by its ID. */
    @GET
    @Path("/clusters/{clusterId}")
    public Response getStopCluster (@PathParam("clusterId") String clusterIdString) {
        StopCluster cluster = index.stopClusterForId.get(clusterIdString);
        if (cluster != null) {
            return Response.status(Status.OK).entity(new StopClusterDetail(cluster, true)).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

}