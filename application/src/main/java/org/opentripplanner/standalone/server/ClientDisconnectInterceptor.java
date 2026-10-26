package org.opentripplanner.standalone.server;

import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a client that disconnects while the response body is written. This is not a server
 * error, and any client can cause it at will, so it is logged at info level without a stack trace.
 * Without this, Jersey logs it as an error with a full stack trace.
 * <p>
 * Failures that are not caused by writing to the client are passed on unchanged.
 */
public class ClientDisconnectInterceptor implements WriterInterceptor {

  private static final Logger LOG = LoggerFactory.getLogger(ClientDisconnectInterceptor.class);

  @Override
  public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
    var clientStream = new FailureTrackingOutputStream(context.getOutputStream());
    context.setOutputStream(clientStream);
    try {
      context.proceed();
    } catch (IOException | RuntimeException e) {
      if (!clientStream.writeFailed) {
        throw e;
      }
      LOG.info(
        "Could not write response, the client probably closed the connection: {}",
        e.toString()
      );
    }
  }

  /**
   * Records whether writing to the client failed, so that this failure can be told apart from
   * other failures while the response is written.
   */
  private static class FailureTrackingOutputStream extends FilterOutputStream {

    private boolean writeFailed = false;

    FailureTrackingOutputStream(OutputStream out) {
      super(out);
    }

    @Override
    public void write(int b) throws IOException {
      try {
        out.write(b);
      } catch (IOException e) {
        writeFailed = true;
        throw e;
      }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      try {
        out.write(b, off, len);
      } catch (IOException e) {
        writeFailed = true;
        throw e;
      }
    }

    @Override
    public void flush() throws IOException {
      try {
        out.flush();
      } catch (IOException e) {
        writeFailed = true;
        throw e;
      }
    }
  }
}
