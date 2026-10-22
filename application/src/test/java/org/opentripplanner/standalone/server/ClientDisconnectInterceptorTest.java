package org.opentripplanner.standalone.server;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ClientDisconnectInterceptorTest {

  private static final byte[] BODY = "{\"data\":{}}".getBytes(StandardCharsets.UTF_8);

  private final ClientDisconnectInterceptor subject = new ClientDisconnectInterceptor();

  @Test
  void writeResponse() throws IOException {
    var client = new ByteArrayOutputStream();

    subject.aroundWriteTo(context(client, out -> out.write(BODY)));

    assertThat(client.toByteArray()).isEqualTo(BODY);
  }

  @Test
  void ignoreClientDisconnect() {
    var closedConnection = new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        throw new IOException("Broken pipe");
      }
    };

    assertDoesNotThrow(() ->
      subject.aroundWriteTo(context(closedConnection, out -> out.write(BODY)))
    );
  }

  @Test
  void ignoreClientDisconnectWrappedByEntityWriter() {
    var closedConnection = new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        throw new IOException("Broken pipe");
      }
    };

    assertDoesNotThrow(() ->
      subject.aroundWriteTo(
        context(closedConnection, out -> {
          try {
            out.write(BODY);
          } catch (IOException e) {
            throw new IllegalStateException("Serialization failed", e);
          }
        })
      )
    );
  }

  @Test
  void rethrowFailureNotCausedByClient() {
    var client = new ByteArrayOutputStream();

    var e = assertThrows(IOException.class, () ->
      subject.aroundWriteTo(
        context(client, out -> {
          throw new IOException("Could not read data");
        })
      )
    );

    assertThat(e).hasMessageThat().isEqualTo("Could not read data");
  }

  @Test
  void rethrowRuntimeFailureNotCausedByClient() {
    var client = new ByteArrayOutputStream();

    assertThrows(IllegalStateException.class, () ->
      subject.aroundWriteTo(
        context(client, out -> {
          throw new IllegalStateException("Serialization failed");
        })
      )
    );
  }

  /**
   * Create a context where {@code proceed()} calls the entity writer with the output stream set
   * by the interceptor, like Jersey does.
   */
  private static WriterInterceptorContext context(OutputStream client, EntityWriter entityWriter)
    throws IOException {
    var context = mock(WriterInterceptorContext.class);
    var stream = new OutputStream[] { client };
    when(context.getOutputStream()).thenAnswer(i -> stream[0]);
    doAnswer(i -> {
      stream[0] = i.getArgument(0);
      return null;
    })
      .when(context)
      .setOutputStream(any());
    doAnswer(i -> {
      entityWriter.write(stream[0]);
      return null;
    })
      .when(context)
      .proceed();
    return context;
  }

  @FunctionalInterface
  private interface EntityWriter {
    void write(OutputStream out) throws IOException;
  }
}
