package org.opentripplanner.framework.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for serializing a GraphQL {@link ExecutionResult} into a String, which
 * can be returned as the body of the HTTP response.
 */
public class GraphQLResponseSerializer {

  static final Logger LOG = LoggerFactory.getLogger(GraphQLResponseSerializer.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static String serialize(ExecutionResult executionResult) {
    try {
      return objectMapper.writeValueAsString(executionResult.toSpecification());
    } catch (JsonProcessingException e) {
      LOG.error("Unable to serialize response", e);
      throw new RuntimeException(e);
    }
  }
}
