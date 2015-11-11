package org.opentripplanner.api.resource;

import com.beust.jcommander.internal.Maps;
import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.param.HourMinuteSecond;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.param.QueryParameter;
import org.opentripplanner.api.param.YearMonthDay;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.profile.Option;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.ProfileResponse;
import org.opentripplanner.profile.ProfileRouter;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
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
import javax.ws.rs.core.Response.Status;
import java.util.Map;

/**
 * A Jersey resource class which exposes OTP profile routing functionality as a web service.
 *
 */
@Path("routers/{routerId}/profile")
public class ProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);
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
            @QueryParam("startTime")    @DefaultValue("07:30") HourMinuteSecond fromTime,
            @QueryParam("endTime")      @DefaultValue("08:30") HourMinuteSecond toTime,
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
            @QueryParam("limit")        @DefaultValue("15")    int limit,       // max options to return PER ACCESS MODE
            @QueryParam("suboptimal")   @DefaultValue("5")     int suboptimalMinutes,
            @QueryParam("bikeSafe")     @DefaultValue("1")     int bikeSafe,
            @QueryParam("bikeSlope")    @DefaultValue("1")     int bikeSlope,
            @QueryParam("bikeTime")     @DefaultValue("1")     int bikeTime,
            @QueryParam("accessModes")  @DefaultValue("WALK,BICYCLE") QualifiedModeSet accessModes,
            @QueryParam("egressModes")  @DefaultValue("WALK")         QualifiedModeSet egressModes,
            @QueryParam("directModes")  @DefaultValue("WALK,BICYCLE") QualifiedModeSet directModes,
            @QueryParam("transitModes") @DefaultValue("TRANSIT")      TraverseModeSet transitModes,
            @QueryParam("banAgency") String banAgency)
            throws Exception {

        QueryParameter.checkRangeInclusive(limit, 0, Integer.MAX_VALUE);
        QueryParameter.checkRangeInclusive(walkSpeed, 0.5, 7);
        QueryParameter.checkRangeInclusive(bikeSpeed, 1, 21);
        QueryParameter.checkRangeInclusive(carSpeed,  1, 36);
        QueryParameter.checkRangeInclusive(streetTime,  1, 480);
        QueryParameter.checkRangeInclusive(maxWalkTime, 1, 480);
        QueryParameter.checkRangeInclusive(maxBikeTime, 1, 480);
        QueryParameter.checkRangeInclusive(maxCarTime,  1, 480);
        QueryParameter.checkRangeInclusive(minBikeTime, 0, maxBikeTime);
        QueryParameter.checkRangeInclusive(minCarTime,  0, maxCarTime);
        QueryParameter.checkRangeInclusive(suboptimalMinutes, 0, 30);
        QueryParameter.checkRangeInclusive(bikeSafe,  0, 1000);
        QueryParameter.checkRangeInclusive(bikeSlope, 0, 1000);
        QueryParameter.checkRangeInclusive(bikeTime,  0, 1000);

        ProfileRequest req = new ProfileRequest();
        req.fromLat      = from.lat;
        req.fromLon      = from.lon;
        // In analyst requests the 'to' coordinates may be null.
        // We need to provide some value though because lower-level routing requests are intolerant of a missing 'to'.
        if (to == null) to = from;
        req.toLat = to.lat;
        req.toLon = to.lon;
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
        req.bikeSafe     = bikeSafe;
        req.bikeSlope    = bikeSlope;
        req.bikeTime     = bikeTime;
        req.suboptimalMinutes = suboptimalMinutes;

        if (req.analyst) {
            if (surfaceCache == null) {
                LOG.error ("You must run OTP with the --analyst option to enable spatial analysis features.");
            }
            TimeSurface.RangeSet result;

            /* There are rarely frequency-only graphs. Use the Raptor profile router for both freqs and schedules. */
            RepeatedRaptorProfileRouter router = new RepeatedRaptorProfileRouter(graph, req);
            router.banAgency = banAgency;
            router.route();
            result = router.timeSurfaceRangeSet;
            Map<String, Integer> idForSurface = Maps.newHashMap();
            idForSurface.put("min", surfaceCache.add(result.min)); // requires analyst mode turned on
            idForSurface.put("avg", surfaceCache.add(result.avg));
            idForSurface.put("max", surfaceCache.add(result.max));
            return Response.status(Status.OK).entity(idForSurface).build();
        } else {
            ProfileRouter router = new ProfileRouter(graph, req);
            try {
                ProfileResponse response = router.route();
                return Response.status(Status.OK).entity(response).build();
            } catch (Throwable throwable) {
                LOG.error("Exception caught in profile routing", throwable);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(throwable.toString()).build();
            } finally {
                router.cleanup(); // destroy routing contexts even when an exception happens
            }
        }
    }
    
}
