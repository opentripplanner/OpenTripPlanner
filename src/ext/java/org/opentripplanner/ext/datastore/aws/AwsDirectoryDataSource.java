package org.opentripplanner.ext.datastore.aws;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * This is a an adapter to to simulate a file directory on a GCS. Files created using an instance of
 * this class wil have a common namespace. It does only support creating new output sources, it can
 * not be used to list files with the common namespace (directory path).
 */
public class AwsDirectoryDataSource extends AbstractAwsDataSource implements CompositeDataSource {

  AwsDirectoryDataSource(S3Client s3Client, S3Object object, FileType type) {
    super(s3Client, object, type);
  }

  @Override
  public boolean exists() {
    // TODO
    return super.exists();
  }

  @Override
  public Collection<DataSource> content() {
    // TODO
    return List.of();
  }

  @Override
  public DataSource entry(String name) {
    S3Object child = object().child(name);

    // TODO
    boolean exists = true;
    // If file exist
    if (exists) {
      return new AwsFileDataSource(s3Client(), child, type());
    }
    // New file
    return new AwsOutFileDataSource(s3Client(), child, type());
  }

  @Override
  public void delete() {
    // TODO
  }

  @Override
  public void close() {}
}
