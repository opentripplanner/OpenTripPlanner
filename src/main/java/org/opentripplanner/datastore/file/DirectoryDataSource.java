package org.opentripplanner.datastore.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * This is a adapter to wrap a file directory and all files in it as a composite data source.
 * Sub-directories are ignored.
 */
public class DirectoryDataSource extends AbstractFileDataSource implements CompositeDataSource {

  public DirectoryDataSource(File path, FileType type) {
    super(path, type);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public Collection<DataSource> content() {
    Collection<DataSource> content = new ArrayList<>();
    if (file.exists()) {
      for (File file : file.listFiles()) {
        // Skip any nested directories
        if (file.isDirectory()) {
          continue;
        }
        // In general the file type at a child level is not used, but we tag
        // each file with the same type as the parent directory.
        // Examples: GTFS or NETEX.
        content.add(new FileDataSource(file, type));
      }
    }
    return content;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public DataSource entry(String filename) {
    if (!file.exists()) {
      file.mkdirs();
    }
    return new FileDataSource(new File(file, filename), type);
  }

  @Override
  public void delete() {
    if (file.exists()) {
      try (var files = Files.walk(file.toPath())) {
        // Need to reverse stream to delete nested files before parent directory
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      } catch (IOException e) {
        throw new RuntimeException(
          "Failed to delete " + path() + ": " + e.getLocalizedMessage(),
          e
        );
      }
    }
  }

  @Override
  public void close() {
    /* Nothing to close */
  }
}
