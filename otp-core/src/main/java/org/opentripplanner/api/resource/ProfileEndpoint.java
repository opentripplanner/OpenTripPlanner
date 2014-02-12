package org.opentripplanner.api.resource;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.Setter;

import org.opentripplanner.profile.ProfileData;
import org.opentripplanner.profile.ProfileRouter;
import org.opentripplanner.profile.Response;
import org.opentripplanner.routing.services.GraphService;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;

@Autowire @Singleton @Path("/profile")
public class ProfileEndpoint {

    @Setter @InjectParam private GraphService graphService;
    private static ProfileData data = null;
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response profileRoute(@QueryParam("from") String from, 
                               @QueryParam("to")   String to,
                               @QueryParam("startTime") String startTime, 
                               @QueryParam("endTime")   String endTime) {
        String[] fromCoords = from.split(",");
        String[] toCoords = to.split(",");
        double fromLat = Double.parseDouble(fromCoords[0]);
        double fromLon = Double.parseDouble(fromCoords[1]);
        double toLat   = Double.parseDouble(toCoords[0]);
        double toLon   = Double.parseDouble(toCoords[1]);

        if (data == null) 
            data = new ProfileData(graphService.getGraph());
        ProfileRouter router = new ProfileRouter (data);
        Response response = new Response (router.route(fromLat, fromLon, toLat, toLon));
        return response;
    }
    
}
