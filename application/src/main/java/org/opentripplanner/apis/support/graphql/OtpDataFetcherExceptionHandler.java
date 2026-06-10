package org.opentripplanner.apis.support.graphql;

import graphql.ErrorClassification;
import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import java.util.concurrent.CompletableFuture;
import org.opentripplanner.apis.support.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OTP-specific exception handler for GraphQL data fetchers. Logs warnings for unexpected
 * exceptions and classifies {@link InvalidInputException} as client errors
 * ({@code BadRequestError}) logged at INFO level.
 */
public class OtpDataFetcherExceptionHandler extends SimpleDataFetcherExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(OtpDataFetcherExceptionHandler.class);

  @Override
  public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
    DataFetcherExceptionHandlerParameters handlerParameters
  ) {
    Throwable exception = unwrap(handlerParameters.getException());

    if (exception instanceof InvalidInputException) {
      LOG.info("Bad request: {}", exception.getMessage());
      var error = GraphQLError.newError()
        .errorType(ErrorClassification.errorClassification("BadRequestError"))
        .message(exception.getMessage())
        .path(handlerParameters.getPath().toList())
        .location(handlerParameters.getSourceLocation())
        .build();
      return CompletableFuture.completedFuture(
        DataFetcherExceptionHandlerResult.newResult(error).build()
      );
    }

    return super.handleException(handlerParameters);
  }

  @Override
  protected void logException(ExceptionWhileDataFetching error, Throwable exception) {
    LOG.warn(error.getMessage(), exception);
  }
}
