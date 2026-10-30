package org.opentripplanner.ext.datastore.gs.read;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A fixed set of equally sized direct buffers, allocated once and circulated between the consumer
 * and the fetch workers.
 * <p>
 * The pool is the single back-pressure point of {@link ParallelRangeInputStream}: a block cannot be
 * dispatched until a buffer is free, so the number of buffers <em>is</em> the number of blocks that
 * can be in flight, and the memory footprint is an invariant rather than a bound.
 * <p>
 * The buffers are direct so that the working set of one read - 192 MB with the defaults - lives
 * outside the heap for the seconds it is needed, instead of pinning a dozen humongous regions
 * there. That is a footprint decision only: the Cloud Storage client copies the response through
 * a small heap array whatever kind of buffer it is handed, so a direct buffer saves no copy.
 */
class ByteBufferPool {

  private final ArrayBlockingQueue<ByteBuffer> free;
  private final int bufferSize;
  private final int capacity;

  ByteBufferPool(int capacity, int bufferSize) {
    this.capacity = capacity;
    this.bufferSize = bufferSize;
    this.free = new ArrayBlockingQueue<>(capacity);
    for (int i = 0; i < capacity; ++i) {
      free.add(ByteBuffer.allocateDirect(bufferSize));
    }
  }

  /** Blocks until a buffer is free. This is the flow control. */
  ByteBuffer acquire() throws InterruptedException {
    return free.take();
  }

  void release(ByteBuffer buffer) {
    buffer.clear();
    // Cannot block: only buffers handed out by this pool are ever returned.
    free.offer(buffer);
  }

  int bufferSize() {
    return bufferSize;
  }

  int capacity() {
    return capacity;
  }

  /** Total off-heap memory held by this pool, for logging and for the sizing guard. */
  long totalBytes() {
    return (long) capacity * bufferSize;
  }
}
