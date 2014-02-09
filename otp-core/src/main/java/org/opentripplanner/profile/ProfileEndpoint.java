package org.opentripplanner.profile;

import java.text.ParseException;

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

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.services.GraphService;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;

@Autowire @Singleton @Path("/profile")
public class ProfileEndpoint {

    @Setter @InjectParam private GraphService graphService;
    private static ProfileData data = null;

    private int secondsFromString (String t) throws ParseException {
        String[] fields = t.split(":");
        int h = Integer.parseInt(fields[0]) ;
        int m = Integer.parseInt(fields[1]) ;
        return (h * 60 + m) * 60;
    }
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response profileRoute(
            @QueryParam("from") String from, 
            @QueryParam("to")   String to,
            @QueryParam("date") String dateString,
            @QueryParam("startTime") @DefaultValue("07:00") String fromTimeString, 
            @QueryParam("endTime")   @DefaultValue("09:00") String toTimeString) {
        if (data == null) data = new ProfileData(graphService.getGraph());
        try {
            ServiceDate date;
            if (dateString == null || dateString.isEmpty()) {
                date = new ServiceDate ();
            } else {
                date = ServiceDate.parseString(dateString);
            }
            String[] fromFields = from.split(",");
            String[] toFields = to.split(",");
            double fromLat = Double.parseDouble(fromFields[0]);
            double fromLon = Double.parseDouble(fromFields[1]);
            double toLat   = Double.parseDouble(toFields[0]);
            double toLon   = Double.parseDouble(toFields[1]);
            int fromTime = secondsFromString(fromTimeString);
            int toTime   = secondsFromString(toTimeString);
            ProfileRouter router = new ProfileRouter (data);
            // we need the graph context to interpret the date as serviceIds, so pass in the raw parameters
            ProfileResponse pr = new ProfileResponse(router.route(fromLat, fromLon, toLat, toLon, fromTime, toTime, date));
            return Response.status(Status.OK).entity(pr).build();
        } catch (ParseException pe) {
            return Response.status(Status.BAD_REQUEST).entity("Error parsing parameter.").build();
        } catch (NumberFormatException pe) {
            return Response.status(Status.BAD_REQUEST).entity("Number format error.").build();
        } catch (NullPointerException npe) {
            return Response.status(Status.BAD_REQUEST).entity("Missing parameter.").build();
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return Response.status(Status.BAD_REQUEST).entity("Malformed parameter.").build();
        }
    }
    
}
