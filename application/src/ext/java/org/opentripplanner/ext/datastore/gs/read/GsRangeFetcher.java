package org.opentripplanner.ext.datastore.gs.read;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.opentripplanner.framework.retry.OtpRetry;
import org.opentripplanner.framework.retry.OtpRetryBuilder;
import org.opentripplanner.framework.retry.OtpRetryException;

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

  /** HTTP status codes that will never succeed on retry. */
  private static final Set<Integer> PERMANENT = Set.of(400, 401, 403, 404, 412);

  /** Attempts to recover a range before the read fails; the client retries below this too. */
  private static final int RETRY_MAX_ATTEMPTS = 3;

  /**
   * The client's own read buffer. It must be small. The range is fixed by seek/limit and arrives
   * as one HTTP request whatever the chunk size, but as soon as the destination has less room left
   * than one chunk the client stops reading straight into it and allocates a heap buffer of a full
   * chunk to read through instead. A chunk the size of a block would therefore cost a heap
   * allocation of the whole block, and an extra copy, for every block.
   */
  private static final int CHUNK_SIZE = 256 * 1024;
  private static final Duration RETRY_INITIAL_DELAY = Duration.ofMillis(200);
  private static final int RETRY_BACKOFF = 2;

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

    // Built per range because the retry cycle must stop as soon as this particular read is
    // cancelled, rather than sleeping and reopening a channel that is about to be thrown away.
    OtpRetry retry = new OtpRetryBuilder()
      .withName("GCS range read of " + path)
      .withMaxAttempts(RETRY_MAX_ATTEMPTS)
      .withInitialRetryInterval(RETRY_INITIAL_DELAY)
      .withBackoffMultiplier(RETRY_BACKOFF)
      .withRetryableException(e -> !cancellation.isCancelled() && isRetryable(unwrap(e)))
      .withOnRetry(retries::incrementAndGet)
      .build();

    try {
      retry.execute(() -> readRange(end, dst, cancellation));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("Interrupted while reading " + path);
    } catch (OtpRetryException e) {
      throw asIOException(unwrap(e.getCause()), end - dst.remaining());
    }
    // Do not leave a closed channel registered for cancellation.
    cancellation.register(null);
  }

  /**
   * One attempt at the range. It resumes at the first byte not yet in {@code dst}, so a retry only
   * refetches what is still missing rather than the whole block.
   */
  private void readRange(long end, ByteBuffer dst, Cancellation cancellation) {
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
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      closeQuietly(channel);
    }
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
      channel.setChunkSize(CHUNK_SIZE);
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

  /** {@link OtpRetry} only sees unchecked exceptions, so the checked cause is wrapped. */
  private static Throwable unwrap(Throwable e) {
    return e instanceof UncheckedIOException wrapped ? wrapped.getCause() : e;
  }

  private static boolean isRetryable(Throwable e) {
    if (e instanceof StorageException se) {
      return !PERMANENT.contains(se.getCode()) && se.isRetryable();
    }
    // Plain I/O failures against the transport are transient by nature; a genuinely truncated
    // object is caught by the length check in ParallelRangeInputStream. Anything else is a bug,
    // and retrying a bug only delays the report.
    return e instanceof IOException && !(e instanceof EOFException);
  }

  private IOException asIOException(Throwable e, long position) {
    if (e instanceof IOException io) {
      return io;
    }
    return new IOException("Failed to read %s at %d".formatted(path, position), e);
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
