package org.opentripplanner.framework.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import jakarta.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for serializing a GraphQL {@link ExecutionResult} into a String or
 * streaming it directly to an output stream.
 */
public class GraphQLResponseSerializer {

  private static final Logger LOG = LoggerFactory.getLogger(GraphQLResponseSerializer.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static String serialize(ExecutionResult executionResult) {
    try {
      return OBJECT_MAPPER.writeValueAsString(executionResult.toSpecification());
    } catch (JsonProcessingException e) {
      LOG.error("Unable to serialize response", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Serialize the execution result as a {@link StreamingOutput} that writes JSON directly to the
   * output stream, avoiding an intermediate String allocation. This is important for large
   * responses that would otherwise create humongous G1 GC objects.
   */
  public static StreamingOutput serializeAsStream(ExecutionResult executionResult) {
    var spec = executionResult.toSpecification();
    return outputStream -> OBJECT_MAPPER.writeValue(outputStream, spec);
  }
}
