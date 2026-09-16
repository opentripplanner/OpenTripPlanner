package org.opentripplanner.ext.datastore.gs.read;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches byte-ranges of a Google Cloud Storage blob. The only GCS-aware part of the parallel read
 * path.
 * <p>
 * Every range is opened on a <em>fresh</em> {@link ReadChannel}. That is deliberate: measurements
 * showed that slow reads are a sustained property of an individual connection rather than of the
 * node or the moment, so a worker that kept one channel for all of its blocks would let a single
 * bad connection poison its whole share of the object. Re-opening per block re-samples the
 * connection, and bounds the cost of a bad draw to one block.
 */
public class GsRangeFetcher implements RangeFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(GsRangeFetcher.class);

  /** HTTP status codes that will never succeed on retry. */
  private static final Set<Integer> PERMANENT = Set.of(400, 401, 403, 404, 412);

  private static final int MAX_RETRIES = Integer.getInteger("otp.gs.rangeReadRetries", 3);
  private static final long RETRY_BACKOFF_MS = Long.getLong("otp.gs.rangeReadBackoffMs", 200);

  private final Blob blob;
  private final String path;
  private final AtomicInteger retries = new AtomicInteger();

  public GsRangeFetcher(Blob blob, String path) {
    this.blob = blob;
    this.path = path;
  }

  @Override
  public void fetch(long offset, ByteBuffer dst, Cancellation cancellation) throws IOException {
    final long end = offset + dst.remaining();
    int attempt = 0;

    while (dst.hasRemaining()) {
      // Resume where the previous attempt stopped, so a failure costs only the bytes not yet
      // received rather than the whole block.
      long position = end - dst.remaining();
      ReadChannel channel = null;
      try {
        channel = open(position, end, cancellation);
        while (dst.hasRemaining()) {
          if (channel.read(dst) < 0) {
            throw new EOFException(
              "Unexpected end of %s at %d, wanted %d more bytes".formatted(
                path,
                end - dst.remaining(),
                dst.remaining()
              )
            );
          }
        }
      } catch (IOException | StorageException e) {
        if (cancellation.isCancelled()) {
          throw new IOException("Read cancelled: " + path, e);
        }
        if (++attempt > MAX_RETRIES || !isRetryable(e)) {
          throw asIOException(e, position);
        }
        retries.incrementAndGet();
        LOG.warn(
          "Retrying read of {} at {} (attempt {} of {}): {}",
          path,
          position,
          attempt,
          MAX_RETRIES,
          e.toString()
        );
        backOff(attempt);
      } finally {
        closeQuietly(channel);
      }
    }
    // Do not leave a closed channel registered for cancellation.
    cancellation.register(null);
  }

  @Override
  public String statsSummary() {
    int n = retries.get();
    return n == 0 ? "" : " " + n + " retries.";
  }

  /* private methods */

  private ReadChannel open(long position, long end, Cancellation cancellation) throws IOException {
    // generationMatch pins every range to the generation already in the fetched metadata (so it
    // costs no extra request). Without it, an object replaced mid-read would be silently spliced
    // together from two generations instead of failing.
    ReadChannel channel = blob.reader(Blob.BlobSourceOption.generationMatch());
    boolean opened = false;
    try {
      channel.seek(position);
      channel.limit(end);
      channel.setChunkSize((int) Math.min(end - position, Integer.MAX_VALUE));
      if (!cancellation.register(channel)) {
        throw new IOException("Read cancelled: " + path);
      }
      opened = true;
      return channel;
    } finally {
      if (!opened) {
        closeQuietly(channel);
      }
    }
  }

  private static boolean isRetryable(Exception e) {
    if (e instanceof StorageException se) {
      return !PERMANENT.contains(se.getCode()) && se.isRetryable();
    }
    // Plain I/O failures against the transport are transient by nature; a genuinely truncated
    // object is caught by the length check in ParallelRangeInputStream.
    return !(e instanceof EOFException);
  }

  private IOException asIOException(Exception e, long position) {
    if (e instanceof IOException io) {
      return io;
    }
    return new IOException("Failed to read %s at %d".formatted(path, position), e);
  }

  private static void backOff(int attempt) throws IOException {
    try {
      Thread.sleep(RETRY_BACKOFF_MS * attempt);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while retrying", ie);
    }
  }

  private static void closeQuietly(@Nullable Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ignored) {
        // Nothing useful to do; the range is either complete or already failing.
      }
    }
  }
}
