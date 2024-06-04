package org.opentripplanner.apis.transmodel;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.noOp;

import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A GraphQL instrumentation that periodically checks the OTP request interruption status while the
 * query is being processed.
 */
public class OTPRequestTimeoutInstrumentation implements Instrumentation {

  private static final Logger LOG = LoggerFactory.getLogger(OTPRequestTimeoutInstrumentation.class);

  private final AtomicLong fieldFetchCounter = new AtomicLong();

  @Override
  public InstrumentationContext<Object> beginFieldFetch(
    InstrumentationFieldFetchParameters parameters,
    InstrumentationState state
  ) {
    long fetched = fieldFetchCounter.incrementAndGet();
    if (fetched % 100000 == 0) {
      LOG.debug("Fetched {} fields", fetched);
      OTPRequestTimeoutException.checkForTimeout();
    }
    return noOp();
  }
}
