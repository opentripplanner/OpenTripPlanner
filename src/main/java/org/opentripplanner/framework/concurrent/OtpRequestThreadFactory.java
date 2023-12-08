package org.opentripplanner.framework.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.application.LogMDCSupport;

/**
 * This thread pool factory should be used to create all threads handling "user" requests in OTP.
 * It is used to instrument new threads which enable log information propagation and error handling,
 * like thread interruption. By "user" we mean users of the query APIs like GTFS GraphQL API,
 * Transmodel GraphQL API and other http endpoints.
 * <p>
 * Real time updaters should NOT use this - we do not want log info propagation and timeout
 * interrupt handling in these threads. They follow a separate multi-threading strategy.
 * <p>
 * The ActuatorAPI is a border-line use-cases for this; It is included because there is no risk
 * involved and that is the simplest solution.
 * <p>
 * We do not apply this to the main grizzly threads either. The factory is used for "child"
 * threads and the "grizzly" threads are the root parents.
 */
public class OtpRequestThreadFactory implements ThreadFactory {

  private final ThreadFactory delegate;

  private OtpRequestThreadFactory(ThreadFactory delegate) {
    this.delegate = delegate;
  }

  public static ThreadFactory of(String nameFormat) {
    var defaultFactory = new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
    return new OtpRequestThreadFactory(defaultFactory);
  }

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    if (LogMDCSupport.isRequestTracingInLoggingEnabled()) {
      return delegate.newThread(new LogMDCRunnableDecorator(r));
    }
    return delegate.newThread(r);
  }
}
