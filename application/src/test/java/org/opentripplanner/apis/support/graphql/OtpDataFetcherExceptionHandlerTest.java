package org.opentripplanner.apis.support.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.ErrorType;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.InvalidInputException;

class OtpDataFetcherExceptionHandlerTest {

  private final OtpDataFetcherExceptionHandler handler = new OtpDataFetcherExceptionHandler();

  @Test
  void invalidInputExceptionReturnsBadRequestError() {
    var params = buildParams(new InvalidInputException("Unable to combine other filters with ids"));

    var result = handler.handleException(params).join();

    var errors = result.getErrors();
    assertEquals(1, errors.size());
    var error = errors.getFirst();
    assertEquals("Unable to combine other filters with ids", error.getMessage());
    assertEquals("BadRequestError", error.getErrorType().toString());
  }

  @Test
  void regularExceptionReturnsDataFetchingException() {
    var params = buildParams(new RuntimeException("Something went wrong"));

    var result = handler.handleException(params).join();

    var errors = result.getErrors();
    assertEquals(1, errors.size());
    assertEquals(ErrorType.DataFetchingException, errors.getFirst().getErrorType());
  }

  private static DataFetcherExceptionHandlerParameters buildParams(Throwable exception) {
    var field = Field.newField("testField").build();
    var mergedField = MergedField.newMergedField(field).build();
    var fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
      .name("testField")
      .type(GraphQLObjectType.newObject().name("TestType").build())
      .build();
    var stepInfo = ExecutionStepInfo.newExecutionStepInfo()
      .type(GraphQLObjectType.newObject().name("TestType").build())
      .fieldDefinition(fieldDefinition)
      .field(mergedField)
      .path(ResultPath.rootPath().segment("testField"))
      .build();

    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
      .fieldDefinition(fieldDefinition)
      .mergedField(mergedField)
      .executionStepInfo(stepInfo)
      .graphQLContext(GraphQLContext.getDefault())
      .build();

    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
      .dataFetchingEnvironment(env)
      .exception(exception)
      .build();
  }
}
