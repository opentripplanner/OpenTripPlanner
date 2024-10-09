package org.opentripplanner.datastore.file;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * Abstract DataSource which wraps a {@link File}.
 */
public abstract class AbstractFileDataSource implements DataSource {

  final File file;
  final FileType type;

  /**
   * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files as
   * well as normal files. It does not handle directories({@link DirectoryDataSource}) or zip-files
   * {@link ZipFileDataSource} which contain multiple files.
   */
  AbstractFileDataSource(File file, FileType type) {
    this.file = file;
    this.type = type;
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public final String path() {
    return file.getPath();
  }

  @Override
  public URI uri() {
    return file.toURI();
  }

  @Override
  public final FileType type() {
    return type;
  }

  @Override
  public final long size() {
    // file.length() may return 0, map this to unknown
    long value = file.length();
    return value != 0L ? value : DataSource.UNKNOWN;
  }

  @Override
  public final long lastModified() {
    // file.lastModified() may return 0, map this to unknown
    long value = file.lastModified();
    return value != 0L ? value : DataSource.UNKNOWN;
  }

  @Override
  public final boolean exists() {
    return file.exists() && file.canRead();
  }

  @Override
  public boolean isWritable() {
    // We assume we can write to a file if the parent directory exist, and if the
    // file exist then it must be writable. If the file do not exist we assume we
    // can create a new file and write to it - there is no check on this.
    return file.getParentFile().exists() && (!file.exists() || file.canWrite());
  }

  @Override
  public int hashCode() {
    return Objects.hash(file, type);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractFileDataSource that = (AbstractFileDataSource) o;
    return file.equals(that.file) && type == that.type;
  }

  @Override
  public String toString() {
    return type + " " + path();
  }
}
