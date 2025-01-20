package org.opentripplanner.updater.spi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.updater.GraphWriterRunnable;

public class PollingGraphUpdaterTest {

  private static final PollingGraphUpdaterParameters config = new PollingGraphUpdaterParameters() {
    @Override
    public Duration frequency() {
      return Duration.ZERO;
    }

    @Override
    public String configRef() {
      return "";
    }
  };

  private static final PollingGraphUpdater subject = new PollingGraphUpdater(config) {
    @Override
    protected void runPolling() {}
  };

  private boolean updateCompleted;

  @BeforeAll
  static void beforeAll() {
    subject.setup(runnable -> CompletableFuture.runAsync(() -> runnable.run(null)));
  }

  @BeforeEach
  void setUp() {
    updateCompleted = false;
  }

  private final GraphWriterRunnable graphWriterRunnable = context -> {
    try {
      Thread.sleep(100);
      updateCompleted = true;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  };

  @Test
  void testUpdateGraphWithWaitFeatureOn() {
    OTPFeature.WaitForGraphUpdateInPollingUpdaters.testOn(() -> {
      callUpdater();
      assertTrue(updateCompleted);
    });
  }

  @Test
  void testProcessGraphUpdaterResultWithWaitFeatureOff() {
    OTPFeature.WaitForGraphUpdateInPollingUpdaters.testOff(() -> {
      callUpdater();
      assertFalse(updateCompleted);
    });
  }

  private void callUpdater() {
    try {
      subject.updateGraph(graphWriterRunnable);
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
