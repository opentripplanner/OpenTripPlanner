package org.opentripplanner.ext.geocoder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * OTP simple built-in geocoder used by the debug client.
 */
@Path("/geocode")
@Produces(MediaType.APPLICATION_JSON)
public class GeocoderResource {

  private final LuceneIndex luceneIndex;

  public GeocoderResource(@Context OtpServerRequestContext requestContext) {
    luceneIndex = requestContext.lucenceIndex();
  }

  @GET
  @Path("stopClusters")
  public Response stopClusters(
    @QueryParam("query") String query,
    @QueryParam("focusLatitude") Double focusLat,
    @QueryParam("focusLongitude") Double focusLon
  ) {
    if (query == null) {
      return Response.status(Response.Status.BAD_REQUEST)
        .entity(Map.of("error", "Query parameter 'query' must be provided."))
        .build();
    }
    WgsCoordinate focusPoint = null;
    if (focusLat != null && focusLon != null) {
      focusPoint = new WgsCoordinate(focusLat, focusLon);
    }
    var clusters = luceneIndex.queryStopClusters(query, focusPoint).toList();

    return Response.status(Response.Status.OK).entity(clusters).build();
  }
}
