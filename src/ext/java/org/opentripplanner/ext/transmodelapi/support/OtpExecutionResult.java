package org.opentripplanner.ext.transmodelapi.support;

import graphql.ErrorClassification;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;

/**
 * Return the GraphQl execution result and a flag to indicate the
 * execution failed due to a timeout.
 */
public record OtpExecutionResult(ExecutionResult result, boolean timeout) {
  private static final String API_PROCESSING_TIMEOUT = "ApiProcessingTimeout";

  public static OtpExecutionResult of(ExecutionResult result) {
    return new OtpExecutionResult(result, false);
  }

  public static OtpExecutionResult ofTimeout() {
    return new OtpExecutionResult(
      ExecutionResult
        .newExecutionResult()
        .addError(
          GraphQLError
            .newError()
            .errorType(ErrorClassification.errorClassification(API_PROCESSING_TIMEOUT))
            .message(OTPRequestTimeoutException.MESSAGE)
            .build()
        )
        .build(),
      true
    );
  }
}
