package org.opentripplanner.standalone.configure;

import org.opentripplanner.datastore.OtpDataStoreConfig;
import org.opentripplanner.standalone.config.StorageConfig;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is a thin wrapper around the OTP configuration to provide a
 * mapping into the storage domain {@link org.opentripplanner.datastore}.
 * Every parameter the data-store needs is passed into it using this adapter.
 * <p/>
 * It allows decoupling the application configuration from the data storage
 * implementation, and at the same time show the mapping from the app config
 * into the data storage in ONE place.
 */
class OtpDataStoreConfigAdapter implements OtpDataStoreConfig {
    private final File baseDirectory;
    private final StorageConfig config;

    OtpDataStoreConfigAdapter(
            File baseDirectory, StorageConfig config
    ) {
        this.baseDirectory = baseDirectory;
        this.config = config;
    }

    @Override
    public File baseDirectory() {
        return baseDirectory;
    }

    @Override
    public URI reportDirectory() {
        return config.buildReportDir;
    }

    @Override
    public String gsCredentials() {
        return config.gsCredentials;
    }

    @Override
    public List<URI> osmFiles() {
        return config.osm;
    }

    @Override
    public List<URI> demFiles() {
        return config.dem;
    }

    @Override
    public List<URI> gtfsFiles() {
        return config.gtfs;
    }

    @Override
    public List<URI> netexFiles() {
        return config.netex;
    }

    @Override
    public URI graph() {
        return config.graph;
    }

    @Override
    public URI streetGraph() {
        return config.streetGraph;
    }

    @Override
    public Pattern gtfsLocalFilePattern() {
        return config.localFileNamePatterns.gtfs;
    }

    @Override
    public Pattern netexLocalFilePattern() {
        return config.localFileNamePatterns.netex;
    }

    @Override
    public Pattern osmLocalFilePattern() {
        return config.localFileNamePatterns.osm;
    }

    @Override
    public Pattern demLocalFilePattern() {
        return config.localFileNamePatterns.dem;
    }
}
