package org.opentripplanner.ext.geocoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.model.StopCollection;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;

/**
 * OTP simple built-in geocoder used by the debug client.
 */
@Path("/routers/{ignoreRouterId}/geocode")
@Produces(MediaType.APPLICATION_JSON)
public class GeocoderResource {

    /**
     * @deprecated The support for multiple routers are removed from OTP2. See
     * https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated
    @PathParam("ignoreRouterId")
    private String ignoreRouterId;

    private final Router router;

    public GeocoderResource(@Context OTPServer otpServer) {
        router = otpServer.getRouter();
    }

    /**
     * Geocode using data using the OTP graph for stops, clusters and street names
     *
     * @param query        The query string we want to geocode
     * @param autocomplete Whether we should use the query string to do a prefix match
     * @param stops        Search for stops, either by name or stop code
     * @param clusters     Search for clusters by their name
     * @param corners      Search for street corners using at least one of the street names
     * @return list of results in in the format expected by GeocoderBuiltin.js in the OTP Leaflet
     * client
     */
    @GET
    public Response textSearch(
            @QueryParam("query") String query,
            @QueryParam("autocomplete") @DefaultValue("false") boolean autocomplete,
            @QueryParam("stops") @DefaultValue("true") boolean stops,
            @QueryParam("clusters") @DefaultValue("false") boolean clusters,
            @QueryParam("corners") @DefaultValue("true") boolean corners
    ) {
        return Response.status(Response.Status.OK).entity(
                query(query, autocomplete, stops, clusters, corners)
        ).build();
    }

    private List<SearchResult> query(
            String query,
            boolean autocomplete,
            boolean stops,
            boolean clusters,
            boolean corners
    ) {
        List<SearchResult> results = new ArrayList<>();

        if (stops) {
            results.addAll(queryStopLocations(query, autocomplete));
        }

        if (clusters) {
            results.addAll(queryStations(query, autocomplete));
        }

        if (corners) {
            results.addAll(queryCorners(query, autocomplete));
        }

        return results;
    }

    private Collection<SearchResult> queryStopLocations(
            String query,
            boolean autocomplete
    ) {
        return Stream.concat(
                router.graph
                        .getAllFlexStopsFlat()
                        .stream(),
                router.graph
                        .index
                        .getAllStops()
                        .stream()
        )
                .filter(sl -> filter(query, autocomplete, Objects.toString(sl.getName()), sl.getCode()))
                .map(sl -> new SearchResult(
                        sl.getCoordinate().latitude(),
                        sl.getCoordinate().longitude(),
                        stringifyStopLocation(sl),
                        FeedScopedIdMapper.mapToApi(sl.getId())
                ))
                .collect(Collectors.toList());
    }

    private Collection<? extends SearchResult> queryStations(String query, boolean autocomplete) {
        return Stream.concat(
                router.graph
                        .stationById
                        .values()
                        .stream()
                        .map(s -> (StopCollection) s),
                router.graph
                        .multiModalStationById
                        .values()
                        .stream()
                        .map(s -> (StopCollection) s)
        )
                .filter(sl -> filter(query, autocomplete, Objects.toString(sl.getName())))
                .map(sc -> new SearchResult(
                        sc.getCoordinate().latitude(),
                        sc.getCoordinate().longitude(),
                        Objects.toString(sc.getName()),
                        FeedScopedIdMapper.mapToApi(sc.getId())
                ))
                .collect(Collectors.toList());
    }

    private Collection<? extends SearchResult> queryCorners(String query, boolean autocomplete) {
        return router.graph
                .getVertices()
                .stream()
                .filter(v -> v instanceof StreetVertex)
                .map(v -> (StreetVertex) v)
                .filter(v -> filter(query, autocomplete, Objects.toString(v.getName()), Objects.toString(v.getIntersectionName())))
                .map(v -> new SearchResult(
                        v.getLat(),
                        v.getLon(),
                        stringifyStreetVertex(v),
                        v.getLabel()
                ))
                .collect(Collectors.toList());
    }

    private boolean filter(String sourceQuery, boolean autocomplete, String... values) {
        var query = sourceQuery.toLowerCase(Locale.ROOT);
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(v -> v.toLowerCase(Locale.ROOT))
                .anyMatch(value ->
                        autocomplete ? value.startsWith(query) : value.contains(query)
                );
    }

    private String stringifyStreetVertex(StreetVertex v) {
        return String.format("%s (%s)", v.getIntersectionName(), v.getLabel());
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
