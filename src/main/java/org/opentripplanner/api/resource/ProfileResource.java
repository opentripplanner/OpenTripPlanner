package org.opentripplanner.api.resource;

import java.util.List;

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

import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.api.model.TimeSurfaceShort;
import org.opentripplanner.api.param.HourMinuteSecond;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.param.QueryParameter;
import org.opentripplanner.api.param.YearMonthDay;
import org.opentripplanner.profile.Option;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.ProfileResponse;
import org.opentripplanner.profile.ProfileRouter;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import com.google.common.collect.Lists;

/**
 * A Jersey resource class which exposes OTP profile routing functionality
 * as a web service.
 */
@Path("routers/{routerId}/profile")
public class ProfileResource {

    private Graph graph;
    private SurfaceCache surfaceCache;

    public ProfileResource (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        graph = router.graph;
        surfaceCache = otpServer.surfaceCache;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response profileRoute (
            @QueryParam("from")  LatLon from,
            @QueryParam("to")    LatLon to,
            @QueryParam("analyst")      @DefaultValue("false") boolean analyst,
            @QueryParam("date")         @DefaultValue("today") YearMonthDay date,
            @QueryParam("startTime")    @DefaultValue("07:00") HourMinuteSecond fromTime,
            @QueryParam("endTime")      @DefaultValue("09:00") HourMinuteSecond toTime,
            @QueryParam("walkSpeed")    @DefaultValue("1.4")   float walkSpeed, // m/sec
            @QueryParam("bikeSpeed")    @DefaultValue("4.1")   float bikeSpeed, // m/sec
            @QueryParam("carSpeed")     @DefaultValue("20")    float carSpeed,  // m/sec
            @QueryParam("streetTime")   @DefaultValue("90")    int streetTime,  // max minutes to reach destination WITHOUT transit
            @QueryParam("maxWalkTime")  @DefaultValue("15")    int maxWalkTime, // max minutes to reach transit on foot
            @QueryParam("maxCarTime")   @DefaultValue("30")    int maxCarTime,
            @QueryParam("maxBikeTime")  @DefaultValue("20")    int maxBikeTime,
            @QueryParam("minCarTime")   @DefaultValue("1")     int minCarTime,
            @QueryParam("minBikeTime")  @DefaultValue("1")     int minBikeTime,
            @QueryParam("orderBy")      @DefaultValue("AVG")   Option.SortOrder orderBy,
            @QueryParam("limit")        @DefaultValue("10")    int limit,
            @QueryParam("suboptimal")   @DefaultValue("5")     int suboptimalMinutes,
            @QueryParam("accessModes")  @DefaultValue("WALK,BICYCLE") TraverseModeSet accessModes,
            @QueryParam("egressModes")  @DefaultValue("WALK")         TraverseModeSet egressModes,
            @QueryParam("directModes")  @DefaultValue("WALK,BICYCLE") TraverseModeSet directModes,
            @QueryParam("transitModes") @DefaultValue("TRANSIT")      TraverseModeSet transitModes)
            throws Exception {

        QueryParameter.checkRangeInclusive(limit, 0, Integer.MAX_VALUE);
        QueryParameter.checkRangeInclusive(walkSpeed, 1, 7);
        QueryParameter.checkRangeInclusive(bikeSpeed, 1, 21);
        QueryParameter.checkRangeInclusive(carSpeed,  1, 36);
        QueryParameter.checkRangeInclusive(streetTime,  1, 480);
        QueryParameter.checkRangeInclusive(maxWalkTime, 1, 480);
        QueryParameter.checkRangeInclusive(maxBikeTime, 1, 480);
        QueryParameter.checkRangeInclusive(maxCarTime,  1, 480);
        QueryParameter.checkRangeInclusive(minBikeTime, 0, maxBikeTime);
        QueryParameter.checkRangeInclusive(minCarTime,  0, maxCarTime);
        QueryParameter.checkRangeInclusive(suboptimalMinutes, 0, 30);

        ProfileRequest req = new ProfileRequest();
        req.from         = from;
        req.to           = to;
        req.fromTime     = fromTime.toSeconds();
        req.toTime       = toTime.toSeconds();
        req.walkSpeed    = walkSpeed;
        req.bikeSpeed    = bikeSpeed;
        req.carSpeed     = carSpeed;
        req.date         = date.toJoda();
        req.orderBy      = orderBy;
        req.limit        = limit;
        req.accessModes  = accessModes;
        req.egressModes  = egressModes;
        req.directModes  = directModes;
        req.transitModes = transitModes;
        req.analyst      = analyst;
        req.streetTime   = streetTime;
        req.maxWalkTime  = maxWalkTime;
        req.maxBikeTime  = maxBikeTime;
        req.maxCarTime   = maxCarTime;
        req.minBikeTime  = minBikeTime;
        req.minCarTime   = minCarTime;
        req.suboptimalMinutes = suboptimalMinutes;

        ProfileRouter router = new ProfileRouter(graph, req);
        try {
            ProfileResponse response = router.route();
            if (req.analyst) {
                surfaceCache.add(router.minSurface);
                surfaceCache.add(router.maxSurface);
                List<TimeSurfaceShort> surfaceShorts = Lists.newArrayList();
                surfaceShorts.add(new TimeSurfaceShort(router.minSurface));
                surfaceShorts.add(new TimeSurfaceShort(router.maxSurface));
                return Response.status(Status.OK).entity(surfaceShorts).build();
            } else {
                return Response.status(Status.OK).entity(response).build();
            }
        } finally {
            router.cleanup(); // destroy routing contexts even when an exception happens
        }

    }
    
}
