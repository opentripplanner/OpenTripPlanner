package org.opentripplanner.apis.gtfs;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.apis.support.TracingUtils;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/gtfs/v1/")
@Produces(MediaType.APPLICATION_JSON)
public class GtfsGraphQLAPI {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsGraphQLAPI.class);

  private final OtpServerRequestContext serverContext;
  private final ObjectMapper deserializer = new ObjectMapper();

  public GtfsGraphQLAPI(@Context OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
  }

  /**
   * This class is only here for backwards-compatibility. It will be removed in the future.
   */
  @Path("/routers/{ignoreRouterId}/index/graphql")
  public static class GtfsGraphQLAPIOldPath extends GtfsGraphQLAPI {

    public GtfsGraphQLAPIOldPath(
      @Context OtpServerRequestContext serverContext,
      @PathParam("ignoreRouterId") String ignore
    ) {
      super(serverContext);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getGraphQL(
    HashMap<String, Object> jsonParameters,
    @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers,
    @Context UriInfo uriInfo
  ) {
    if (jsonParameters == null || !jsonParameters.containsKey("query")) {
      LOG.debug("No query found in body");
      return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.TEXT_PLAIN_TYPE)
        .entity("No query found in body")
        .build();
    }

    Locale locale = headers.getAcceptableLanguages().size() > 0
      ? headers.getAcceptableLanguages().get(0)
      : serverContext.defaultRouteRequest().preferences().locale();

    String query = (String) jsonParameters.get("query");
    Object queryVariables = jsonParameters.getOrDefault("variables", null);
    String operationName = (String) jsonParameters.getOrDefault("operationName", null);
    Map<String, Object> variables;

    if (queryVariables instanceof Map) {
      variables = (Map) queryVariables;
    } else if (queryVariables instanceof String && !((String) queryVariables).isEmpty()) {
      try {
        variables = deserializer.readValue((String) queryVariables, Map.class);
      } catch (IOException e) {
        return Response.status(Response.Status.BAD_REQUEST)
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
      GraphQLRequestContext.ofServerContext(serverContext),
      TracingUtils.findTagsInHeadersOrQueryParameters(
        serverContext.gtfsApiParameters().tracingTags(),
        headers,
        uriInfo.getQueryParameters()
      )
    );
  }

  @POST
  @Consumes("application/graphql")
  public Response getGraphQL(
    String query,
    @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers,
    @Context UriInfo uriInfo
  ) {
    Locale locale = headers.getAcceptableLanguages().size() > 0
      ? headers.getAcceptableLanguages().get(0)
      : serverContext.defaultRouteRequest().preferences().locale();
    return GtfsGraphQLIndex.getGraphQLResponse(
      query,
      null,
      null,
      maxResolves,
      timeout,
      locale,
      GraphQLRequestContext.ofServerContext(serverContext),
      TracingUtils.findTagsInHeadersOrQueryParameters(
        serverContext.gtfsApiParameters().tracingTags(),
        headers,
        uriInfo.getQueryParameters()
      )
    );
  }
}
