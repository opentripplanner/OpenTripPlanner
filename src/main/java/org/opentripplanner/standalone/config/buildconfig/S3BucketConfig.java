package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds credentials and a bucket ID for downloading things from an Amazon S3 bucket. These
 * configuration options can be loaded from JSON using the fromJson method.
 */
public class S3BucketConfig {

  private static final Logger LOG = LoggerFactory.getLogger(S3BucketConfig.class);

  public String accessKey;
  public String secretKey;
  public String bucketName;

  public static S3BucketConfig fromConfig(NodeAdapter root, String elevationBucketName) {
    return fromConfig(
      root
        .of(elevationBucketName)
        .since(NA)
        .summary("If specified, download NED elevation tiles from the given AWS S3 bucket")
        .asObject()
    );
  }

  /** Create a BucketConfig from a JSON configuration node. */
  public static S3BucketConfig fromConfig(NodeAdapter config) {
    if (config.isEmpty()) {
      /* No configuration was specified, nothing should be downloaded from S3. */
      return null;
    }
    S3BucketConfig bucketConfig = new S3BucketConfig();
    try {
      bucketConfig.accessKey =
        config
          .of("accessKey")
          .since(NA)
          .summary("Credentials: the Amazon Web Services access key")
          .asString();
      bucketConfig.secretKey =
        config
          .of("secretKey")
          .since(NA)
          .summary(
            "Credentials: the Amazon Web Services secret key corresponding to the access key."
          )
          .asString();
      bucketConfig.bucketName =
        config
          .of("bucketName")
          .since(NA)
          .summary("The bucket from which you want to download.")
          .asString();
    } catch (OtpAppException ex) {
      LOG.error(
        "You must specify an accessKey, a secretKey, and a bucketName when " +
        "configuring S3 download. " +
        ex.getMessage()
      );
      throw ex;
    }
    return bucketConfig;
  }

  public String toString() {
    return (
      "[AWS S3 bucket configuration: bucketName=" +
      bucketName +
      " accessKey=" +
      accessKey +
      " secretKey=***]"
    );
  }
}
