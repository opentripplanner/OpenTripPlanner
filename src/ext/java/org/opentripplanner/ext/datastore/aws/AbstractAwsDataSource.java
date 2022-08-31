package org.opentripplanner.ext.datastore.aws;

import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import software.amazon.awssdk.services.s3.S3Client;

abstract class AbstractAwsDataSource implements DataSource {

  private final S3Client s3Client;

  private final S3Object object;
  private final FileType type;

  AbstractAwsDataSource(S3Client s3Client, S3Object object, FileType type) {
    this.s3Client = s3Client;
    this.object = object;
    this.type = type;
  }

  @Override
  public final String name() {
    return object.name();
  }

  @Override
  public final String path() {
    return object.toUriString();
  }

  @Override
  public final FileType type() {
    return type;
  }

  @Override
  public final String toString() {
    return type + " " + path();
  }

  S3Client s3Client() {
    return s3Client;
  }

  S3Object object() {
    return object;
  }

  String bucketName() {
    return object.bucket();
  }
}
