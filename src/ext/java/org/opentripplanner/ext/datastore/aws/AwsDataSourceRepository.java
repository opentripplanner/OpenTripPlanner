package org.opentripplanner.ext.datastore.aws;

import static org.opentripplanner.ext.datastore.aws.S3Object.S3_SCHEMA_PREFIX;

import java.net.URI;
import javax.annotation.Nonnull;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.base.ZipStreamDataSourceDecorator;
import org.opentripplanner.util.lang.StringUtils;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class AwsDataSourceRepository implements DataSourceRepository {

  private static final String DEFAULT_CREDENTIALS_PROFILE = "DEFAULT";

  private final Region region;

  private final String credentialsProfile;
  private S3Client s3Client;

  public AwsDataSourceRepository(String region, String credentialsProfile) {
    this.region = Region.of(region);
    this.credentialsProfile = credentialsProfile;
  }

  @Override
  public String description() {
    return "Google Cloud Storage";
  }

  @Override
  public void open() {
    this.s3Client = connectToStorage();
  }

  @Override
  public DataSource findSource(@Nonnull URI uri, @Nonnull FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    S3Object object = S3Object.toS3Object(uri);
    return createSource(object, type);
  }

  @Override
  public CompositeDataSource findCompositeSource(@Nonnull URI uri, @Nonnull FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    return createCompositeSource(S3Object.toS3Object(uri), type);
  }

  /* private methods */

  private static boolean skipUri(URI uri) {
    return !S3_SCHEMA_PREFIX.equals(uri.getScheme());
  }

  private DataSource createSource(S3Object object, FileType type) {
    /* TODO Check if object exist */
    boolean exist = true;

    if (exist) {
      return new AwsFileDataSource(s3Client, object, type);
    } else {
      return new AwsOutFileDataSource(s3Client, object, type);
    }
  }

  private CompositeDataSource createCompositeSource(S3Object object, FileType type) {
    if (object.isRoot()) {
      return new AwsDirectoryDataSource(s3Client, object, type);
    }

    if (object.name().endsWith(".zip")) {
      /* TODO */
      boolean exist = true;

      if (exist) {
        throw new IllegalArgumentException(type.text() + " not found: " + object.toUriString());
      }
      DataSource gsSource = new AwsFileDataSource(s3Client, object, type);
      return new ZipStreamDataSourceDecorator(gsSource);
    }
    return new AwsDirectoryDataSource(s3Client, object, type);
  }

  private S3Client connectToStorage() {
    var builder = S3Client.builder().region(region);

    if (StringUtils.hasValue(credentialsProfile)) {
      if (DEFAULT_CREDENTIALS_PROFILE.equals(credentialsProfile)) {
        builder.credentialsProvider(ProfileCredentialsProvider.create());
      } else {
        builder.credentialsProvider(ProfileCredentialsProvider.create(credentialsProfile));
      }
    }
    return builder.build();
  }
}
