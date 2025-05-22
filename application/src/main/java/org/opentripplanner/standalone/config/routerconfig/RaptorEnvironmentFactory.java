package org.opentripplanner.standalone.config.routerconfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.concurrent.OtpRequestThreadFactory;
import org.opentripplanner.raptor.api.request.RaptorEnvironment;

/**
 * Create {@link RaptorEnvironment} from config and adapt it to the OTP application.
 */
public class RaptorEnvironmentFactory {

  public static RaptorEnvironment create(final int threadPoolSize) {
    return new RaptorEnvironment() {
      @Override
      public Runnable timeoutHook() {
        return OTPRequestTimeoutException::checkForTimeout;
      }

      /**
       * OTP web server will interrupt all request threads in case of a timeout. In OTP
       * such events should be mapped to {@link OTPRequestTimeoutException}, which will
       * later be mapped to the right API response.
       */
      @Override
      public RuntimeException mapInterruptedException(InterruptedException e) {
        return new OTPRequestTimeoutException();
      }

      @Nullable
      @Override
      public ExecutorService threadPool() {
        return threadPoolSize > 0
          ? Executors.newFixedThreadPool(threadPoolSize, OtpRequestThreadFactory.of("raptor-%d"))
          : null;
      }
    };
  }
}
