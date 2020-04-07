package org.opentripplanner.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.opentripplanner.api.parameter.BoundingBox;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResults;

import org.locationtech.jts.geom.Envelope;

/**
 * Maybe the internal geocoder resource should just chain to defined external geocoders?
 */
@Path("/geocode")
public class ExternalGeocoderResource {
  
// uncommenting injectparam will require a specific Geocoder to be instantiated
//    @InjectParam 
    public Geocoder geocoder;
    
    @GET
    @Produces({MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    public GeocoderResults geocode(
            @QueryParam("address") String address,
            @QueryParam("bbox") BoundingBox bbox) {
        if (address == null) {
            badRequest ("no address");
        }
        Envelope env = (bbox == null) ? null : bbox.envelope();
        return geocoder.geocode(address, env);
    }

    private void badRequest (String message) {
        throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                .entity(message).type("text/plain").build());
    }
}
