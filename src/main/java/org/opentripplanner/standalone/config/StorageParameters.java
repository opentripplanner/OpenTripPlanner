package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Configure paths to each individual file resource. Use URIs to specify paths. If a parameter is
 * specified, it override any local files, and the local file is NOT loaded.
 * <p>
 * Local file access is supported. Use the following URI format:
 * <pre>
 *     file:/a/b/c/filename.ext
 * </pre>
 * <p>
 * Example {@code build-config.json}:
 * <pre>
 * {
 *     htmlAnnotations: true,
 *     storage: {
 *         osm: [ "file:/a/b/osm-oslo-mini.pbf" ],
 *         dem: [ "file:/public/dem/norway.dem.tif" ],
 *         gtfs: ["file:/a/b/rut-gtfs.zip", "file:/a/b/vy-gtfs.zip"],
 *         buildReportDir: "file:/a/b/otp/build-report"
 *     }
 *  }
 * </pre>
 */
public class StorageParameters {

    /**
     * URI to the street graph object file for reading and writing. The file is created or
     * overwritten if OTP saves the graph to the file.
     * <p>
     * Example: {@code "streetGraph" : "file:///Users/kelvin/otp/streetGraph.obj" }
     * <p>
     * This parameter is optional.
     */
    public final URI streetGraph;

    /**
     * URI to the graph object file for reading and writing. The file is created or overwritten if
     * OTP saves the graph to the file.
     * <p>
     * Example: {@code "graph" : "gs://my-bucket/otp/graph.obj" }
     * <p>
     * This parameter is optional.
     */
    public final URI graph;

    /**
     * Array of URIs to the open street map pbf files (the pbf format is the only one supported).
     * <p>
     * Example: {@code "osm" : [ "file:///Users/kelvin/otp/norway-osm.pbf" ] }
     * <p>
     * This parameter is optional.
     */
    public final List<URI> osm = new ArrayList<>();

    /**
     * Array of URIs to elevation data files.
     * <p>
     * Example: {@code "osm" : [ "file:///Users/kelvin/otp/norway-dem.tif" ] }
     * <p>
     * This parameter is optional.
     */
    public final List<URI> dem = new ArrayList<>();

    /**
     * Array of URIs to GTFS data files .
     * <p>
     * Example: {@code "transit" : [ "file:///Users/kelvin/otp/gtfs.zip", "gs://my-bucket/gtfs.zip" ]" }
     * <p>
     * This parameter is optional.
     */
    @NotNull
    public final List<URI> gtfs = new ArrayList<>();

    /**
     * Array of URIs to Netex data files.
     * <p>
     * Example: {@code "transit" : [ "file:///Users/kelvin/otp/netex.zip", "gs://my-bucket/netex.zip" ]" }
     * <p>
     * This parameter is optional.
     */
    @NotNull
    public final List<URI> netex = new ArrayList<>();

    /**
     * URI to the directory where the graph build report should be written to. The html report is
     * written into this directory. If the directory exist, any existing files are deleted.
     * If it does not exist, it is created.
     * <p>
     * Example: {@code "osm" : "file:///Users/kelvin/otp/buildReport" }
     * <p>
     * This parameter is optional.
     */
    public final URI buildReportDir;

    StorageParameters(JsonNode node) {
        this.graph = uriFromJson("graph", node);
        this.streetGraph = uriFromJson("streetGraph", node);
        this.osm.addAll(uris("osm", node));
        this.dem.addAll(uris("dem", node));
        this.gtfs.addAll(uris("gtfs", node));
        this.netex.addAll(uris("netex", node));
        this.buildReportDir = uriFromJson("buildReportDir", node);
    }

    static List<URI> uris(String name, JsonNode node) {
        List<URI> uris = new ArrayList<>();
        JsonNode array = node.path(name);

        if(array.isMissingNode()) {
            return uris;
        }
        if(!array.isArray()) {
            throw new IllegalArgumentException(
                    "Unable to parse 'storage' parameter in 'build-config.json': "
                    + "\n\tActual: \"" + name + "\" : \"" + array.asText() + "\""
                    + "\n\tExpected ARRAY of URIs: [ \"<uri>\", .. ]."
            );
        }
        for (JsonNode it : array) {
            uris.add(uriFromString(name, it.asText()));
        }
        return uris;
    }

    static URI uriFromJson(String name, JsonNode node) {
        return uriFromString(name, node.path(name).asText());
    }

    static URI uriFromString(String name, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new URI(text);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Unable to parse 'storage' parameter in 'build-config.json': "
                    + "\n\tActual: \"" + name + "\" : \"" + text + "\""
                    + "\n\tExpected valid URI, it should be parsable by java.net.URI class.");
        }
    }
}
