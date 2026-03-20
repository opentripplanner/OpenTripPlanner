package org.opentripplanner.framework.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * This class is responsible for serializing a GraphQL {@link ExecutionResult} by streaming it
 * directly to an output stream, avoiding an intermediate String allocation.
 */
public class GraphQLResponseSerializer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
