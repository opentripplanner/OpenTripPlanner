package org.opentripplanner.standalone.config;

import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds credentials and a bucket ID for downloading things from an Amazon S3 bucket.
 * These configuration options can be loaded from JSON using the fromJson method.
 */
public class S3BucketConfig {

    private static Logger LOG = LoggerFactory.getLogger(S3BucketConfig.class);

    /** Credentials: the Amazon Web Services access key */
    public String accessKey;

    /** Credentials: the Amazon Web Services secret key corresponding to the access key. */
    public String secretKey;

    /** The bucket from which you want to download. */
    public String bucketName;

    /** Create a BucketConfig from a JSON configuration node. */
    public static S3BucketConfig fromConfig(NodeAdapter config) {
        if (config.isEmpty()) {
            /* No configuration was specified, nothing should be downloaded from S3. */
            return null;
        }
        S3BucketConfig bucketConfig = new S3BucketConfig();
        try {
            bucketConfig.accessKey = config.asText("accessKey");
            bucketConfig.secretKey = config.asText("secretKey");
            bucketConfig.bucketName = config.asText("bucketName");
        } catch (OtpAppException ex) {
            LOG.error(
                    "You must specify an accessKey, a secretKey, and a bucketName when " +
                    "configuring S3 download. " + ex.getMessage()
            );
            throw ex;
        }
        return bucketConfig;
    }

    public String toString() {
        return "[AWS S3 bucket configuration: bucketName=" + bucketName + " accessKey=" + accessKey + " secretKey=***]";
    }
}
