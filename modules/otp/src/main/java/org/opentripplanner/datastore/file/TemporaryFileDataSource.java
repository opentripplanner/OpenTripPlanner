package org.opentripplanner.datastore.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.opentripplanner.datastore.api.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemporaryFileDataSource extends FileDataSource {

  private static final Logger LOG = LoggerFactory.getLogger(TemporaryFileDataSource.class);
  private final String originalName;

  /**
   * Create a data source wrapper around a temporary file. This wrapper handles GZIP(.gz) compressed
   * files as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
   * zip-files {@link ZipFileDataSource} which contain multiple files.
   */
  public TemporaryFileDataSource(String originalName, File file, FileType type) {
    super(file, type);
    this.originalName = originalName;
  }

  @Override
  public String name() {
    return originalName;
  }

  public void deleteFile() {
    try {
      Files.delete(file.toPath());
    } catch (IOException e) {
      LOG.warn(
        "Could not delete temporary file {} for temporary file datasource {}",
        file.getName(),
        name(),
        e
      );
    }
  }
}
