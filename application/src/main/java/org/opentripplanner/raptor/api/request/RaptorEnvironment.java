package org.opentripplanner.raptor.api.request;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;

/**
 * The raptor environment provides a few hooks and integration points to the caller. The default
 * implementation will work just fine, override to adjust Raptor to the calling application.
 */
public interface RaptorEnvironment {
  Runnable NOOP = () -> {};

  /**
   * Use the timeout-hook to register a callback from Raptor. The hook is called periodically to
   * check if a time-out is reached. The hook should then exit with an exception handled by the
   * caller. Raptor does not have blocking method calls so just calling {@link Thread#interrupt()}
   * will not terminate the Raptor search.
   */
  default Runnable timeoutHook() {
    return NOOP;
  }

  /**
   * Raptor has support for running a few things in parallel. If Raptor catches an
   * {@link InterruptedException}, Raptor will convert the checked exception to an unchecked
   * exception. The default is {@link RuntimeException}. Override this method to map
   * {@link InterruptedException} to your prefered runtime exception.
   */
  default RuntimeException mapInterruptedException(InterruptedException e) {
    return new RuntimeException(e);
  }

  /**
   * Inject a thread pool into Raptor to run part of the raptor search in parallel. If no
   * thread pool is provided, then Raptor runs everything in the caller thread.
   */
  @Nullable
  default ExecutorService threadPool() {
    return null;
  }
}
