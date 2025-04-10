package org.opentripplanner.ext.geocoder;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.site.StopLocation;

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

  /**
   * This class is only here for backwards-compatibility. It will be removed in the future.
   */
  @Path("/routers/{ignoreRouterId}/geocode")
  public static class GeocoderResourceOldPath extends GeocoderResource {

    public GeocoderResourceOldPath(
      @Context OtpServerRequestContext serverContext,
      @PathParam("ignoreRouterId") String ignore
    ) {
      super(serverContext);
    }
  }

  /**
   * Geocode using data using the OTP graph for stops, clusters and street names
   *
   * @param query        The query string we want to geocode
   * @param autocomplete Whether we should use the query string to do a prefix match
   * @param stops        Search for stops, either by name or stop code
   * @param clusters     Search for clusters by their name
   * @return list of results in the format expected by GeocoderBuiltin.js in the OTP Leaflet
   * client
   */
  @GET
  public Response textSearch(
    @QueryParam("query") String query,
    @QueryParam("autocomplete") @DefaultValue("false") boolean autocomplete,
    @QueryParam("stops") @DefaultValue("true") boolean stops,
    @QueryParam("clusters") @DefaultValue("false") boolean clusters
  ) {
    return Response.status(Response.Status.OK)
      .entity(query(query, autocomplete, stops, clusters))
      .build();
  }

  @GET
  @Path("stopClusters")
  public Response stopClusters(@QueryParam("query") String query) {
    var clusters = luceneIndex.queryStopClusters(query).toList();

    return Response.status(Response.Status.OK).entity(clusters).build();
  }

  private List<SearchResult> query(
    String query,
    boolean autocomplete,
    boolean stops,
    boolean clusters
  ) {
    List<SearchResult> results = new ArrayList<>();

    if (stops) {
      results.addAll(queryStopLocations(query, autocomplete));
    }

    if (clusters) {
      results.addAll(queryStations(query, autocomplete));
    }

    return results;
  }

  private Collection<SearchResult> queryStopLocations(String query, boolean autocomplete) {
    return luceneIndex
      .queryStopLocations(query, autocomplete)
      .map(sl ->
        new SearchResult(
          sl.getCoordinate().latitude(),
          sl.getCoordinate().longitude(),
          stringifyStopLocation(sl),
          sl.getId().toString()
        )
      )
      .collect(Collectors.toList());
  }

  private Collection<? extends SearchResult> queryStations(String query, boolean autocomplete) {
    return luceneIndex
      .queryStopLocationGroups(query, autocomplete)
      .map(sc ->
        new SearchResult(
          sc.getCoordinate().latitude(),
          sc.getCoordinate().longitude(),
          Objects.toString(sc.getName()),
          sc.getId().toString()
        )
      )
      .collect(Collectors.toList());
  }

  private String stringifyStopLocation(StopLocation sl) {
    return sl.getCode() != null
      ? String.format("%s (%s)", sl.getName(), sl.getCode())
      : Objects.toString(sl.getName());
  }

  public static class SearchResult {

    public double lat;
    public double lng;
    public String description;
    public String id;

    private SearchResult(double lat, double lng, String description, String id) {
      this.lat = lat;
      this.lng = lng;
      this.description = description;
      this.id = id;
    }
  }
}
