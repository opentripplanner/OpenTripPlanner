package org.opentripplanner.ext.transmodelapi.support;

import graphql.execution.AbortExecutionException;

/**
 * Custom GraphQL exception triggered when a GraphQL API call times out.
 */
public class OTPProcessingTimeoutGraphQLException extends AbortExecutionException {

  public OTPProcessingTimeoutGraphQLException(String message) {
    super(message);
  }
}
