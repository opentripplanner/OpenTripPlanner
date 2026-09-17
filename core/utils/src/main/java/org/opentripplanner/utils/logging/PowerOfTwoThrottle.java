package org.opentripplanner.utils.logging;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * A utility class that provides a mechanism for throttling sequential actions. The class ensures
 * that only specific, power-of-two-numbered actions are executed while skipping others. This can
 * be useful in scenarios where some actions need to be performed less frequently, but still at
 * predictable intervals.
 * <p>
 * This implementation is thread-safe and leverages atomic operations to manage internal state.
 */
public class PowerOfTwoThrottle {

  private final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Executes the provided action only if the internal throttling condition evaluates to false.
   * The throttling mechanism ensures that only certain sequential actions are processed, based
   * on the internal counter's state. If the condition is met, the action is executed with the
   * current value of the counter as its argument.
   *
   * @param body An {@code IntConsumer} representing the action to be executed when the throttling
   *             condition fails. The current counter-value is passed to this consumer.
   */
  public void throttle(IntConsumer body) {
    if (!throttle()) {
      body.accept(counter.get());
    }
  }

  private boolean throttle() {
    int i = counter.incrementAndGet();
    // overflow
    if (counter.get() < 0) {
      return true;
    }
    return !isPowerOfTwo(i);
  }

  private static boolean isPowerOfTwo(int n) {
    return (n & (n - 1)) == 0;
  }
}
