package org.opentripplanner.api.resource;

import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;

/**
 * For debugging
 */
@Path("routers/{routerId}/rrtr")
public class RepeatedRaptorTestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);
    private Graph graph;
    private SurfaceCache surfaceCache;

    private int n_increase = 0;
    private int n_decrease= 0;
    private int n_same= 0;
    private int n_total = 0;

    public RepeatedRaptorTestResource (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        graph = router.graph;
        surfaceCache = otpServer.surfaceCache;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response profileRoute (
            @QueryParam("from") LatLon from,
            @QueryParam("fromStop") String fromStopString,
            @QueryParam("banRoute") @DefaultValue("TriMet:100") String banRouteString) {

        String[] banFields = banRouteString.split(":");
        AgencyAndId banRoute = new AgencyAndId(banFields[0], banFields[1]);
        if (from != null) {
            oneOrigin(from.lat, from.lon, banRoute);
        } else {
            Collection<Stop> originStops = new ArrayList<>();
            if (fromStopString != null) {
                String[] fields = fromStopString.split(":");
                originStops.add(graph.index.stopForId.get(new AgencyAndId(fields[0], fields[1])));
            } else {
                originStops = graph.index.stopForId.values();
            }
            LOG.info("from stops {}", originStops);
            for (Stop stop : originStops) {
                LOG.info("{}/{}: {}", 0, originStops.size(), stop);
                oneOrigin(stop.getLat(), stop.getLon(), banRoute);
            }
        }

        return Response.ok().entity("OK\n").build();
    }

    private void oneOrigin (double lat, double lon, AgencyAndId banRoute) {

        ProfileRequest req = new ProfileRequest();
        req.fromLat      = lat;
        req.fromLon      = lon;
        req.fromTime     = 8 * 60 * 60;
        req.toTime       = req.fromTime + 61;
        req.walkSpeed    = 2;
        req.bikeSpeed    = 4;
        req.carSpeed     = 8;
        req.date         = new LocalDate(2015, 04, 20);
        req.maxWalkTime  = 20; // minutes
        req.accessModes  = new QualifiedModeSet("WALK");
        req.egressModes  = new QualifiedModeSet("WALK");
        req.transitModes = new TraverseModeSet("TRANSIT");
        req.analyst      = true;

        if (surfaceCache == null) {
            LOG.error("You must run OTP with the --analyst option to enable spatial analysis features.");
        }

        final RepeatedRaptorProfileRouter router_a = new RepeatedRaptorProfileRouter(graph, req);
        final RepeatedRaptorProfileRouter router_b = new RepeatedRaptorProfileRouter(graph, req);
        router_b.banRoute = banRoute;

        try {
            router_a.route();
            router_b.route();
        } catch (VertexNotFoundException ex) {
            LOG.error("vertex not found");
            return;
        }

        System.out.printf("stop, min_a, min_b, min_diff, max_a, max_b, max_diff\n");
        boolean decreased = false;
        for (TransitStop destVertex : graph.index.stopVertexForStop.values()) {
            int min_a = router_a.mins.get(destVertex);
            int max_a = router_a.maxs.get(destVertex);
            int min_b = router_b.mins.get(destVertex);
            int max_b = router_b.maxs.get(destVertex);
            long min_diff = (long) min_b - min_a;
            long max_diff = (long) max_b - max_a;
            if (min_b == Integer.MAX_VALUE) {
                min_diff = Integer.MAX_VALUE;
            }
            if (max_b == Integer.MIN_VALUE) {
                max_diff = Integer.MAX_VALUE;
            }
            n_total += 1;
            if (min_diff < 0 || max_diff < 0) {
                n_decrease += 1;
                // Time decreased due to banning a route. This is bad, print it out.
                System.out.printf("\"%s\",%d,%d,%d,%d,%d,%d\n",
                        destVertex.getStop().getId(), min_a, min_b, min_diff, max_a, max_b, max_diff);
                decreased = true;
            } else if (min_diff > 0 || max_diff > 0) {
                n_increase += 1;
            } else {
                n_same += 1;
            }

        }
        if (decreased) {
            LOG.error("Decreases happened at this origin!");
        }
        LOG.info("{} increased, {} decreased out of {} destinations total", n_increase, n_decrease, n_total);
    }
}
