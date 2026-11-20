package org.opentripplanner.ext.datastore.gs.read;

import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.GsParameters;

/**
 * Configured sizing for parallel ranged reads, and the derivation of the effective plan for one
 * object.
 * <p>
 * The configured values are a ceiling, not a prescription: a small object gets fewer blocks and
 * fewer workers, because a bigger object needs the same aggregate bandwidth for longer rather than
 * more connections.
 */
public record ParallelReadParameters(
  int parallelism,
  int blockSizeBytes,
  long minParallelReadSizeBytes
) {
  private static final int MB = 1024 * 1024;

  /** Smallest block worth a separate request; below this the per-request overhead dominates. */
  static final int MIN_BLOCK_SIZE_BYTES = 4 * MB;

  /** The pool ceiling: a larger block could never be in flight at all. */
  static final int MAX_BLOCK_SIZE_BYTES = 256 * MB;

  static final int MAX_PARALLELISM = 16;

  /** Blocks in flight per worker: one draining, one waiting, one being fetched. */
  static final int IN_FLIGHT_PER_WORKER = 3;

  /**
   * Hard ceiling on the off-heap footprint of a single stream. Direct memory sits outside the heap
   * but inside the container memory limit, and {@code MaxDirectMemorySize} defaults to the maximum
   * heap size, so the JVM will not cap this for us - the cgroup would, by OOM-kill.
   */
  static final long MAX_POOL_BYTES = 256L * MB;

  public static final ParallelReadParameters DEFAULT = new ParallelReadParameters(
    4,
    16 * MB,
    32L * MB
  );

  public ParallelReadParameters {
    // 1 is rejected on purpose: every block opens its own channel, so a single worker serialises
    // the requests and pays each one's setup with nothing to hide it behind - slower than off.
    if (parallelism < 0 || parallelism == 1 || parallelism > MAX_PARALLELISM) {
      throw new IllegalArgumentException(
        "parallelReads must be 0 (off) or in [2, " + MAX_PARALLELISM + "], was " + parallelism
      );
    }
    if (
      parallelism > 0 &&
      (blockSizeBytes < MIN_BLOCK_SIZE_BYTES || blockSizeBytes > MAX_BLOCK_SIZE_BYTES)
    ) {
      throw new IllegalArgumentException(blockSizeError(blockSizeBytes / MB));
    }
    if (minParallelReadSizeBytes < 0) {
      throw new IllegalArgumentException("parallelReadMinSizeMb must not be negative");
    }
  }

  /**
   * @throws IllegalArgumentException if the parameters are out of range; the message names the
   * offending configuration key.
   */
  public static ParallelReadParameters of(GsParameters p) {
    // Checked in megabytes first: converting an absurd value to bytes would wrap around in int.
    int blockSizeMb = p.parallelReadBlockSizeMb();
    if (
      p.parallelReads() > 0 &&
      (blockSizeMb < MIN_BLOCK_SIZE_BYTES / MB || blockSizeMb > MAX_BLOCK_SIZE_BYTES / MB)
    ) {
      throw new IllegalArgumentException(blockSizeError(blockSizeMb));
    }
    return new ParallelReadParameters(
      p.parallelReads(),
      blockSizeMb * MB,
      (long) p.parallelReadMinSizeMb() * MB
    );
  }

  private static String blockSizeError(long blockSizeMb) {
    return (
      "parallelReadBlockSizeMb must be in [" +
      MIN_BLOCK_SIZE_BYTES / MB +
      ", " +
      MAX_BLOCK_SIZE_BYTES / MB +
      "], was " +
      blockSizeMb
    );
  }

  /**
   * The plan for an object of {@code size} bytes, or {@code null} when it should be read
   * sequentially - which is the case for a disabled configuration, an unknown size, every object
   * below the threshold, and an object that fits in a single block. A {@code null} plan allocates
   * nothing and starts no threads.
   */
  @Nullable
  public ReadPlan planFor(long size) {
    if (parallelism == 0 || size <= 0 || size < minParallelReadSizeBytes) {
      return null;
    }
    // Keep at least 4 blocks per worker so a slow connection is amortised over a small share of
    // the object and the fast workers can steal the rest.
    long targetBlock = Math.max(MIN_BLOCK_SIZE_BYTES, size / (4L * parallelism));
    int blockSize = (int) Math.min(Math.min(targetBlock, blockSizeBytes), size);

    int blockCount = (int) Math.min(Integer.MAX_VALUE, (size + blockSize - 1) / blockSize);
    if (blockCount < 2) {
      // One block is one request: a thread pool and a pool of buffers would be pure overhead.
      return null;
    }
    int workers = Math.max(1, Math.min(parallelism, blockCount));
    int inFlight = Math.min(IN_FLIGHT_PER_WORKER * workers, blockCount);

    // Degrade rather than risk the container memory limit: first shrink the window, then the
    // worker count, and give up on parallel reads entirely if even one worker will not fit.
    while ((long) inFlight * blockSize > MAX_POOL_BYTES && inFlight > workers) {
      --inFlight;
    }
    while ((long) inFlight * blockSize > MAX_POOL_BYTES && workers > 1) {
      --workers;
      inFlight = workers;
    }
    if ((long) inFlight * blockSize > MAX_POOL_BYTES) {
      return null;
    }
    return new ReadPlan(blockSize, blockCount, workers, inFlight);
  }

  /** The effective sizing for one object. */
  public record ReadPlan(int blockSize, int blockCount, int parallelism, int inFlight) {
    public long poolBytes() {
      return (long) inFlight * blockSize;
    }
  }
}
