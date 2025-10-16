package org.opentripplanner.datastore.api;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.base.ByteArrayDataSource;
import org.opentripplanner.datastore.file.FileDataSourceRepository;

public class DataSourceTestFactory {

  /**
   * Use this to get a composite data source, bypassing the {@link OtpDataStore}.
   */
  public static CompositeDataSource compositeSource(File file, FileType type) {
    return FileDataSourceRepository.compositeSource(file, type);
  }

  /**
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
}
