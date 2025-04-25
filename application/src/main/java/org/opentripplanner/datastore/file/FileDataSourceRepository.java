package org.opentripplanner.datastore.file;

import static org.opentripplanner.datastore.OtpDataStore.BUILD_REPORT_DIR;
import static org.opentripplanner.datastore.api.FileType.CONFIG;
import static org.opentripplanner.datastore.api.FileType.DEM;
import static org.opentripplanner.datastore.api.FileType.EMISSION;
import static org.opentripplanner.datastore.api.FileType.GRAPH;
import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;
import static org.opentripplanner.datastore.api.FileType.REPORT;
import static org.opentripplanner.datastore.api.FileType.UNKNOWN;
import static org.opentripplanner.datastore.base.LocalDataSourceRepository.isCurrentDir;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.LocalDataSourceRepository;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.application.OtpFileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class FileDataSourceRepository implements LocalDataSourceRepository {

  private static final Logger LOG = LoggerFactory.getLogger(FileDataSourceRepository.class);

  private final Pattern GRAPH_PATTERN = Pattern.compile("(?i)(street)?graph.*\\.obj");
  private final Pattern EMISSION_PATTERN = Pattern.compile("(?i)(emission).*\\.(txt|csv)");

  private final File baseDir;
  private final Pattern gtfsLocalFilePattern;
  private final Pattern netexLocalFilePattern;
  private final Pattern osmLocalFilePattern;
  private final Pattern demLocalFilePattern;

  public FileDataSourceRepository(
    File baseDir,
    Pattern gtfsLocalFilePattern,
    Pattern netexLocalFilePattern,
    Pattern osmLocalFilePattern,
    Pattern demLocalFilePattern
  ) {
    this.baseDir = baseDir;
    this.gtfsLocalFilePattern = gtfsLocalFilePattern;
    this.netexLocalFilePattern = netexLocalFilePattern;
    this.osmLocalFilePattern = osmLocalFilePattern;
    this.demLocalFilePattern = demLocalFilePattern;
  }

  /**
   * Use for unit testing
   */
  public static CompositeDataSource compositeSource(File file, FileType type) {
    // The cast is safe
    return createCompositeSource(file, type);
  }

  @Override
  public String description() {
    return baseDir.getPath();
  }

  @Override
  public void open() {
    /* Nothing to do */
  }

  @Override
  public DataSource findSource(URI uri, FileType type) {
    return new FileDataSource(openFile(uri, type), type);
  }

  @Override
  public CompositeDataSource findCompositeSource(URI uri, FileType type) {
    return createCompositeSource(openFile(uri, type), type);
  }

  @Override
  public DataSource findSource(String filename, FileType type) {
    return new FileDataSource(new File(baseDir, filename), type);
  }

  @Override
  public CompositeDataSource findCompositeSource(String localFilename, FileType type) {
    // If the local file name is '.' then use the 'baseDir', if not create a new file directory.
    File file = isCurrentDir(localFilename) ? baseDir : new File(baseDir, localFilename);
    return createCompositeSource(file, type);
  }

  @Override
  public List<DataSource> listExistingSources(FileType type) {
    // Return ALL resources of the given type, this is
    // auto-detecting matching files on the local file system
    List<DataSource> existingFiles = new ArrayList<>();
    File[] files = baseDir.listFiles();

    if (files == null) {
      LOG.error("'{}' is not a readable input directory.", baseDir);
      return existingFiles;
    }

    for (File file : files) {
      if (type == resolveFileType(file)) {
        if (isCompositeDataSource(file)) {
          existingFiles.add(createCompositeSource(file, type));
        } else {
          existingFiles.add(new FileDataSource(file, type));
        }
      }
    }
    return existingFiles;
  }

  @Override
  public String toString() {
    return "FileDataSourceRepository{" + "baseDir=" + baseDir + '}';
  }

  /* private methods */

  public File openFile(URI uri, FileType type) {
    try {
      return uri.isAbsolute() ? new File(uri) : new File(baseDir, uri.getPath());
    } catch (IllegalArgumentException e) {
      throw new OtpAppException(
        "The file URI is invalid for file type " +
        type +
        ". " +
        "URI: '" +
        uri +
        "', details: " +
        e.getMessage()
      );
    }
  }

  private static CompositeDataSource createCompositeSource(File file, FileType type) {
    if (file.exists() && file.isDirectory()) {
      return new DirectoryDataSource(file, type);
    }
    if (file.getName().endsWith(".zip")) {
      return new ZipFileDataSource(file, type);
    }
    // If writing to a none-existing directory
    if (!file.exists() && type.isOutputDataSource()) {
      return new DirectoryDataSource(file, type);
    }
    throw new IllegalArgumentException(
      "The " +
      file +
      " is not recognized as a zip-file or " +
      "directory. Unable to create composite data source for file type " +
      type +
      "."
    );
  }

  private static boolean isTransitFile(File file, Pattern pattern) {
    return (
      pattern.matcher(file.getName()).find() &&
      (file.isDirectory() || file.getName().endsWith(".zip"))
    );
  }

  private boolean isCompositeDataSource(File file) {
    return file.isDirectory() || file.getName().endsWith(".zip");
  }

  private FileType resolveFileType(File file) {
    String name = file.getName();
    if (isTransitFile(file, gtfsLocalFilePattern)) {
      return GTFS;
    }
    if (isTransitFile(file, netexLocalFilePattern)) {
      return NETEX;
    }
    if (osmLocalFilePattern.matcher(name).find()) {
      return OSM;
    }
    // Digital elevation model (elevation raster)
    if (demLocalFilePattern.matcher(name).find()) {
      return DEM;
    }
    if (EMISSION_PATTERN.matcher(name).find()) {
      return EMISSION;
    }
    if (GRAPH_PATTERN.matcher(name).find()) {
      return GRAPH;
    }
    if (name.equals(BUILD_REPORT_DIR)) {
      return REPORT;
    }
    if (OtpFileNames.isConfigFile(name)) {
      return CONFIG;
    }
    return UNKNOWN;
  }
}
