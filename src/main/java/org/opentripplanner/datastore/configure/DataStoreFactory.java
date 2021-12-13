package org.opentripplanner.datastore.configure;

import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.OtpDataStoreConfig;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.file.FileDataSourceRepository;
import org.opentripplanner.ext.datastore.gs.GsDataSourceRepository;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the global access point to create a data store and create datasource objects(tests). It
 * uses a build pattern to configure the data store before creating it.
 * <p>
 * Note that opening a data store should not download or open any data sources, only fetch meta-data
 * to figure out what data is available. A data source is accessed (lazy) using streams.
 * <p>
 * The only available data store is using the local file system to fetch data, but it is designed so
 * individual forks of OTP can provide their own implementation to fetch data from the cloud, mixed
 * with file access.
 * <p>
 * Implementation details. This class should contain minimal amount of business logic, delegating
 * all tasks to the underlying implementations.
 */
public class DataStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreFactory.class);

    private final OtpDataStoreConfig config;

    /**
     * @param config is used to create and configure the store.
     */
    public DataStoreFactory(OtpDataStoreConfig config) {
        this.config = config;
    }

    /* static factory methods, mostly used by tests */

    /**
     * For test only.
     * <p>
     * Use this to get a composite data source, bypassing the {@link OtpDataStore}.
     */
    public static CompositeDataSource compositeSource(File file, FileType type) {
        return FileDataSourceRepository.compositeSource(file, type);
    }

    /**
     * Connect to data source and prepare to retrieve data.
     */
    public OtpDataStore open() {
        List<DataSourceRepository> repositories = new ArrayList<>();

        // Adding Google Cloud Storage, if the config file contains URIs with prefix "gs:"
        if (OTPFeature.GoogleCloudStorage.isOn()) {
            LOG.info("Google Cloud Store Repository enabled - GS resources detected.");
            repositories.add(new GsDataSourceRepository(config.gsCredentials()));
        }
        // The file data storage repository should be last, to allow
        // other repositories to "override" and grab files analyzing the
        // datasource uri passed in
        repositories.add(
            new FileDataSourceRepository(
                config.baseDirectory(),
                config.gtfsLocalFilePattern(),
                config.netexLocalFilePattern(),
                config.osmLocalFilePattern(),
                config.demLocalFilePattern()
            )
        );

        OtpDataStore store = new OtpDataStore(config, repositories);

        store.open();
        return store;
    }
}
