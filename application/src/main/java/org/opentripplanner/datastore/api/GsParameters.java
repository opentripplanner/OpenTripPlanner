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

  /** @see #parallelReads() */
  int DEFAULT_PARALLEL_READS = 4;
  /** @see #parallelReadBlockSizeMb() */
  int DEFAULT_PARALLEL_READ_BLOCK_SIZE_MB = 16;
  /** @see #parallelReadMinSizeMb() */
  int DEFAULT_PARALLEL_READ_MIN_SIZE_MB = 32;

  /**
   * How many byte-ranges of one object to download concurrently.
   * <p>
   * A single Cloud Storage connection tops out well below what the network can deliver, and its
   * throughput varies a lot from connection to connection, so a large object read over one stream
   * has a long tail. Fetching several ranges at once removes that tail.
   * <p>
   * {@code 0} disables the feature and restores a single sequential stream; {@code 1} keeps one
   * stream but moves it to a background thread so the download overlaps the caller's work.
   */
  default int parallelReads() {
    return DEFAULT_PARALLEL_READS;
  }

  /** The size of each range request, in megabytes. */
  default int parallelReadBlockSizeMb() {
    return DEFAULT_PARALLEL_READ_BLOCK_SIZE_MB;
  }

  /** Objects smaller than this are read with a single sequential stream and no extra threads. */
  default int parallelReadMinSizeMb() {
    return DEFAULT_PARALLEL_READ_MIN_SIZE_MB;
  }

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
