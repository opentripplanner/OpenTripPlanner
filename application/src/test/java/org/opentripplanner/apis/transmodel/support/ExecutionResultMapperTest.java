package org.opentripplanner.apis.transmodel.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.utils.lang.StringUtils.quoteReplace;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.junit.jupiter.api.Test;

class ExecutionResultMapperTest {

  private static ExecutionResult OK_RESULT_WITH_DATA_AND_ERROR =
    ExecutionResult.newExecutionResult()
      .data("Test")
      .addError(GraphQLError.newError().message("Error").build())
      .build();

  private static String RESULT_SERIALIZED = quoteReplace(
    "{" +
    "'errors':[" +
    "{'message':'Error','locations':[],'extensions':{'classification':'DataFetchingException'}}" +
    "]," +
    "'data':'Test'" +
    "}"
  );

  private static String TIMEOUT_RESPONSE = quoteReplace(
    "{" +
    "'errors':[{" +
    "'message':'TIMEOUT! The request is too resource intensive.'," +
    "'locations':[]," +
    "'extensions':{'classification':'ApiProcessingTimeout'}" +
    "}]" +
    "}"
  );

  public static final String TOO_LARGE_MESSAGE =
    "The number of fields in the GraphQL result exceeds the maximum allowed: 100000";

  private static final String TOO_LARGE_RESPONSE = quoteReplace(
    "{'" +
    "errors':[{" +
    "'message':'" +
    TOO_LARGE_MESSAGE +
    "'," +
    "'locations':[]," +
    "'extensions':{'classification':'ResponseTooLarge'}" +
    "}]" +
    "}"
  );

  public static final String SYSTEM_ERROR_MESSAGE = "A system error!";

  public static final String SYSTEM_ERROR_RESPONSE = quoteReplace(
    "{" +
    "'errors':[{" +
    "'message':'" +
    SYSTEM_ERROR_MESSAGE +
    "'," +
    "'locations':[]," +
    "'extensions':{'classification':'InternalServerError'}" +
    "}]" +
    "}"
  );

  @Test
  void okResponse() {
    var response = ExecutionResultMapper.okResponse(OK_RESULT_WITH_DATA_AND_ERROR);
    assertEquals(200, response.getStatus());
    assertEquals(RESULT_SERIALIZED, response.getEntity().toString());
  }

  @Test
  void timeoutResponse() {
    var response = ExecutionResultMapper.timeoutResponse();
    assertEquals(422, response.getStatus());
    assertEquals(TIMEOUT_RESPONSE, response.getEntity().toString());
  }

  @Test
  void tooLargeResponse() {
    var response = ExecutionResultMapper.tooLargeResponse(TOO_LARGE_MESSAGE);
    assertEquals(422, response.getStatus());
    assertEquals(TOO_LARGE_RESPONSE, response.getEntity().toString());
  }

  @Test
  void systemErrorResponse() {
    var response = ExecutionResultMapper.systemErrorResponse(SYSTEM_ERROR_MESSAGE);
    assertEquals(500, response.getStatus());
    assertEquals(SYSTEM_ERROR_RESPONSE, response.getEntity().toString());
  }
}
