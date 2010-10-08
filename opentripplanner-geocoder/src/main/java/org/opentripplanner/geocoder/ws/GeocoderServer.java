package org.opentripplanner.geocoder.ws;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResults;


@Path("/geocode")
public class GeocoderServer {
    
    private Geocoder geocoder;
    
    public void setGeocoder(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public GeocoderResults geocode(@QueryParam("address") String address) {
        if (address == null) {
            throw new WebApplicationException(Response.status(400)
                    .entity("no address")
                    .type("text/plain")
                    .build());
        }
        return geocoder.geocode(address);
    }
}
