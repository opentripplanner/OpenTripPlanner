package org.opentripplanner.ext.datastore.aws;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.opentripplanner.datastore.api.AmazonS3DSRepository;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public class AwsDataSourceModule {

  private static final Logger LOG = LoggerFactory.getLogger(AwsDataSourceModule.class);

  @Provides
  @Singleton
  @Nullable
  @AmazonS3DSRepository
  DataSourceRepository provideGoogleStorageDataSourceRepository(OtpDataStoreConfig c) {
    if (OTPFeature.AmazonAwsS3Storage.isOff()) {
      return null;
    }
    LOG.info("Amazon AWS S3 Storage Support enabled - S3 resources detected.");
    return new AwsDataSourceRepository(c.s3Region(), c.s3CredentialsProfile());
  }
}
