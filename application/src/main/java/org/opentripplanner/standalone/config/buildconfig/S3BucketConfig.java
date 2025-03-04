package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
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
        .summary("Used to download NED elevation tiles from the given AWS S3 bucket.")
        .description(
          """
          In the United States, a high resolution [National Elevation Dataset](http://ned.usgs.gov/) is
          available for the entire territory. It used to be possible for OTP to download NED tiles on the fly
          from a rather complex USGS SOAP service. This process was somewhat unreliable and would greatly slow
          down the graph building process. In any case the service has since been replaced. But the USGS would
          also deliver the whole dataset in bulk if you
          [sent them a hard drive](https://web.archive.org/web/20150811051917/http://ned.usgs.gov:80/faq.html#DATA).
          We did this many years back and uploaded the entire data set to Amazon AWS S3. OpenTripPlanner
          contains another module that can automatically fetch data in this format from any Amazon S3 copy of
          the bulk data.

          This `ned13` bucket is still available on S3 under a "requester pays" policy. As long as you specify
          valid AWS account credentials you should be able to download tiles, and any bandwidth costs will be
          billed to your AWS account.

          Once the tiles are downloaded for a particular geographic area, OTP will keep them in local cache
          for the next graph build operation. You should add the `--cache <directory>` command line parameter
          to specify your NED tile cache location.
          """
        )
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
      bucketConfig.accessKey = config
        .of("accessKey")
        .since(NA)
        .summary("Credentials: the Amazon Web Services access key")
        .asString();
      bucketConfig.secretKey = config
        .of("secretKey")
        .since(NA)
        .summary("Credentials: the Amazon Web Services secret key corresponding to the access key.")
        .asString();
      bucketConfig.bucketName = config
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
