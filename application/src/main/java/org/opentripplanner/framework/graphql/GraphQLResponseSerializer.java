package org.opentripplanner.framework.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.execution.AbortExecutionException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opentripplanner.ext.restapi.serialization.JSONObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for serializing a GraphQL {@link ExecutionResult} into a String, which
 * can be returned as the body of the HTTP response. This differs from the mapper provided by {@link
 * JSONObjectMapperProvider}, by serializing all fields in the objects, including null fields.
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
