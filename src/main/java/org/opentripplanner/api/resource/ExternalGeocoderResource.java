package org.opentripplanner.api.resource;

import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.parameter.BoundingBox;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResults;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
            throw new BadRequestException("no address");
        }
        Envelope env = (bbox == null) ? null : bbox.envelope();
        return geocoder.geocode(address, env);
    }

}
