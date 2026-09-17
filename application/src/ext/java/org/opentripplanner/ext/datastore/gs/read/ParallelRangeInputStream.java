package org.opentripplanner.ext.datastore.gs.read;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.opentripplanner.ext.datastore.gs.read.ParallelReadParameters.ReadPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presents an object as a sequential {@link InputStream} while fetching fixed-size blocks of it
 * concurrently, so that transfer time overlaps the consumer's work instead of being serialised
 * with it.
 * <p>
 * The object is split into blocks of {@code blockSize} bytes. The <em>consumer thread is also the
 * dispatcher</em>: it submits the first {@code inFlight} blocks up front, and each time it finishes
 * draining a block it releases that buffer and submits exactly one more. Submission order is
 * therefore block order, so taking from the pipeline yields blocks in order and no reordering
 * buffer is needed - workers may complete in any order, they only count down their own latch. The
 * consumer never waits on anything but the head.
 * <p>
 * With {@code parallelism == 1} this degenerates to a single feeder thread reading one range at a
 * time. It is not equivalent to reading the object over one continuous connection: every block
 * still opens its own channel, so there is no second request to hide that setup cost behind, and
 * the configuration therefore rejects it - it only exists here as the base case of the algorithm.
 * <p>
 * Not thread-safe for concurrent readers: one consumer thread at a time. {@link #close()} may be
 * called from any thread. The worker threads are released when the stream is closed or read to the
 * end, and time out on their own if it is abandoned part way.
 */
public final class ParallelRangeInputStream extends InputStream {

  private static final Logger LOG = LoggerFactory.getLogger(ParallelRangeInputStream.class);

  private static final long CLOSE_TIMEOUT_SECONDS = 10;
  private static final long POLL_INTERVAL_MS = 200;
  private static final long WORKER_IDLE_TIMEOUT_SECONDS = 30;

  private final RangeFetcher fetcher;
  private final String sourceName;
  private final long size;
  private final ReadPlan plan;
  private final ByteBufferPool pool;
  private final ExecutorService executor;
  private final ArrayBlockingQueue<PendingBlock> pipeline;

  /**
   * Blocks that have been submitted and not yet handed to the consumer. {@link #close()} cancels
   * these; the pipeline itself is deliberately left intact so a consumer blocked in {@code take()}
   * still receives its block and can observe the cancellation.
   */
  private final Set<PendingBlock> outstanding = ConcurrentHashMap.newKeySet();

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final long startNanos = System.nanoTime();

  /* Consumer-thread-confined state - no synchronisation needed. */
  private int nextBlockToDispatch = 0;
  private int blocksConsumed = 0;

  @Nullable
  private PendingBlock current = null;

  private long totalDelivered = 0;
  private long headOfLineWaitNanos = 0;
  private boolean eof = false;

  /** Sticky: once set, every subsequent read rethrows it, so truncation is never seen as EOF. */
  @Nullable
  private IOException failure = null;

  public ParallelRangeInputStream(
    RangeFetcher fetcher,
    String sourceName,
    long size,
    ReadPlan plan
  ) {
    this.fetcher = fetcher;
    this.sourceName = sourceName;
    this.size = size;
    this.plan = plan;
    this.pool = new ByteBufferPool(plan.inFlight(), plan.blockSize());
    this.pipeline = new ArrayBlockingQueue<>(plan.inFlight());
    this.executor = newExecutor(plan.parallelism(), sourceName);

    LOG.debug(
      "Reading {} ({} bytes) with {} workers, {} blocks of {} bytes, {} MB off-heap.",
      sourceName,
      size,
      plan.parallelism(),
      plan.blockCount(),
      plan.blockSize(),
      plan.poolBytes() / (1024 * 1024)
    );

    try {
      for (int i = 0; i < plan.inFlight(); ++i) {
        dispatchNext();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      this.failure = new InterruptedIOException("Interrupted while opening " + sourceName);
    }
  }

  @Override
  public int read() throws IOException {
    if (!ensureCurrent()) {
      return -1;
    }
    int b = current.buffer.get() & 0xFF;
    ++totalDelivered;
    return b;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, b.length);
    if (len == 0) {
      checkNotClosed();
      return 0;
    }
    if (!ensureCurrent()) {
      return -1;
    }
    int n = Math.min(len, current.buffer.remaining());
    current.buffer.get(b, off, n);
    totalDelivered += n;
    return n;
  }

  @Override
  public long skip(long n) throws IOException {
    long remaining = n;
    while (remaining > 0 && ensureCurrent()) {
      int step = (int) Math.min(remaining, current.buffer.remaining());
      current.buffer.position(current.buffer.position() + step);
      totalDelivered += step;
      remaining -= step;
    }
    return n - remaining;
  }

  /** Never blocks: only what is already decoded in the current block. */
  @Override
  public int available() {
    return current == null ? 0 : current.buffer.remaining();
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    // Cancel before shutting the executor down: a worker blocked on the network is released by
    // closing its channel, not by interruption, and a worker that was never scheduled is released
    // by counting its latch down here.
    for (PendingBlock block : outstanding) {
      block.cancel();
      block.done.countDown();
    }
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        LOG.warn("Timed out waiting for the range readers of {} to stop.", sourceName);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    logSummary();
  }

  /* private methods */

  /**
   * Make sure {@link #current} has bytes left, taking and awaiting further blocks as needed.
   * Returns {@code false} at end of stream.
   */
  private boolean ensureCurrent() throws IOException {
    if (failure != null) {
      throw failure;
    }
    checkNotClosed();
    if (eof) {
      return false;
    }
    while (current == null || !current.buffer.hasRemaining()) {
      if (current != null) {
        pool.release(current.buffer);
        current = null;
        ++blocksConsumed;
        dispatchNextOrFail();
      }
      if (blocksConsumed >= plan.blockCount()) {
        eof = true;
        // Nothing more will be dispatched: let the workers go now rather than at close(), so a
        // caller that reads to the end but never closes does not keep them alive.
        executor.shutdown();
        verifyComplete();
        return false;
      }
      current = takeNext();
    }
    return true;
  }

  private PendingBlock takeNext() throws IOException {
    try {
      // Poll rather than take: close() stops the dispatcher, so a consumer that arrives here just
      // after close() could otherwise wait on a queue nothing will ever fill again. In normal
      // operation the queue is never empty here and this returns on the first poll.
      PendingBlock block;
      while ((block = pipeline.poll(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)) == null) {
        if (closed.get()) {
          throw fail(new IOException("Stream closed: " + sourceName));
        }
      }
      long waitStart = System.nanoTime();
      block.done.await();
      headOfLineWaitNanos += System.nanoTime() - waitStart;
      outstanding.remove(block);

      // Check cancellation first, so closing produces "stream closed" rather than the incidental
      // I/O error that closing the channel provoked.
      if (closed.get() || block.isCancelled()) {
        throw fail(new IOException("Stream closed: " + sourceName));
      }
      if (block.error != null) {
        throw fail(
          new IOException(
            "Failed to read bytes [%d, %d) of %s".formatted(
              block.offset,
              block.offset + block.length,
              sourceName
            ),
            block.error
          )
        );
      }
      return block;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw fail(new InterruptedIOException("Interrupted while reading " + sourceName));
    }
  }

  private void dispatchNextOrFail() throws IOException {
    try {
      dispatchNext();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw fail(new InterruptedIOException("Interrupted while reading " + sourceName));
    }
  }

  private void dispatchNext() throws InterruptedException {
    if (nextBlockToDispatch >= plan.blockCount() || closed.get()) {
      return;
    }
    // The single back-pressure point: there are exactly inFlight buffers, so at most inFlight
    // blocks can be outstanding and the footprint is an invariant.
    ByteBuffer buffer = pool.acquire();

    int index = nextBlockToDispatch++;
    long offset = (long) index * plan.blockSize();
    int length = (int) Math.min(plan.blockSize(), size - offset);
    buffer.clear();
    buffer.limit(length);

    PendingBlock block = new PendingBlock(index, offset, length, buffer);
    outstanding.add(block);
    pipeline.add(block);

    try {
      executor.execute(() -> runBlock(block));
    } catch (RejectedExecutionException e) {
      // Lost the race with close(); make sure the consumer is not left waiting.
      block.cancel();
      block.done.countDown();
    }
  }

  private void runBlock(PendingBlock block) {
    try {
      // The closed check covers a block dispatched after close() had already swept `outstanding`
      // but before the executor rejected new work; without it that block would fetch to the end.
      if (closed.get() || block.isCancelled()) {
        throw new IOException("Read cancelled");
      }
      fetcher.fetch(block.offset, block.buffer, block);
      block.buffer.flip();
    } catch (Throwable t) {
      // Deliberately Throwable: an unchecked exception that escaped here would leave the consumer
      // waiting on the latch forever.
      block.error = t;
    } finally {
      block.done.countDown();
    }
  }

  private void verifyComplete() throws IOException {
    if (totalDelivered != size) {
      throw fail(
        new IOException(
          "Short read of %s: expected %d bytes but assembled %d".formatted(
            sourceName,
            size,
            totalDelivered
          )
        )
      );
    }
  }

  private void checkNotClosed() throws IOException {
    if (closed.get()) {
      throw new IOException("Stream closed: " + sourceName);
    }
  }

  private IOException fail(IOException e) {
    failure = e;
    return e;
  }

  private void logSummary() {
    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
    if (elapsedMs <= 0) {
      elapsedMs = 1;
    }
    LOG.info(
      "Read {} of {} in {} ms ({} MB/s) - {} workers, {} blocks of {} MB, {} ms head-of-line wait.{}",
      totalDelivered,
      sourceName,
      elapsedMs,
      totalDelivered / 1_048_576.0 / (elapsedMs / 1000.0),
      plan.parallelism(),
      plan.blockCount(),
      plan.blockSize() / (1024 * 1024),
      headOfLineWaitNanos / 1_000_000,
      fetcher.statsSummary()
    );
  }

  /**
   * Idle workers are GC roots that hold the executor alive, so they must not be permanent: they
   * are shut down at end of stream and on close, and time out on their own otherwise.
   */
  private static ExecutorService newExecutor(int parallelism, String sourceName) {
    var executor = new ThreadPoolExecutor(
      parallelism,
      parallelism,
      WORKER_IDLE_TIMEOUT_SECONDS,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(),
      threadFactory(sourceName)
    );
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  private static ThreadFactory threadFactory(String sourceName) {
    String shortName = sourceName.substring(sourceName.lastIndexOf('/') + 1);
    if (shortName.length() > 24) {
      shortName = shortName.substring(0, 24);
    }
    return Thread.ofPlatform()
      .name("gs-range-" + shortName + "-", 0)
      .daemon(true)
      .factory();
  }

  private static void closeQuietly(@Nullable Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ignored) {
        // Closing to abort a read; a failure here carries no information.
      }
    }
  }

  /**
   * One block in flight. Also the {@link RangeFetcher.Cancellation} handed to the fetcher, so the
   * resource to close in order to abort a blocked read is published without racing cancellation.
   */
  private static final class PendingBlock implements RangeFetcher.Cancellation {

    private final int index;
    private final long offset;
    private final int length;
    private final ByteBuffer buffer;
    private final CountDownLatch done = new CountDownLatch(1);

    @Nullable
    private volatile Throwable error = null;

    private boolean cancelled = false;

    @Nullable
    private Closeable resource = null;

    PendingBlock(int index, long offset, int length, ByteBuffer buffer) {
      this.index = index;
      this.offset = offset;
      this.length = length;
      this.buffer = buffer;
    }

    @Override
    public boolean register(@Nullable Closeable closeable) {
      Closeable previous;
      synchronized (this) {
        if (cancelled) {
          return false;
        }
        previous = resource;
        resource = closeable;
      }
      closeQuietly(previous);
      return true;
    }

    @Override
    public boolean isCancelled() {
      synchronized (this) {
        return cancelled;
      }
    }

    void cancel() {
      Closeable open;
      synchronized (this) {
        if (cancelled) {
          return;
        }
        cancelled = true;
        open = resource;
        resource = null;
      }
      closeQuietly(open);
    }

    @Override
    public String toString() {
      return "block " + index + " [" + offset + ", " + (offset + length) + ")";
    }
  }
}
