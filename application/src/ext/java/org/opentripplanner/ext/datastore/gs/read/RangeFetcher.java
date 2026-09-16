package org.opentripplanner.ext.datastore.gs.read;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/**
 * Fetches one byte-range of an object. This is the only seam between
 * {@link ParallelRangeInputStream} and the underlying transport, which keeps the stream itself
 * free of any Google Cloud Storage knowledge and testable against an in-memory source.
 */
@FunctionalInterface
public interface RangeFetcher {
  /**
   * Fill {@code dst} with exactly {@code dst.remaining()} bytes read from {@code offset}. The
   * buffer is <em>not</em> flipped by the implementation.
   * <p>
   * The implementation must register every resource it opens with {@code cancellation} so that a
   * concurrent {@link ParallelRangeInputStream#close()} can abort a read that is blocked on the
   * network. Interrupting the worker thread is not sufficient for that.
   */
  void fetch(long offset, ByteBuffer dst, Cancellation cancellation) throws IOException;

  /**
   * Transport-level counters to append to the one-line summary the stream logs when it closes,
   * formatted so it can be concatenated directly (leading space, or empty).
   */
  default String statsSummary() {
    return "";
  }

  /**
   * Lets a fetcher publish the resource that has to be closed to abort a blocked read, without
   * racing against {@link #cancel()}.
   */
  interface Cancellation {
    /**
     * Publish the currently open resource, replacing any previous one, which is closed. Returns
     * {@code false} if the read was already cancelled, in which case the caller owns
     * {@code closeable} and must abandon the fetch.
     */
    boolean register(@Nullable Closeable closeable);

    boolean isCancelled();
  }
}
