package org.opentripplanner.datastore.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.api.GoogleStorageDSRepository;
import org.opentripplanner.datastore.api.OtpBaseDirectory;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.datastore.base.ByteArrayDataSource;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.base.ListCompositeDataSource;
import org.opentripplanner.datastore.file.FileDataSourceRepository;
import org.opentripplanner.datastore.https.HttpsDataSourceRepository;

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
@Module
public abstract class DataStoreModule {

  /**
   * For test only.
   * <p>
   * Use this to get a composite data source, bypassing the {@link OtpDataStore}.
   */
  public static CompositeDataSource compositeSource(File file, FileType type) {
    return FileDataSourceRepository.compositeSource(file, type);
  }

  /**
   * For test only.
   * <p>
   * Use this to get a composite data source. Pass in all child data sources.
   */
  public static CompositeDataSource compositeSource(
    String name,
    FileType type,
    DataSource... children
  ) {
    return new ListCompositeDataSource(name, type, Arrays.asList(children));
  }

  /**
   * For test only.
   * <p>
   * Use this to create a read-only data source backed by the given {@code content} string.
   */
  public static DataSource dataSource(String name, FileType type, String content) {
    var buf = content.getBytes(StandardCharsets.UTF_8);
    return new ByteArrayDataSource(
      name,
      name,
      type,
      buf.length,
      System.currentTimeMillis(),
      false
    ).withBytes(buf);
  }

  /**
   * Connect to data source and prepare to retrieve data.
   */
  @Provides
  @Singleton
  public static OtpDataStore provideDataStore(
    @OtpBaseDirectory File baseDirectory,
    OtpDataStoreConfig config,
    @Nullable @GoogleStorageDSRepository DataSourceRepository gsRepository
  ) {
    List<DataSourceRepository> repositories = new ArrayList<>();

    if (gsRepository != null) {
      repositories.add(gsRepository);
    }

    repositories.add(new HttpsDataSourceRepository());

    // The file data storage repository should be last, to allow
    // other repositories to "override" and grab files analyzing the
    // datasource uri passed in
    repositories.add(createFileDataSourceRepository(baseDirectory, config));

    var dataStore = new OtpDataStore(config, repositories);

    // It might not be "best-practice" to open files during application construction,
    // but delegating this to the client(potentially more than one) is a bit messy as well.
    dataStore.open();

    return dataStore;
  }

  private static FileDataSourceRepository createFileDataSourceRepository(
    File baseDirectory,
    OtpDataStoreConfig config
  ) {
    return new FileDataSourceRepository(
      baseDirectory,
      config.gtfsLocalFilePattern(),
      config.netexLocalFilePattern(),
      config.osmLocalFilePattern(),
      config.demLocalFilePattern()
    );
  }
}
