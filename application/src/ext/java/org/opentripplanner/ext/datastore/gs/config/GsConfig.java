package org.opentripplanner.ext.datastore.gs.config;

import org.opentripplanner.datastore.api.GsParameters;
import org.opentripplanner.ext.datastore.gs.read.ParallelReadParameters;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class GsConfig implements GsParameters {

  private final String host;
  private final String credentialFile;
  private final int parallelReads;
  private final int parallelReadBlockSizeMb;
  private final int parallelReadMinSizeMb;

  public GsConfig(String host, String credentialFile) {
    this(
      host,
      credentialFile,
      GsParameters.DEFAULT_PARALLEL_READS,
      GsParameters.DEFAULT_PARALLEL_READ_BLOCK_SIZE_MB,
      GsParameters.DEFAULT_PARALLEL_READ_MIN_SIZE_MB
    );
  }

  public GsConfig(
    String host,
    String credentialFile,
    int parallelReads,
    int parallelReadBlockSizeMb,
    int parallelReadMinSizeMb
  ) {
    this.host = host;
    this.credentialFile = credentialFile;
    this.parallelReads = parallelReads;
    this.parallelReadBlockSizeMb = parallelReadBlockSizeMb;
    this.parallelReadMinSizeMb = parallelReadMinSizeMb;
  }

  public static GsConfig fromConfig(NodeAdapter root, String parameterName) {
    NodeAdapter gsRoot = root
      .of(parameterName)
      .since(OtpVersion.V2_8)
      .summary("Configuration for Google Cloud Storage")
      .asObject();

    String host = gsRoot
      .of("cloudServiceHost")
      .since(OtpVersion.V2_8)
      .summary("Host of the Google Cloud Storage Server")
      .description(
        """
        Host of the Google Cloud Storage server. In case of a real GCS Bucket this parameter can be
        omitted. When the host differs from the usual GCS host, for example when emulating GCS in a
        docker container for testing purposes, the host has to be specified including the port.
        Eg: http://localhost:4443"""
      )
      .asString(null);

    String credentialFile = gsRoot
      .of("credentialFile")
      .since(OtpVersion.V2_8)
      .summary("Local file system path to Google Cloud Platform service accounts credentials file.")
      .description(
        """
        The credentials are used to access GCS URLs. When using GCS from outside of Google Cloud you
        need to provide a path the the service credentials. Environment variables in the path are
        resolved.

        This is a path to a file on the local file system, not an URI.
        """
      )
      .asString(null);

    int parallelReads = gsRoot
      .of("parallelReads")
      .since(OtpVersion.V2_11)
      .summary("Number of byte-ranges of a single object to download concurrently.")
      .description(
        """
        A single Cloud Storage connection tops out well below the available bandwidth, and its
        throughput varies substantially from one connection to the next. Reading a large object -
        typically `graph.obj` - over one stream therefore has a long tail, where an unlucky
        connection takes several times the median. Downloading several ranges at once removes that
        tail, and lets the transfer overlap the work of the consumer.

        Set to `0` to disable this and use a single sequential stream. Otherwise the value must be
        between 2 and 16: each range is fetched on its own connection, so `1` would issue the
        requests one after another and pay the setup cost of every one of them with no other
        request to hide it behind, which is slower than turning the feature off. It is rejected.

        Each concurrent read holds buffers off the Java heap. The memory used by one object read is
        at most `3 * parallelReads * parallelReadBlockSizeMb` megabytes - 192 MB with the default
        settings. That memory is outside the heap but inside the container memory limit, so reduce
        `parallelReads` or `parallelReadBlockSizeMb` if the process is close to that limit.
        """
      )
      .asInt(GsParameters.DEFAULT_PARALLEL_READS);

    int parallelReadBlockSizeMb = gsRoot
      .of("parallelReadBlockSizeMb")
      .since(OtpVersion.V2_11)
      .summary("The size of each range request in megabytes, when reading in parallel.")
      .description(
        """
        Smaller blocks spread a slow connection over a smaller share of the object, but cost one
        more request each. The default is a reasonable balance for objects of a few gigabytes.
        Must be between 4 and 256.
        """
      )
      .asInt(GsParameters.DEFAULT_PARALLEL_READ_BLOCK_SIZE_MB);

    int parallelReadMinSizeMb = gsRoot
      .of("parallelReadMinSizeMb")
      .since(OtpVersion.V2_11)
      .summary("Objects smaller than this are read with a single sequential stream.")
      .description(
        """
        Parallel reads pay for themselves only on large objects. Below this size OTP reads the
        object exactly as it did before, allocating no buffers and starting no threads.
        """
      )
      .asInt(GsParameters.DEFAULT_PARALLEL_READ_MIN_SIZE_MB);

    var config = new GsConfig(
      host,
      credentialFile,
      parallelReads,
      parallelReadBlockSizeMb,
      parallelReadMinSizeMb
    );
    validateParallelReads(gsRoot, config);
    return config;
  }

  /**
   * Fail while the configuration is being read, with the path of the offending block, instead of
   * later from inside the data store wiring with a bare exception.
   */
  private static void validateParallelReads(NodeAdapter gsRoot, GsConfig config) {
    try {
      ParallelReadParameters.of(config);
    } catch (IllegalArgumentException e) {
      throw new OtpAppException("Invalid '" + gsRoot.contextPath() + "': " + e.getMessage());
    }
  }

  @Override
  public String host() {
    return host;
  }

  @Override
  public String credentialFile() {
    return credentialFile;
  }

  @Override
  public int parallelReads() {
    return parallelReads;
  }

  @Override
  public int parallelReadBlockSizeMb() {
    return parallelReadBlockSizeMb;
  }

  @Override
  public int parallelReadMinSizeMb() {
    return parallelReadMinSizeMb;
  }
}
