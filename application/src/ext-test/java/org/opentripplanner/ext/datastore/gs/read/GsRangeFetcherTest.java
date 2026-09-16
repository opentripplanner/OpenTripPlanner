package org.opentripplanner.ext.datastore.gs.read;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.ReadChannel;
import com.google.cloud.RestorableState;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Exercises the Cloud Storage specific half of the parallel read - range setup, generation pinning
 * and the retry/resume loop - against a fake {@link ReadChannel}, so no bucket is needed.
 */
class GsRangeFetcherTest {

  private static final String PATH = "gs://bucket/object";
  private static final byte[] DATA = randomBytes(4096);

  @Test
  void readsTheRequestedRangeAndSetsTheChannelUp() throws IOException {
    var channel = new FakeReadChannel(DATA);
    var fetcher = new GsRangeFetcher(blobReturning(channel), PATH);

    ByteBuffer dst = ByteBuffer.allocate(1000);
    fetcher.fetch(1024, dst, alwaysActive());

    dst.flip();
    assertThat(bytesOf(dst)).isEqualTo(Arrays.copyOfRange(DATA, 1024, 2024));
    assertThat(channel.seekedTo).isEqualTo(1024L);
    assertThat(channel.limitedTo).isEqualTo(2024L);
    assertThat(channel.closed).isTrue();
  }

  @Test
  void pinsTheObjectGenerationSoRangesCannotStraddleARewrite() throws IOException {
    Blob blob = mock(Blob.class);
    var captor = ArgumentCaptor.forClass(Blob.BlobSourceOption.class);
    when(blob.reader(captor.capture())).thenReturn(new FakeReadChannel(DATA));

    new GsRangeFetcher(blob, PATH).fetch(0, ByteBuffer.allocate(16), alwaysActive());

    assertThat(captor.getAllValues()).contains(Blob.BlobSourceOption.generationMatch());
  }

  @Test
  void retriesAfterATransientFailureAndResumesWhereItStopped() throws IOException {
    var first = new FakeReadChannel(DATA);
    first.failAfter = 400;
    first.failure = new IOException("connection reset");
    var second = new FakeReadChannel(DATA);

    var fetcher = new GsRangeFetcher(blobReturning(first, second), PATH);
    ByteBuffer dst = ByteBuffer.allocate(1000);
    fetcher.fetch(1024, dst, alwaysActive());

    dst.flip();
    assertThat(bytesOf(dst)).isEqualTo(Arrays.copyOfRange(DATA, 1024, 2024));
    // The retry must not refetch the bytes already in the buffer.
    assertThat(second.seekedTo).isEqualTo(1024L + 400);
    assertThat(second.limitedTo).isEqualTo(2024L);
  }

  @Test
  void doesNotRetryAPermanentFailure() {
    var channel = new FakeReadChannel(DATA);
    channel.failAfter = 0;
    channel.failure = new StorageException(404, "No such object");

    var fetcher = new GsRangeFetcher(blobReturning(channel, new FakeReadChannel(DATA)), PATH);

    var e = assertThrows(IOException.class, () ->
      fetcher.fetch(0, ByteBuffer.allocate(64), alwaysActive())
    );
    assertThat(e).hasMessageThat().contains(PATH);
  }

  @Test
  void abortsImmediatelyWhenTheReadIsAlreadyCancelled() {
    var channel = new FakeReadChannel(DATA);
    var fetcher = new GsRangeFetcher(blobReturning(channel), PATH);

    assertThrows(IOException.class, () ->
      fetcher.fetch(0, ByteBuffer.allocate(64), alreadyCancelled())
    );
    // The channel it had just opened must not be left dangling.
    assertThat(channel.closed).isTrue();
  }

  @Test
  void reportsRetriesInTheSummary() throws IOException {
    var first = new FakeReadChannel(DATA);
    first.failAfter = 10;
    first.failure = new IOException("blip");
    var fetcher = new GsRangeFetcher(blobReturning(first, new FakeReadChannel(DATA)), PATH);

    assertThat(fetcher.statsSummary()).isEmpty();
    fetcher.fetch(0, ByteBuffer.allocate(64), alwaysActive());
    assertThat(fetcher.statsSummary()).contains("1 retries");
  }

  /* helpers */

  private static Blob blobReturning(ReadChannel... channels) {
    Blob blob = mock(Blob.class);
    List<ReadChannel> queue = new ArrayList<>(Arrays.asList(channels));
    when(blob.reader(any(Blob.BlobSourceOption[].class))).thenAnswer(invocation ->
      queue.isEmpty() ? new FakeReadChannel(DATA) : queue.remove(0)
    );
    return blob;
  }

  private static RangeFetcher.Cancellation alwaysActive() {
    return new RangeFetcher.Cancellation() {
      @Override
      public boolean register(@Nullable Closeable closeable) {
        return true;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }
    };
  }

  private static RangeFetcher.Cancellation alreadyCancelled() {
    var opened = new AtomicReference<Closeable>();
    return new RangeFetcher.Cancellation() {
      @Override
      public boolean register(@Nullable Closeable closeable) {
        opened.set(closeable);
        return false;
      }

      @Override
      public boolean isCancelled() {
        return true;
      }
    };
  }

  private static byte[] bytesOf(ByteBuffer buffer) {
    byte[] out = new byte[buffer.remaining()];
    buffer.get(out);
    return out;
  }

  private static byte[] randomBytes(int size) {
    byte[] data = new byte[size];
    new Random(42).nextBytes(data);
    return data;
  }

  /** A {@link ReadChannel} over a byte array that can fail part way through a range. */
  private static final class FakeReadChannel implements ReadChannel {

    private final byte[] data;

    long seekedTo = -1;
    long limitedTo = -1;
    boolean closed = false;

    /** Raise {@link #failure} after this many bytes of this channel have been served. */
    int failAfter = -1;

    @Nullable
    RuntimeException storageFailure;

    @Nullable
    Exception failure;

    private long position = 0;
    private long limit = Long.MAX_VALUE;
    private int served = 0;

    FakeReadChannel(byte[] data) {
      this.data = data;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
      if (failure != null && served >= failAfter) {
        throwFailure();
      }
      long end = Math.min(limit, data.length);
      if (position >= end) {
        return -1;
      }
      int n = (int) Math.min(dst.remaining(), end - position);
      if (failure != null) {
        n = Math.min(n, failAfter - served);
      }
      if (n <= 0) {
        throwFailure();
      }
      dst.put(data, (int) position, n);
      position += n;
      served += n;
      return n;
    }

    private void throwFailure() throws IOException {
      switch (failure) {
        case IOException io -> throw io;
        case RuntimeException re -> throw re;
        case null -> throw new IOException("unexpected");
        default -> throw new IOException(failure);
      }
    }

    @Override
    public void seek(long newPosition) {
      seekedTo = newPosition;
      position = newPosition;
    }

    @Override
    public ReadChannel limit(long newLimit) {
      limitedTo = newLimit;
      this.limit = newLimit;
      return this;
    }

    @Override
    public long limit() {
      return limit;
    }

    @Override
    public void setChunkSize(int chunkSize) {
      // not relevant for the fake
    }

    @Override
    public boolean isOpen() {
      return !closed;
    }

    @Override
    public void close() {
      closed = true;
    }

    @Override
    public RestorableState<ReadChannel> capture() {
      throw new UnsupportedOperationException();
    }
  }
}
