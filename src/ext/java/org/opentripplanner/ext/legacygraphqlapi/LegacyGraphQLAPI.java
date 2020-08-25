package org.opentripplanner.ext.legacygraphqlapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{routerId}/index/graphql")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class LegacyGraphQLAPI {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(LegacyGraphQLAPI.class);

  private final Router router;
  private final ObjectMapper deserializer = new ObjectMapper();

  public LegacyGraphQLAPI(
      @Context OTPServer otpServer,
      @Context Providers providers,
      @PathParam("routerId") String routerId
  ) {
    this.router = otpServer.getRouter();

    ContextResolver<ObjectMapper> resolver =
        providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE);
    ObjectMapper mapper = resolver.getContext(ObjectMapper.class);
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
  }

  @POST
  @Path("/")
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
        : router.defaultRoutingRequest.locale;

    String query = (String) queryParameters.get("query");
    Object queryVariables = queryParameters.getOrDefault("variables", null);
    String operationName = (String) queryParameters.getOrDefault("operationName", null);
    Map<String, Object> variables;
    if (queryVariables instanceof Map) {
      variables = (Map) queryVariables;
    }
    else if (queryVariables instanceof String && !((String) queryVariables).isEmpty()) {
      try {
        variables = deserializer.readValue((String) queryVariables, Map.class);
      }
      catch (IOException e) {
        return Response
            .status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity("Variables must be a valid json object")
            .build();
      }
    }
    else {
      variables = new HashMap<>();
    }
    return LegacyGraphQLIndex.getGraphQLResponse(
        query,
        router,
        variables,
        operationName,
        maxResolves,
        timeout,
        locale
    );
  }

  @POST
  @Path("/")
  @Consumes("application/graphql")
  public Response getGraphQL(
      String query,
      @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
      @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
      @Context HttpHeaders headers
  ) {
    Locale locale = headers.getAcceptableLanguages().size() > 0
        ? headers.getAcceptableLanguages().get(0)
        : router.defaultRoutingRequest.locale;
    return LegacyGraphQLIndex.getGraphQLResponse(
        query,
        router,
        null,
        null,
        maxResolves,
        timeout,
        locale);
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
    List<Map<String, Object>> responses = new ArrayList<>();
    List<Callable<Map>> futures = new ArrayList();

    Locale locale = headers.getAcceptableLanguages().size() > 0
        ? headers.getAcceptableLanguages().get(0)
        : router.defaultRoutingRequest.locale;

    for (HashMap<String, Object> query : queries) {
      Map<String, Object> variables;
      if (query.get("variables") instanceof Map) {
        variables = (Map) query.get("variables");
      }
      else if (query.get("variables") instanceof String
          && ((String) query.get("variables")).length() > 0) {
        try {
          variables = deserializer.readValue((String) query.get("variables"), Map.class);
        }
        catch (IOException e) {
          return Response
              .status(Response.Status.BAD_REQUEST)
              .type(MediaType.TEXT_PLAIN_TYPE)
              .entity("Variables must be a valid json object")
              .build();
        }
      }
      else {
        variables = null;
      }
      String operationName = (String) query.getOrDefault("operationName", null);

      futures.add(() -> LegacyGraphQLIndex.getGraphQLExecutionResult((String) query.get("query"),
          router,
          variables,
          operationName,
          maxResolves,
          timeout,
          locale
      ));
    }

    try {
      List<Future<Map>> results = LegacyGraphQLIndex.threadPool.invokeAll(futures);

      for (int i = 0; i < queries.size(); i++) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("id", queries.get(i).get("id"));
        response.put("payload", results.get(i).get());
        responses.add(response);
      }
    }
    catch (CancellationException | ExecutionException | InterruptedException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.status(Response.Status.OK).entity(responses).build();
  }
}
