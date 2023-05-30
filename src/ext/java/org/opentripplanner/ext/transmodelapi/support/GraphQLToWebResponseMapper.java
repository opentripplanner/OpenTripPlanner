package org.opentripplanner.ext.transmodelapi.support;

import jakarta.ws.rs.core.Response;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.framework.http.OtpHttpStatus;

/**
 * Map from a GraphQL ExecutionResult to a web Response
 */
public class GraphQLToWebResponseMapper {

  public static Response map(OtpExecutionResult result) {
    if (result.timeout()) {
      return Response
        .status(OtpHttpStatus.STATUS_UNPROCESSABLE_ENTITY.statusCode())
        .entity(GraphQLResponseSerializer.serialize(result.result()))
        .build();
    }
    // Default - OK
    return Response.ok(GraphQLResponseSerializer.serialize(result.result())).build();
  }
}
