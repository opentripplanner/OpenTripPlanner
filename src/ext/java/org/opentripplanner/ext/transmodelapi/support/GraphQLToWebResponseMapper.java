package org.opentripplanner.ext.transmodelapi.support;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.framework.http.OtpHttpStatus;

/**
 * Map from a GraphQL ExecutionResult to a web Response
 */
public class GraphQLToWebResponseMapper {

  public static Response map(ExecutionResult result) {
    List<GraphQLError> errors = result.getErrors();
    if (errors.stream().anyMatch(OTPProcessingTimeoutGraphQLException.class::isInstance)) {
      return Response
        .status(OtpHttpStatus.STATUS_UNPROCESSABLE_ENTITY.statusCode())
        .entity(GraphQLResponseSerializer.serialize(result))
        .build();
    } else {
      return Response
        .status(Response.Status.OK)
        .entity(GraphQLResponseSerializer.serialize(result))
        .build();
    }
  }
}
