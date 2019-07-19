package org.opentripplanner.api.resource;

import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.raptor.router.RaptorRouter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.api.resource.ServerInfo.Q;

/**
 * This is the primary entry point for the trip planning web service.
 * All parameters are passed in the query string. These parameters are defined as fields in the abstract
 * RoutingResource superclass, which also has methods for building routing requests from query
 * parameters. This allows multiple web services to have the same set of query parameters.
 * In order for inheritance to work, the REST resources are request-scoped (constructed at each request)
 * rather than singleton-scoped (a single instance existing for the lifetime of the OTP server).
 */
@Path("routers/{routerId}/plan") // final element needed here rather than on method to distinguish from routers API
public class PlannerResource extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(PlannerResource.class);

    // We inject info about the incoming request so we can include the incoming query
    // parameters in the outgoing response. This is a TriMet requirement.
    // Jersey uses @Context to inject internal types and @InjectParam or @Resource for DI objects.
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public Response plan(@Context UriInfo uriInfo, @Context Request grizzlyRequest) {

        /*
         * TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)
         * TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords 
         *       or vertex ids to planner (or error back to user)
         * TODO: org.opentripplanner.routing.module.PathServiceImpl has COOORD parsing. Abstract that
         *       out so it's used here too...
         */

        // Create response object, containing a copy of all request parameters. Maybe they should be in the debug section of the response.
        Response response = new Response(uriInfo);
        RoutingRequest request = null;
        Router router = null;
        List<GraphPath> paths = null;
        try {

            /* Fill in request fields from query parameters via shared superclass method, catching any errors. */
            request = super.buildRequest();
            router = otpServer.getRouter(request.routerId);
            request.setRoutingContext(router.graph);

            List<Itinerary> itineraries = new ArrayList<>();

            if (request.modes.getNonTransitSet().isValid()) {
                double distance = SphericalDistanceLibrary.distance(request.rctx.origin.getCoordinate(), request.rctx.target.getCoordinate());
                double limit = request.maxWalkDistance * 2;
                // Handle int overflow, in which case the multiplication will be less than zero
                if (limit < 0 || distance < limit) {
                    itineraries.addAll(findNonTransitItineraries(request, router));
                }
            }

            if (request.modes.isTransit()) {
                // Route on realtime data, and compare with scheduled data while converting to an Itinerary.
                RaptorRouter raptorRouter = new RaptorRouter(request, router.graph.realtimeTransitLayer);
                itineraries.addAll(raptorRouter.route());
            }

            if (itineraries.isEmpty()) {
                throw new PathNotFoundException();
            }

            TripPlan tripPlan = createTripPlan(request, itineraries);
            response.setPlan(tripPlan);

            /* Populate up the elevation metadata */
            response.elevationMetadata = new ElevationMetadata();
            response.elevationMetadata.ellipsoidToGeoidDifference = router.graph.ellipsoidToGeoidDifference;
            response.elevationMetadata.geoidElevation = request.geoidElevation;

        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            if(!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            response.setError(error);
        } finally {
            if (request != null) {
                if (request.rctx != null) {
                    response.debugOutput = request.rctx.debugOutput;
                }
                request.cleanup(); // TODO verify that this cleanup step is being done on Analyst web services
            }
        }

        /* Log this request if such logging is enabled. */
        if (request != null && router != null && router.requestLogger != null) {
            StringBuilder sb = new StringBuilder();
            String clientIpAddress = grizzlyRequest.getRemoteAddr();
            //sb.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            sb.append(clientIpAddress);
            sb.append(' ');
            sb.append(request.arriveBy ? "ARRIVE" : "DEPART");
            sb.append(' ');
            sb.append(LocalDateTime.ofInstant(Instant.ofEpochSecond(request.dateTime), ZoneId.systemDefault()));
            sb.append(' ');
            sb.append(request.modes.getAsStr());
            sb.append(' ');
            sb.append(request.from.lat);
            sb.append(' ');
            sb.append(request.from.lng);
            sb.append(' ');
            sb.append(request.to.lat);
            sb.append(' ');
            sb.append(request.to.lng);
            sb.append(' ');
            if (paths != null) {
                for (GraphPath path : paths) {
                    sb.append(path.getDuration());
                    sb.append(' ');
                    sb.append(path.getTrips().size());
                    sb.append(' ');
                }
            }
            router.requestLogger.info(sb.toString());
        }

        return response;
    }

    private List<Itinerary> findNonTransitItineraries(RoutingRequest request, Router router) {
        RoutingRequest nonTransitRequest = request.clone();
        nonTransitRequest.modes.setTransit(false);

        try {
            // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
            GraphPathFinder gpFinder = new GraphPathFinder(router);
            List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(nonTransitRequest);

            /* Convert the internal GraphPaths to a TripPlan object that is included in an OTP web service Response. */
            TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
            return plan.itinerary;
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    private TripPlan createTripPlan(RoutingRequest request, List<Itinerary> itineraries) {
        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        itineraries = itineraries.stream().sorted(Comparator.comparing(i -> i.endTime))
                .limit(request.numItineraries).collect(Collectors.toList());
        tripPlan.itinerary = itineraries;
        LOG.info("Returning {} itineraries", itineraries.size());
        return tripPlan;
    }
}
