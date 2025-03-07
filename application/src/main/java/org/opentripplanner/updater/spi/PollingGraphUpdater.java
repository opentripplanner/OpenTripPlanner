package org.opentripplanner.updater.spi;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements logic that is shared between all polling updaters. Usage example
 * ('polling' name is an example and 'polling-updater' should be the type of a concrete class
 * derived from this abstract class):
 *
 * <pre>
 * polling.type = polling-updater
 * polling.frequency = 60
 * </pre>
 *
 * @see GraphUpdater
 */
public abstract class PollingGraphUpdater implements GraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(PollingGraphUpdater.class);
  private final String configRef;
  /** How long to wait after polling to poll again. */
  private final Duration pollingPeriod;

  // TODO OTP2 eliminate this field for reasons in "primed" javadoc; also "initialized" is not a clear term.
  protected boolean blockReadinessUntilInitialized;

  /**
   * True when a full batch of realtime data has been fetched and applied to the graph. There was
   * previously a second boolean field that controlled whether this affected "readiness". If we are
   * waiting for any realtime data to be applied, we should wait for all of it to be applied, so I
   * removed that.
   */
  protected volatile boolean primed;
  /**
   * Parent update manager. Is used to execute graph writer runnables.
   */
  private WriteToGraphCallback saveResultOnGraph;

  /**
   * A Future representing pending completion of most recently submitted task.
   * If the updater posts several tasks during one polling cycle, the handle will point to the
   * latest posted task.
   * Initially null when the polling updater starts.
   */
  @Nullable
  private volatile Future<?> previousTask;

  /** Shared configuration code for all polling graph updaters. */
  protected PollingGraphUpdater(PollingGraphUpdaterParameters config) {
    this.pollingPeriod = config.frequency();
    this.configRef = config.configRef();
  }

  public Duration pollingPeriod() {
    return pollingPeriod;
  }

  @Override
  public final void run() {
    try {
      if (OTPFeature.WaitForGraphUpdateInPollingUpdaters.isOn()) {
        waitForPreviousTask();
      }

      // Run concrete polling graph updater's implementation method.
      runPolling();
      if (runOnlyOnce()) {
        LOG.info(
          "As requested in configuration, updater {} has run only once and will now stop.",
          this.getClass().getSimpleName()
        );
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.info(
        "OTP is shutting down, polling updater {} was interrupted and is stopping.",
        this.getClass().getName()
      );
    } catch (CancellationException e) {
      LOG.info("OTP is shutting down, the polling updater {} was interrupted", this, e);
    } catch (Exception e) {
      LOG.error("Error while running polling updater {}", this, e);
      // TODO Should we cancel the task? Or after n consecutive failures? cancel();
    } finally {
      primed = true;
    }
  }

  /**
   * Non-positive polling period values mean to run the updater only once.
   */
  public boolean runOnlyOnce() {
    return pollingPeriod.toSeconds() <= 0;
  }

  /**
   * Allow clients to wait for all realtime data to be loaded before submitting any travel plan
   * requests. This does not block use of the OTP server. The client must voluntarily hit an
   * endpoint and wait for readiness.
   * TODO OTP2 This is really a bit backward. We should just run() the updaters once before scheduling them to poll,
   *           and not bring the router online until they have finished.
   */
  @Override
  public boolean isPrimed() {
    return primed;
  }

  public String getConfigRef() {
    return configRef;
  }

  @Override
  public final void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  /**
   * Mirrors GraphUpdater.run method. Only difference is that runPolling will be run multiple times
   * with pauses in between. The length of the pause is defined in the preference frequency.
   */
  protected abstract void runPolling() throws Exception;

  /**
   * Post an update task to the GraphWriter queue.
   * This is non-blocking.
   * This can be called several times during one polling cycle.
   * This is the sole way for polling updater implementations to submit real-time update tasks,
   * while technical details about the execution of these tasks
   * (frequency, concurrency, waiting, ...) are encapsulated in this parent class.
   */
  protected final void updateGraph(GraphWriterRunnable task) {
    previousTask = saveResultOnGraph.execute(task);
  }

  /**
   * If the previous task takes longer than the polling interval,
   * we delay the next polling cycle until the task is complete.
   * This prevents tasks from piling up.
   * If the updater sends several tasks during a polling cycle, we wait on the latest posted task.
   * */
  private void waitForPreviousTask() throws InterruptedException, ExecutionException {
    if (previousTask != null && !previousTask.isDone()) {
      LOG.info("Delaying polling until the previous task is complete");
      long startBlockingWait = System.currentTimeMillis();
      previousTask.get();
      LOG.info(
        "Resuming polling after waiting an additional {}s",
        (System.currentTimeMillis() - startBlockingWait) / 1000
      );
    }
  }
}
