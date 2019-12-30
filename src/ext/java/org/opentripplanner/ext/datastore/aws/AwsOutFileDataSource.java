package org.opentripplanner.ext.datastore.aws;

import java.io.OutputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import software.amazon.awssdk.services.s3.S3Client;

class AwsOutFileDataSource extends AbstractAwsDataSource implements DataSource {

  /**
   * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files as
   * well as normal files. It does not handle directories({@link DirectoryDataSource}) or zip-files
   * {@link ZipFileDataSource} which contain multiple files.
   */
  AwsOutFileDataSource(S3Client s3Client, S3Object object, FileType type) {
    super(s3Client, object, type);
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public OutputStream asOutputStream() {
    return new AwsOutputStream(s3Client(), object()).open();
  }
}
