package org.opentripplanner.ext.datastore.gs.read;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import javax.annotation.Nullable;

/**
 * A {@link RangeFetcher} over a byte array, so the parallel read logic can be tested without Google
 * Cloud Storage. Latency and failures are programmable per range offset, and it records enough to
 * assert on concurrency and back-pressure.
 */
class InMemoryRangeFetcher implements RangeFetcher {

  private final byte[] data;
  private final AtomicInteger inFlight = new AtomicInteger();
  private final AtomicInteger maxInFlight = new AtomicInteger();
  private final Map<Long, Integer> fetchCount = new ConcurrentHashMap<>();

  /** Delay in milliseconds before a range is served, by offset. */
  volatile LongFunction<Long> delayMillis = offset -> 0L;

  /** Failure to raise instead of serving a range, by offset. */
  volatile LongFunction<Throwable> failure = offset -> null;

  /** Invoked just before a range is served, for barriers and latches. */
  volatile RangeHook beforeServe = offset -> {};

  InMemoryRangeFetcher(byte[] data) {
    this.data = data;
  }

  @Override
  public void fetch(long offset, ByteBuffer dst, Cancellation cancellation) throws IOException {
    maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
    try {
      fetchCount.merge(offset, 1, Integer::sum);
      beforeServe.accept(offset);

      long delay = delayMillis.apply(offset);
      if (delay > 0) {
        Thread.sleep(delay);
      }
      Throwable error = failure.apply(offset);
      if (error != null) {
        throwUnchecked(error);
      }
      int length = dst.remaining();
      dst.put(data, (int) offset, length);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted", e);
    } finally {
      inFlight.decrementAndGet();
    }
  }

  int maxInFlight() {
    return maxInFlight.get();
  }

  int fetchCount(long offset) {
    return fetchCount.getOrDefault(offset, 0);
  }

  int rangesStarted() {
    return fetchCount.size();
  }

  private static void throwUnchecked(Throwable error) throws IOException {
    switch (error) {
      case IOException io -> throw io;
      case RuntimeException re -> throw re;
      case Error err -> throw err;
      default -> throw new IOException(error);
    }
  }

  @FunctionalInterface
  interface RangeHook {
    void accept(long offset) throws InterruptedException;
  }

  /** Convenience for the common "fail at exactly this offset" case. */
  static LongFunction<Throwable> failAt(long offset, @Nullable Throwable error) {
    return o -> o == offset ? error : null;
  }
}
