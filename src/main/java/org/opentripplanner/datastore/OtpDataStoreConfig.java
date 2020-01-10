package org.opentripplanner.datastore;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.util.List;


/**
 * The {@link OtpDataStore} config, needed to create a store. This should be provided by the
 * OTP application.
 */
public interface OtpDataStoreConfig {

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
}
