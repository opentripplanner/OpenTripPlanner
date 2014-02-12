package org.opentripplanner.profile;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.opentripplanner.api.param.HourMinuteSecond;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.param.YearMonthDay;
import org.opentripplanner.routing.services.GraphService;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;

@Autowire @Singleton @Path("/profile")
public class ProfileEndpoint {

    @Setter @InjectParam private GraphService graphService;
    private static ProfileData data = null;
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response profileRoute(
            @QueryParam("from") LatLon from, 
            @QueryParam("to")   LatLon to,
            @QueryParam("date")      @DefaultValue("today") YearMonthDay date,
            @QueryParam("startTime") @DefaultValue("07:00") HourMinuteSecond fromTime, 
            @QueryParam("endTime")   @DefaultValue("09:00") HourMinuteSecond toTime) {
        if (data == null) data = new ProfileData(graphService.getGraph());
        ProfileRouter router = new ProfileRouter (data);
        ProfileResponse pr = router.route(from, to, fromTime.toSeconds(), toTime.toSeconds(), date.toJoda());
        return Response.status(Status.OK).entity(pr).build();
    }
    
}
