package org.opentripplanner.datastore;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;


/**
 * The {@link OtpDataStore} config, needed to create a store. This should be provided by the
 * OTP application.
 */
public interface OtpDataStoreConfig {

    /**
     * Match all filenames that contains "gtfs".
     * The pattern is NOT Case sensitive.
     */
    String DEFAULT_GTFS_PATTERN = "(?i)gtfs";

    /**
     * Match all filenames that contain "netex".
     * The pattern is NOT Case sensitive.
     */
    String DEFAULT_NETEX_PATTERN = "(?i)netex";

    /**
     * Match all filenames that ends with suffix {@code .pbf}, {@code .osm} or {@code .osm.xml}.
     * The pattern is NOT Case sensitive.
     */
    String DEFAULT_OSM_PATTERN = "(?i)(\\.pbf|\\.osm|\\.osm\\.xml)$";

    /**
     * Default: {@code (?i).tiff?$} - Match all filenames that ends with suffix
     * {@code .tif} or {@code .tiff}.
     * The pattern is NOT Case sensitive.
     */
    String DEFAULT_DEM_PATTERN = "(?i)\\.tiff?$";

    /**
     * The base directory on the local file-system. Used to lookup config files and all input
     * files if no URIs are found in the config.
     */
    File baseDirectory();

    /**
     * Save the build issue report to this location (URI).
     * If {@code null} the {@link #baseDirectory()} + {@code /report} is used.
     */
    URI reportDirectory();

    /**
     * Local file system path to Google Cloud Platform service accounts credentials file. The
     * credentials is used to access GCS urls. When using GCS from outside of the bucket cluster you
     * need to provide a path the the service credentials.
     * <p>
     * This is a path to a file on the local file system, not an URI.
     * <p>
     * Optional. May return {@code null}.
     */
    String gsCredentials();

    /**
     * Array of URIs to the open street map pbf files (the pbf format is the only one supported).
     * <p>
     * This parameter is optional. If {@code null} OSM files are loaded from
     * {@link #baseDirectory()}.
     */
    List<URI> osmFiles();

    /**
     * Array of URIs to elevation data files.
     * <p>
     * This parameter is optional. If {@code null} DEM files are loaded from
     * {@link #baseDirectory()}.
     */
     List<URI> demFiles();

    /**
     * Array of URIs to GTFS data files .
     * <p>
     * This parameter is optional. If {@code null} GTFS files are loaded from
     * {@link #baseDirectory()}.
     */
    @NotNull List<URI> gtfsFiles();

    /**
     * Array of URIs to Netex data files.
     * <p>
     * This parameter is optional. If {@code null} Netex files are loaded from
     * {@link #baseDirectory()}.
     */
    @NotNull List<URI> netexFiles();

    /**
     * The URI to the graph object file to load and/or save.
     */
    URI graph();

    /**
     * The URI to the street graph object file to load and/or save.
     */
    URI streetGraph();

    /**
     * Patterns for matching GTFS zip-files or directories. If the filename contains the
     * given pattern it is considered a match. Any legal Java Regular expression is allowed.
     * <p>
     * @see #DEFAULT_GTFS_PATTERN for default value.
     */
    Pattern gtfsLocalFilePattern();

    /**
     * Patterns for matching NeTEx zip files or directories. If the filename contains the
     * given pattern it is considered a match. Any legal Java Regular expression is allowed.
     * <p>
     * @see #DEFAULT_NETEX_PATTERN for default value.
     */
    Pattern netexLocalFilePattern();

    /**
     * Pattern for matching Open Street Map input files. If the filename contains the
     * given pattern it is considered a match. Any legal Java Regular expression is allowed.
     * <p>
     * @see #DEFAULT_OSM_PATTERN for default value.
     */
    Pattern osmLocalFilePattern();

    /**
     * Pattern for matching elevation DEM files. If the filename contains the
     * given pattern it is considered a match. Any legal Java Regular expression is allowed.
     * <p>
     * @see #DEFAULT_DEM_PATTERN for default value.
     */
    Pattern demLocalFilePattern();
}
