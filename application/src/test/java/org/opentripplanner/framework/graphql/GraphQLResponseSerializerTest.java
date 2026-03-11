package org.opentripplanner.framework.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GraphQLResponseSerializerTest {

  private static final ExecutionResult RESULT = ExecutionResult.newExecutionResult()
    .data("Hello")
    .addError(GraphQLError.newError().message("An error").build())
    .build();

  @Test
  void serializeAsStreamProducesSameOutputAsSerialize() throws IOException {
    var stringResult = GraphQLResponseSerializer.serialize(RESULT);
    var streaming = GraphQLResponseSerializer.serializeAsStream(RESULT);
    var baos = new ByteArrayOutputStream();
    streaming.write(baos);
    assertEquals(stringResult, baos.toString(StandardCharsets.UTF_8));
  }
}
