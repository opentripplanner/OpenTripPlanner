package org.opentripplanner.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;


/**
 * Wrap an InputStream and log progress while reading it.
 */
class ProgressTrackerOutputStream extends OutputStream {
    private final ProgressTracker progress;
    private final Consumer<String> logger;
    private final OutputStream delegate;

    public ProgressTrackerOutputStream(
            ProgressTracker progress,
            OutputStream delegate,
            Consumer<String> logger
    ) {
        this.progress = progress;
        this.delegate = delegate;
        this.logger = logger;

        // Log start message
        this.logger.accept(this.progress.startMessage());
    }

    @Override
    public void write(byte[] b) throws IOException {
        progress.steps(b.length, logger);
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        progress.steps(len-off, logger);
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void write(int b) throws IOException {
        progress.step(logger);
        delegate.write(b);
    }

    @Override
    public void close() throws IOException {
        logger.accept(progress.completeMessage());
        delegate.close();
    }
}
