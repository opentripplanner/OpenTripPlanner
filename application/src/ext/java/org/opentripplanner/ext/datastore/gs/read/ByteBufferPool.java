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
 * The buffers are direct for two reasons: {@code ReadChannel.read(ByteBuffer)} is the native API
 * and a heap buffer would force an extra copy through a thread-local direct buffer anyway, and
 * keeping them off-heap means the block size is a throughput decision rather than a G1
 * humongous-allocation decision (the humongous threshold moves with the heap size).
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
