package org.opentripplanner.ext.datastore.gs.read;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the parallel read logic against an in-memory source; no Google Cloud Storage is involved.
 * <p>
 * The cancellation and failure cases run under a preemptive timeout on purpose: the defects they
 * guard against manifest as a consumer parked forever, which would otherwise hang CI rather than
 * fail it.
 */
class ParallelRangeInputStreamTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(15);
  private static final int BLOCK = 1024;
  private static final String NAME = "test://bucket/object";

  /* Correctness */

  static Stream<Arguments> sizesAndParallelism() {
    return Stream.of(0, 1, BLOCK - 1, BLOCK, BLOCK + 1, (7 * BLOCK) / 2, 100 * BLOCK).flatMap(
      size -> Stream.of(1, 2, 4, 7).map(k -> Arguments.of(size, k))
    );
  }

  @ParameterizedTest(name = "{0} bytes, {1} workers")
  @MethodSource("sizesAndParallelism")
  void readsExactlyTheSourceBytes(int size, int parallelism) throws IOException {
    byte[] data = randomBytes(size);
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, parallelism)) {
      assertThat(in.readAllBytes()).isEqualTo(data);
    }
  }

  @Test
  void oneWorkerProducesTheSameBytesAsMany() throws IOException {
    byte[] data = randomBytes(20 * BLOCK);
    byte[] withOne;
    byte[] withFour;
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 1)) {
      withOne = in.readAllBytes();
    }
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 4)) {
      withFour = in.readAllBytes();
    }
    assertThat(withOne).isEqualTo(data);
    assertThat(withFour).isEqualTo(data);
  }

  @Test
  void singleByteReadsReturnUnsignedValuesAndThenEof() throws IOException {
    byte[] data = { 0, 1, (byte) 0x80, (byte) 0xFF };
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), 2, 2)) {
      assertThat(in.read()).isEqualTo(0);
      assertThat(in.read()).isEqualTo(1);
      assertThat(in.read()).isEqualTo(128);
      assertThat(in.read()).isEqualTo(255);
      assertThat(in.read()).isEqualTo(-1);
      assertThat(in.read()).isEqualTo(-1);
    }
  }

  @Test
  void bulkReadsSpanBlockBoundaries() throws IOException {
    byte[] data = randomBytes(10 * BLOCK + 13);
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 3)) {
      byte[] target = new byte[data.length];
      int offset = 0;
      // A read never crosses a block boundary, so this must loop.
      while (offset < target.length) {
        int n = in.read(target, offset, Math.min(777, target.length - offset));
        assertThat(n).isGreaterThan(0);
        offset += n;
      }
      assertThat(target).isEqualTo(data);
      assertThat(in.read()).isEqualTo(-1);
    }
  }

  @Test
  void skipCrossesBlocks() throws IOException {
    byte[] data = randomBytes(10 * BLOCK);
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 2)) {
      assertThat(in.skip(3 * BLOCK + 5)).isEqualTo(3 * BLOCK + 5);
      assertThat(in.read()).isEqualTo(data[3 * BLOCK + 5] & 0xFF);
    }
  }

  @Test
  void zeroLengthReadReturnsZeroEvenAtEndOfStream() throws IOException {
    byte[] data = randomBytes(BLOCK);
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 1)) {
      assertThat(in.read(new byte[4], 0, 0)).isEqualTo(0);
      assertThat(in.readAllBytes()).isEqualTo(data);
      assertThat(in.read(new byte[4], 0, 0)).isEqualTo(0);
    }
  }

  @Test
  void rejectsOutOfRangeArguments() throws IOException {
    byte[] data = randomBytes(BLOCK);
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 1)) {
      byte[] target = new byte[8];
      assertThrows(IndexOutOfBoundsException.class, () -> in.read(target, -1, 4));
      assertThrows(IndexOutOfBoundsException.class, () -> in.read(target, 0, 9));
      assertThrows(NullPointerException.class, () -> in.read(null, 0, 1));
    }
  }

  /* Behaviour */

  @Test
  void reassemblesBlocksThatCompleteOutOfOrder() throws IOException {
    byte[] data = randomBytes(8 * BLOCK);
    var fetcher = new InMemoryRangeFetcher(data);
    // Later blocks finish first.
    fetcher.delayMillis = offset -> 100L - (offset / BLOCK) * 10L;

    try (InputStream in = open(data, fetcher, BLOCK, 8)) {
      assertThat(in.readAllBytes()).isEqualTo(data);
    }
  }

  @Test
  void doesNotFetchBeyondTheInFlightWindow() throws Exception {
    byte[] data = randomBytes(100 * BLOCK);
    var fetcher = new InMemoryRangeFetcher(data);
    int parallelism = 2;
    int inFlight = 3 * parallelism;

    try (InputStream in = open(data, fetcher, BLOCK, parallelism)) {
      // Consume nothing, then let the workers settle.
      Thread.sleep(300);
      assertThat(fetcher.rangesStarted()).isAtMost(inFlight);
      assertThat(fetcher.maxInFlight()).isAtMost(inFlight);

      assertThat(in.readAllBytes()).isEqualTo(data);
      // Over the whole read the window still bounds concurrency.
      assertThat(fetcher.maxInFlight()).isAtMost(inFlight);
    }
  }

  @Test
  void actuallyFetchesInParallel() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(20 * BLOCK);
      var fetcher = new InMemoryRangeFetcher(data);
      int parallelism = 4;
      CountDownLatch entered = new CountDownLatch(parallelism);
      CountDownLatch release = new CountDownLatch(1);
      fetcher.beforeServe = offset -> {
        entered.countDown();
        release.await();
      };

      try (InputStream in = open(data, fetcher, BLOCK, parallelism)) {
        // If the implementation serialised the fetches this would never reach zero.
        assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        assertThat(in.readAllBytes()).isEqualTo(data);
      }
    });
  }

  /* Failure handling */

  @Test
  void failureSurfacesInStreamOrderAfterTheBytesBeforeIt() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(10 * BLOCK);
      var fetcher = new InMemoryRangeFetcher(data);
      long failingOffset = 5L * BLOCK;
      // Block 7 fails too, and sooner, but block 5 is the one the consumer must observe.
      fetcher.failure = offset ->
        offset == failingOffset || offset == 7L * BLOCK
          ? new IOException("boom at " + offset)
          : null;
      fetcher.delayMillis = offset -> offset == failingOffset ? 200L : 0L;

      try (InputStream in = open(data, fetcher, BLOCK, 4)) {
        byte[] before = in.readNBytes(5 * BLOCK);
        assertThat(before).isEqualTo(java.util.Arrays.copyOf(data, 5 * BLOCK));

        IOException e = assertThrows(IOException.class, in::read);
        assertThat(e).hasMessageThat().contains("[5120, 6144)");
        assertThat(e)
          .hasCauseThat()
          .hasMessageThat()
          .contains("boom at " + failingOffset);

        // Sticky: it must not later look like a clean end of stream.
        assertThrows(IOException.class, in::read);
      }
    });
  }

  @Test
  void uncheckedExceptionInAFetcherDoesNotHangTheConsumer() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(4 * BLOCK);
      var fetcher = new InMemoryRangeFetcher(data);
      fetcher.failure = InMemoryRangeFetcher.failAt(0, new IllegalStateException("unchecked"));

      try (InputStream in = open(data, fetcher, BLOCK, 2)) {
        IOException e = assertThrows(IOException.class, in::read);
        assertThat(e).hasCauseThat().isInstanceOf(IllegalStateException.class);
      }
    });
  }

  @Test
  void errorInAFetcherDoesNotHangTheConsumer() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(4 * BLOCK);
      var fetcher = new InMemoryRangeFetcher(data);
      fetcher.failure = InMemoryRangeFetcher.failAt(0, new StackOverflowError("deep"));

      try (InputStream in = open(data, fetcher, BLOCK, 2)) {
        IOException e = assertThrows(IOException.class, in::read);
        assertThat(e).hasCauseThat().isInstanceOf(StackOverflowError.class);
      }
    });
  }

  /* Cancellation and lifecycle */

  @Test
  void closeUnblocksAConsumerWaitingOnAStalledRange() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(8 * BLOCK);
      CountDownLatch entered = new CountDownLatch(1);
      // A range that only returns when its registered resource is closed - which is how a real
      // read blocked on a socket is aborted.
      RangeFetcher stalling = (offset, dst, cancellation) -> {
        CountDownLatch aborted = new CountDownLatch(1);
        if (!cancellation.register(aborted::countDown)) {
          throw new IOException("already cancelled");
        }
        entered.countDown();
        try {
          aborted.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        throw new IOException("aborted");
      };

      InputStream in = open(data, stalling, BLOCK, 2);
      var thrown = new AtomicReference<Throwable>();
      Thread consumer = new Thread(() -> {
        try {
          in.read();
        } catch (Throwable t) {
          thrown.set(t);
        }
      });
      consumer.start();
      assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();

      in.close();

      consumer.join(10_000);
      assertThat(consumer.isAlive()).isFalse();
      assertThat(thrown.get()).isInstanceOf(IOException.class);
    });
  }

  @Test
  void closeLeavesNoRangeReaderThreadsBehind() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(50 * BLOCK);
      var fetcher = new InMemoryRangeFetcher(data);
      fetcher.delayMillis = offset -> 20L;

      InputStream in = open(data, fetcher, BLOCK, 4);
      // The "early abandon" pattern: read a little, then drop the stream.
      assertThat(in.read()).isEqualTo(data[0] & 0xFF);
      in.close();

      pollUntil(() -> liveRangeThreads() == 0, Duration.ofSeconds(10));
      assertThat(liveRangeThreads()).isEqualTo(0);
    });
  }

  @Test
  void readingToTheEndReleasesTheRangeReaderThreadsEvenWithoutClose() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(20 * BLOCK);
      InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 4);
      assertThat(in.readAllBytes()).isEqualTo(data);

      // Idle pool threads are GC roots, so a forgotten close() must not leak them.
      pollUntil(() -> liveRangeThreads() == 0, Duration.ofSeconds(10));
      assertThat(liveRangeThreads()).isEqualTo(0);

      in.close();
    });
  }

  @Test
  void closeIsIdempotentAndReadsAfterCloseThrow() throws IOException {
    byte[] data = randomBytes(4 * BLOCK);
    InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, 2);
    in.close();
    in.close();
    in.close();
    assertThrows(IOException.class, in::read);
    assertThrows(IOException.class, () -> in.read(new byte[4], 0, 4));
  }

  @Test
  void interruptingTheConsumerRaisesInterruptedIoException() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      byte[] data = randomBytes(8 * BLOCK);
      var fetcher = new InMemoryRangeFetcher(data);
      CountDownLatch hold = new CountDownLatch(1);
      fetcher.beforeServe = offset -> hold.await();

      try (InputStream in = open(data, fetcher, BLOCK, 2)) {
        var thrown = new AtomicReference<Throwable>();
        Thread consumer = new Thread(() -> {
          try {
            in.read();
          } catch (Throwable t) {
            thrown.set(t);
          }
        });
        consumer.start();
        Thread.sleep(200);
        consumer.interrupt();
        consumer.join(10_000);

        assertThat(consumer.isAlive()).isFalse();
        assertThat(thrown.get()).isInstanceOf(InterruptedIOException.class);
        hold.countDown();
      }
    });
  }

  /* helpers */

  private static ParallelRangeInputStream open(
    byte[] data,
    RangeFetcher fetcher,
    int blockSize,
    int parallelism
  ) {
    int blockCount = (int) ((data.length + (long) blockSize - 1) / blockSize);
    int inFlight = Math.max(1, Math.min(3 * parallelism, blockCount));
    var plan = new ParallelReadParameters.ReadPlan(blockSize, blockCount, parallelism, inFlight);
    return new ParallelRangeInputStream(fetcher, NAME, data.length, plan);
  }

  private static byte[] randomBytes(int size) {
    byte[] data = new byte[size];
    new Random(size).nextBytes(data);
    return data;
  }

  private static long liveRangeThreads() {
    return Thread.getAllStackTraces()
      .keySet()
      .stream()
      .filter(t -> t.getName().startsWith("gs-range-"))
      .filter(Thread::isAlive)
      .count();
  }

  private static void pollUntil(BooleanCondition condition, Duration timeout)
    throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.isTrue()) {
        return;
      }
      Thread.sleep(25);
    }
  }

  @FunctionalInterface
  private interface BooleanCondition {
    boolean isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 4 })
  void emptyObjectIsAnImmediateEndOfStream(int parallelism) throws IOException {
    byte[] data = new byte[0];
    try (InputStream in = open(data, new InMemoryRangeFetcher(data), BLOCK, parallelism)) {
      assertThat(in.read()).isEqualTo(-1);
      assertThat(in.readAllBytes()).isEmpty();
    }
  }
}
