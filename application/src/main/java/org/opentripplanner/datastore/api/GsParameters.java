package org.opentripplanner.datastore.api;

import javax.annotation.Nullable;

public interface GsParameters {
  /**
   * Host of the Google Cloud Services, including the port.
   * <p>
   * Optional. May return {@code null}. If the host is not set, the connection to the Google Cloud
   * Platform is done via the default host {@code https://storage.googleapis.com:4443}.
   *
   */
  @Nullable
  String host();

  /**
   * Local file system path to Google Cloud Platform service accounts credentials file. The
   * credentials are used to access GCS urls. When using GCS from outside of the bucket cluster you
   * need to provide a path to the service credentials.
   * <p>
   * This is a path to a file on the local file system, not an URI.
   * <p>
   * Optional. May return {@code null}. If the credentials are not set, the connection to the Google
   * Cloud Platform is done without any authorization.
   */
  @Nullable
  String credentialFile();

  static GsParameters defaultValues() {
    return new GsParameters() {
      @Override
      public String host() {
        return null;
      }

      @Override
      public String credentialFile() {
        return null;
      }
    };
  }
}
