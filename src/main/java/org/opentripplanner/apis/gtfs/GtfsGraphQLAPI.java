package org.opentripplanner.apis.gtfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.opentripplanner.framework.graphql.GraphQLResponseSerializer;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/routers/{ignoreRouterId}/index/graphql")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class GtfsGraphQLAPI {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(GtfsGraphQLAPI.class);

  private final OtpServerRequestContext serverContext;
  private final ObjectMapper deserializer = new ObjectMapper();

  public GtfsGraphQLAPI(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getGraphQL(
    HashMap<String, Object> queryParameters,
    @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    if (queryParameters == null || !queryParameters.containsKey("query")) {
      LOG.debug("No query found in body");
      return Response
        .status(Response.Status.BAD_REQUEST)
        .type(MediaType.TEXT_PLAIN_TYPE)
        .entity("No query found in body")
        .build();
    }

    Locale locale = headers.getAcceptableLanguages().size() > 0
      ? headers.getAcceptableLanguages().get(0)
      : serverContext.defaultLocale();

    String query = (String) queryParameters.get("query");
    Object queryVariables = queryParameters.getOrDefault("variables", null);
    String operationName = (String) queryParameters.getOrDefault("operationName", null);
    Map<String, Object> variables;

    if (queryVariables instanceof Map) {
      variables = (Map) queryVariables;
    } else if (queryVariables instanceof String && !((String) queryVariables).isEmpty()) {
      try {
        variables = deserializer.readValue((String) queryVariables, Map.class);
      } catch (IOException e) {
        return Response
          .status(Response.Status.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN_TYPE)
          .entity("Variables must be a valid json object")
          .build();
      }
    } else {
      variables = new HashMap<>();
    }
    return GtfsGraphQLIndex.getGraphQLResponse(
      query,
      variables,
      operationName,
      maxResolves,
      timeout,
      locale,
      GraphQLRequestContext.ofServerContext(serverContext)
    );
  }

  @POST
  @Consumes("application/graphql")
  public Response getGraphQL(
    String query,
    @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    Locale locale = headers.getAcceptableLanguages().size() > 0
      ? headers.getAcceptableLanguages().get(0)
      : serverContext.defaultLocale();
    return GtfsGraphQLIndex.getGraphQLResponse(
      query,
      null,
      null,
      maxResolves,
      timeout,
      locale,
      GraphQLRequestContext.ofServerContext(serverContext)
    );
  }

  @POST
  @Path("/batch")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getGraphQLBatch(
    List<HashMap<String, Object>> queries,
    @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    List<Callable<ExecutionResult>> futures = new ArrayList<>();
    Locale locale = headers.getAcceptableLanguages().size() > 0
      ? headers.getAcceptableLanguages().get(0)
      : serverContext.defaultLocale();

    for (HashMap<String, Object> query : queries) {
      Map<String, Object> variables;
      if (query.get("variables") instanceof Map) {
        variables = (Map) query.get("variables");
      } else if (
        query.get("variables") instanceof String && ((String) query.get("variables")).length() > 0
      ) {
        try {
          variables = deserializer.readValue((String) query.get("variables"), Map.class);
        } catch (IOException e) {
          return Response
            .status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity("Variables must be a valid json object")
            .build();
        }
      } else {
        variables = null;
      }
      String operationName = (String) query.getOrDefault("operationName", null);

      futures.add(() ->
        GtfsGraphQLIndex.getGraphQLExecutionResult(
          (String) query.get("query"),
          variables,
          operationName,
          maxResolves,
          timeout,
          locale,
          GraphQLRequestContext.ofServerContext(serverContext)
        )
      );
    }

    try {
      List<Future<ExecutionResult>> results = GtfsGraphQLIndex.threadPool.invokeAll(futures);
      return Response
        .status(Response.Status.OK)
        .entity(GraphQLResponseSerializer.serializeBatch(queries, results))
        .build();
    } catch (InterruptedException e) {
      LOG.error("Batch query interrupted", e);
      throw new RuntimeException(e);
    }
  }
}
