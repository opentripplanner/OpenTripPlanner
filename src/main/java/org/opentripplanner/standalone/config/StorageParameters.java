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
 * Google Cloud Storage(GCS) access is supported. Use the following URI format:
 * <pre>
 *     gs://bucket-name/a/b/c/blobname.ext
 * </pre>
 * <p>
 * Example {@code build-config.json}:
 * <pre>
 * {
 *     htmlAnnotations: true,
 *     storage: {
 *         gsCredentials: "${OTP_GOOGLE_SERVICE_ACCOUNT}",
 *         osm: [ "gs://otp-test-bucket/a/b/osm-oslo-mini.pbf" ],
 *         dem: [ "file:/public/dem/norway.dem.tif" ],
 *         gtfs: ["gs://otp-bucket/rut-gtfs.zip", "gs://otp-bucket/vy-gtfs.zip"],
 *         buildReportDir: "gs://otp-bucket/build-report"
 *     }
 *  }
 * </pre>
 * In the example above, the Google cloud service credentials file resolved using an environment
 * variable. The OSM and GTFS data is streamed from Google Cloud Storage, the elevation data is
 * fetched from the local file system and the build report is stored in the cloud. All other
 * artifacts like the loaded graph, saved graph and NeTEx files are loaded and written from/to the local
 * base directory - it they exist.
 */
public class StorageParameters {

    /**
     * Local file system path to Google Cloud Platform service accounts credentials file. The
     * credentials is used to access GCS urls. When using GCS from outside of the bucket cluster you
     * need to provide a path the the service credentials. Environment variables in the path is
     * resolved.
     * <p>
     * Example: {@code "credentialsFile" : "${MY_GOC_SERVICE}"} or {@code "app-1-3983f9f66728.json"
     * : "~/"}
     * <p>
     * This is a path to a file on the local file system, not an URI.
     * <p>
     * This parameter is optional.
     */
    public final String gsCredentials;

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
        this.gsCredentials = node.path("gsCredentials").asText(null);
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
