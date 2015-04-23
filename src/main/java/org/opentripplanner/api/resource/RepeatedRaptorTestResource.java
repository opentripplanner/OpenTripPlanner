package org.opentripplanner.api.resource;

import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private int n_total = 0;
    private long sum_decrease = 0;

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
            @QueryParam("banAgency") String banAgency,
            @QueryParam("banRoute") String banRouteString) {

        if (from != null) {
            oneOrigin(from.lat, from.lon, banAgency);
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
                // Shift slightly so we don't always search right on top of a transit stop.
                oneOrigin(stop.getLat() + 0.001, stop.getLon() + 0.001, banAgency);
            }
        }

        return Response.ok().entity("OK\n").build();
    }

    private void oneOrigin (double lat, double lon, String banAgency) {

        ProfileRequest req = new ProfileRequest();
        req.fromLat      = lat;
        req.fromLon      = lon;
        req.fromTime     = 60 * 60 * 8;
        req.toTime       = 60 * 60 * 9;
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
        router_b.banAgency = banAgency;

        try {
            router_a.route();
            router_b.route();
        } catch (VertexNotFoundException ex) {
            LOG.error("vertex not found");
            return;
        }

        System.out.printf("stop, min_a, min_b, min_diff, max_a, max_b, max_diff\n");
        boolean decreased = false;

        // Compare the propagated results
        decreased = false;
        TimeSurface.RangeSet timeSurfaces_a = router_a.timeSurfaceRangeSet;
        TimeSurface.RangeSet timeSurfaces_b = router_b.timeSurfaceRangeSet;
        for (Vertex destVertex : timeSurfaces_a.min.times.keySet()) {
            int min_a = timeSurfaces_a.min.getTime(destVertex);
            int max_a = timeSurfaces_a.max.getTime(destVertex);
            int avg_a = timeSurfaces_a.avg.getTime(destVertex);
            int min_b = timeSurfaces_b.min.getTime(destVertex);
            int max_b = timeSurfaces_b.max.getTime(destVertex);
            int avg_b = timeSurfaces_b.avg.getTime(destVertex);
            long min_diff = (long) min_b - min_a;
            long max_diff = (long) max_b - max_a;
            long avg_diff = (long) avg_b - avg_a;
            if (min_b == TimeSurface.UNREACHABLE) {
                min_diff = Integer.MAX_VALUE;
                max_diff = Integer.MAX_VALUE;
                avg_diff = Integer.MAX_VALUE;
            }
            n_total += 1;
            if (min_diff < 0 || max_diff < 0 || avg_diff < 0) {
                n_decrease += 1;
                sum_decrease += max_diff;
                // Time decreased due to banning a route. This is bad, print it out.
                System.out.printf("\"%s\",%d,%d,%d,%d,%d,%d\n",
                        destVertex.getName(), min_a, min_b, min_diff, max_a, max_b, max_diff);
                decreased = true;
            } else if (avg_diff > 0) {
                n_increase += 1;
            }

        }
        if (decreased) {
            LOG.error("Decreases happened at propagated street vertices for this origin!");
        }
        LOG.info("Street Vertices: {} increased, {} decreased out of {} destinations total", n_increase, n_decrease, n_total);
    }
}
