package org.opentripplanner.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;


/**
 * Wrap an InputStream and log progress while reading it.
 */
class ProgressTrackerInputStream extends InputStream {
    private final ProgressTracker progress;
    private final Consumer<String> logger;
    private final InputStream delegate;

    public ProgressTrackerInputStream(
            ProgressTracker progress,
            InputStream delegate,
            Consumer<String> logger
    ) {
        this.progress = progress;
        this.delegate = delegate;
        this.logger = logger;

        // Log start message
        this.logger.accept(this.progress.startMessage());
    }

    @Override
    public int read() throws IOException {
        int read = delegate.read();
        progress.step(logger);
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int size = delegate.read(b);
        progress.steps(size, logger);
        return size;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int size = delegate.read(b, off, len);
        progress.steps(size, logger);
        return size;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        byte[] bytes = delegate.readAllBytes();
        progress.steps(bytes.length, logger);
        return bytes;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        byte[] bytes = delegate.readNBytes(len);
        progress.steps(bytes.length, logger);
        return bytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int size = delegate.readNBytes(b, off, len);
        progress.steps(size, logger);
        return size;
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        logger.accept(progress.completeMessage());
        delegate.close();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return delegate.transferTo(out);
    }
}
