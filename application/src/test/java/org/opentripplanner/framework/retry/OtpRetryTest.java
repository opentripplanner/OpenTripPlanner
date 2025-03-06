package org.opentripplanner.framework.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpRetryTest {

  private CompletableFuture<Boolean> hasRetried;
  private OtpRetryBuilder otpRetryBuilder;

  @BeforeEach
  void beforeEach() {
    hasRetried = new CompletableFuture<>();
    otpRetryBuilder = new OtpRetryBuilder()
      .withName("Test-retry")
      .withInitialRetryInterval(Duration.ZERO)
      .withOnRetry(() -> hasRetried.complete(true));
  }

  @Test
  void testNoInitialFailureNoRetryAttempt() throws InterruptedException {
    otpRetryBuilder.withMaxAttempts(0).build().execute(() -> {});
    assertFalse(hasRetried.isDone());
  }

  @Test
  void testNoInitialFailureOneRetryAttempt() throws InterruptedException {
    otpRetryBuilder.withMaxAttempts(1).build().execute(() -> {});
    assertFalse(hasRetried.isDone());
  }

  @Test
  void testInitialFailureNoRetryAttempt() {
    OtpRetry retry = otpRetryBuilder.withMaxAttempts(0).build();
    assertThrows(OtpRetryException.class, () ->
      retry.execute(() -> {
        throw new RuntimeException("Failed retry");
      })
    );
    assertFalse(hasRetried.isDone());
  }

  @Test
  void testInitialFailureAndOneRetryAttempt() {
    OtpRetry retry = otpRetryBuilder.withMaxAttempts(1).build();
    assertThrows(OtpRetryException.class, () ->
      retry.execute(() -> {
        throw new RuntimeException("Failed retry");
      })
    );
    assertTrue(hasRetried.isDone());
  }

  @Test
  void testInitialFailureAndTwoRetryAttempts() {
    AtomicInteger retryCounter = new AtomicInteger();
    OtpRetry retry = otpRetryBuilder
      .withMaxAttempts(2)
      .withOnRetry(retryCounter::incrementAndGet)
      .build();
    assertThrows(OtpRetryException.class, () ->
      retry.execute(() -> {
        throw new RuntimeException("Failed retry");
      })
    );
    assertEquals(2, retryCounter.get());
  }

  @Test
  void testInitialFailureWithRecoveryAndOneRetryAttempt() throws InterruptedException {
    OtpRetry retry = otpRetryBuilder.withMaxAttempts(1).build();
    retry.execute(() -> {
      if (!hasRetried.isDone()) {
        throw new RuntimeException("Failed retry");
      }
    });
    assertTrue(hasRetried.isDone());
  }

  @Test
  void testInitialFailureWithNonRetryableExceptionAndOneRetryAttempt() {
    OtpRetry retry = otpRetryBuilder
      .withMaxAttempts(1)
      .withRetryableException(IOException.class::isInstance)
      .build();
    assertThrows(OtpRetryException.class, () ->
      retry.execute(() -> {
        throw new RuntimeException("Failed retry");
      })
    );
    assertFalse(hasRetried.isDone());
  }
}
