package org.opentripplanner.utils.logging;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.opentripplanner.utils.text.FileSizeToTextConverter;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * The progress tracker notify the caller based a time interval.
 * <p>
 * To avoid the caller from being notified to often the tracker uses a 'timer'. The 'timer' prevent
 * notification unless a minimum amount of time is passed since last time the caller was notified.
 * The quiet period is set to 5 seconds.
 * <p>
 * There is also a 'minBlockSize' which prevent the tracker of calling the {@link
 * System#currentTimeMillis()} for each step, instead the timer is checked once for each block of
 * steps. This make the progress step up in regular, nice to read, chunks too.
 * <p>
 * THIS CLASS IS THREAD SAFE The progress tracker is created to be thread-safe.
 */
public class ProgressTracker {

  /**
   * Set the quiet period for the progress tracker, this value is used by all production code.
   */
  public static final int QUIET_PERIOD_MILLISECONDS = 5000;

  /**
   * The expected number of steps.
   */
  private final long expectedNumberOfSteps;

  /**
   * The minimum time in milliseconds between each progress notification.
   */
  private final long quietPeriodMilliseconds;

  /**
   * Format the steps as bytes - e.g. reading bytes from a file.
   */
  private final boolean logFormatAsBytes;

  /**
   * The minimum number of steps between each time check. This make sure the {@link
   * System#currentTimeMillis()} is not called for every step.
   */
  private final int minBlockSize;

  /**
   * The name to use in the notification messages.
   */
  private final String actionName;

  /** Count number of steps */
  private final AtomicLong stepCounter = new AtomicLong(0);

  /** The time when the caller started the process. */
  private final Instant startTime;

  /** The time for the last notification */
  private Instant lastNotification;

  /** Package local to allow unit testing. */
  ProgressTracker(
    String actionName,
    int minBlockSize,
    long expectedNumberOfSteps,
    long quietPeriodMilliseconds,
    boolean logFormatAsBytes
  ) {
    this.actionName = actionName;
    this.minBlockSize = Math.max(1, minBlockSize);
    this.logFormatAsBytes = logFormatAsBytes;
    this.expectedNumberOfSteps = expectedNumberOfSteps;
    this.quietPeriodMilliseconds = quietPeriodMilliseconds;

    // Init the tracker here in case the caller do NOT call start
    this.startTime = Instant.now();
    this.lastNotification = startTime;
  }

  /**
   * Track progress for the given action.
   *
   * @param actionName   the action name to include in the notification strings.
   * @param minBlockSize the minimum number of steps between each time check. A reasonably value is
   *                     to use the number of steps which would take approximately 1 second. Set
   *                     this lower if the time variation for each step is big.
   * @param size         The expected number the step method is called. If negative the size is
   *                     considered unknown.
   */
  public static ProgressTracker track(String actionName, int minBlockSize, long size) {
    return new ProgressTracker(actionName, minBlockSize, size, QUIET_PERIOD_MILLISECONDS, false);
  }

  /**
   * Create an InputStream that decorate another InputStream with progress logging.
   *
   * @param actionName           the action name to include in the notification strings.
   * @param minBlockSize         the minimum number of steps between each time check. A reasonably
   *                             value is to use the number of steps which would take approximately
   *                             1 second. Set this lower if the time variation for each step is
   *                             big.
   * @param size                 The expected number the step method is called. If negative the size
   *                             is considered unknown.
   * @param inputStream          the "real" input stream to delegate all operations to.
   * @param progressNotification the progress notification handler/subscriber.
   */
  public static InputStream track(
    String actionName,
    int minBlockSize,
    long size,
    InputStream inputStream,
    Consumer<String> progressNotification
  ) {
    return new ProgressTrackerInputStream(
      new ProgressTracker(actionName, minBlockSize, size, QUIET_PERIOD_MILLISECONDS, true),
      inputStream,
      progressNotification
    );
  }

  /**
   * Create an OutputStream that decorate another OutputStream with progress logging.
   *
   * @param actionName           the action name to include in the notification strings.
   * @param minBlockSize         the minimum number of steps between each time check. A reasonably
   *                             value is to use the number of steps which would take approximately
   *                             1 second. Set this lower if the time variation for each step is
   *                             big.
   * @param size                 The expected number the step method is called. If negative the size
   *                             is considered unknown.
   * @param outputStream         the "real" input stream to delegate all operations to.
   * @param progressNotification the progress notification handler/subscriber.
   */
  public static OutputStream track(
    String actionName,
    int minBlockSize,
    long size,
    OutputStream outputStream,
    Consumer<String> progressNotification
  ) {
    return new ProgressTrackerOutputStream(
      new ProgressTracker(actionName, minBlockSize, size, QUIET_PERIOD_MILLISECONDS, true),
      outputStream,
      progressNotification
    );
  }

  public String startMessage() {
    return actionName + " progress tracking started.";
  }

  /**
   * This method calls {@code progressNotification} with the {@link #startMessage()} if it is the
   * first step, if not it calls the {@link #steps(int, Consumer)}.
   * <p>
   * This method is used if you would like to avoid logging the start message - in case
   * the progress completes before reaching the first {@link #startOrStep(Consumer)}
   * statement.
   */
  public void startOrStep(Consumer<String> progressNotification) {
    long counter = stepCounter.incrementAndGet();
    if (counter == 1) {
      progressNotification.accept(startMessage());
      return;
    }
    if (counter % minBlockSize != 0) {
      return;
    }
    notifyIfQuietPeriodIsOver(counter, progressNotification);
  }

  public void step(Consumer<String> progressNotification) {
    long counter = stepCounter.incrementAndGet();
    if (counter % minBlockSize != 0) {
      return;
    }
    notifyIfQuietPeriodIsOver(counter, progressNotification);
  }

  /**
   * This method is used to report more than one step. Let say you can not call the progress tracker
   * for each step, but want the logging of the steps performed to reflect the actual number of
   * elements processed. Then this method gives you the flexibility to "jump" a number of given
   * {@code deltaSteps} for each invocation.
   *
   * @param deltaSteps           number of steps performed for this invocation.
   * @param progressNotification the notification callback
   */
  public void steps(int deltaSteps, Consumer<String> progressNotification) {
    // This need to be THREAD-SAFE, so we can only access the stepCounter once. We need to know
    // the current value and the new value after it the stepCounter is incremented. This is
    // necessary to be able to know if we should proceed with a notification. We achieve this
    // by reading and incrementing the stepCounter in one operation, and then calculating the
    // "unknown" value in the local thread. We deliberate avoid to ask for what the value has
    // become, because another thread might have updated the stepCounter in the mean time.
    long prev = stepCounter.getAndAdd(deltaSteps);

    // This could be replaced by "stepCounter.get()", but that would NOT be thread safe.
    long counter = prev + deltaSteps;
    long nextNotificationIndex = (1 + prev / minBlockSize) * minBlockSize;

    if (counter < nextNotificationIndex) {
      return;
    }

    notifyIfQuietPeriodIsOver(counter, progressNotification);
  }

  /**
   * Log complete message if at least one step is performed. This is usually used in combination
   * with {@link #startOrStep(Consumer)}.
   */
  public void completeIfHasSteps(Consumer<String> progressNotification) {
    if (stepCounter.get() > 0) {
      progressNotification.accept(completeMessage());
    }
  }

  public String completeMessage() {
    long ii = stepCounter.get();
    Duration totalTime = Duration.between(startTime, Instant.now());
    // Add 1 millisecond to prevent / by zero.
    String stepsPerSecond = toStr(Math.round((1000d * ii) / (totalTime.toMillis() + 1)));
    return String.format(
      "%s progress tracking complete. %s done in %s (%s per second). ",
      actionName,
      toStr(ii),
      DurationUtils.durationToStr(totalTime),
      stepsPerSecond
    );
  }

  private void notifyIfQuietPeriodIsOver(final long counter, final Consumer<String> notification) {
    // And it is more than N milliseconds since last notification
    Instant time = Instant.now();

    // Check if the quiet time is over, and that it is time to do a new
    // notification.
    synchronized (this) {
      if (time.isBefore(lastNotification.plusMillis(quietPeriodMilliseconds))) {
        return;
      }
      // Prepare for next iteration
      lastNotification = time;
    }

    // Notify caller
    if (expectedNumberOfSteps > 0) {
      long p = (100 * counter) / expectedNumberOfSteps;
      notification.accept(
        String.format(
          "%s progress: %s of %s (%2d%%)",
          actionName,
          toStr(counter),
          toStr(expectedNumberOfSteps),
          p
        )
      );
    } else {
      notification.accept(String.format("%s progress: %s done", actionName, toStr(counter)));
    }
  }

  private String toStr(long value) {
    return logFormatAsBytes
      ? FileSizeToTextConverter.fileSizeToString(value)
      : DecimalFormat.getInstance().format(value);
  }
}
