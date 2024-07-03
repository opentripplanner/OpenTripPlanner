package org.opentripplanner.apis.support.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log a warning message when an exception occurs in a data fetcher.
 */
public class LoggingDataFetcherExceptionHandler extends SimpleDataFetcherExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(
    LoggingDataFetcherExceptionHandler.class
  );

  @Override
  protected void logException(ExceptionWhileDataFetching error, Throwable exception) {
    LOG.warn(error.getMessage(), exception);
  }
}
