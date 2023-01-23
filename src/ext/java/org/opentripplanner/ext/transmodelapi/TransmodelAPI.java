package org.opentripplanner.ext.transmodelapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{ignoreRouterId}/transmodel/index")
// It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class TransmodelAPI {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TransmodelAPI.class);

  private static GraphQLSchema schema;
  private static Collection<String> tracingHeaderTags;

  private final OtpServerRequestContext serverContext;
  private final TransmodelGraph index;
  private final ObjectMapper deserializer = new ObjectMapper();

  public TransmodelAPI(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
    this.index = new TransmodelGraph(schema);
  }

  /**
   * This method should be called BEFORE the Web-Container is started and load new instances of this
   * class. This is a hack, and it would be better if the configuration was done more explicit and
   * enforced, not relaying on a "static" setup method to be called.
   */
  public static void setUp(
    TransmodelAPIParameters config,
    TransitModel transitModel,
    RouteRequest defaultRouteRequest
  ) {
    if (config.hideFeedId()) {
      TransitIdMapper.setupFixedFeedId(transitModel.getAgencies());
    }
    tracingHeaderTags = config.tracingHeaderTags();
    GqlUtil gqlUtil = new GqlUtil(transitModel.getTimeZone());
    schema = TransmodelGraphQLSchema.create(defaultRouteRequest, gqlUtil);
  }

  /**
   * Return 200 when service is loaded.
   */
  @GET
  @Path("/live")
  public Response isAlive() {
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @POST
  @Path("/graphql")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getGraphQL(
    HashMap<String, Object> queryParameters,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    if (queryParameters == null || !queryParameters.containsKey("query")) {
      LOG.debug("No query found in body");
      throw new BadRequestException("No query found in body");
    }

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
        throw new BadRequestException("Variables must be a valid json object");
      }
    } else {
      variables = new HashMap<>();
    }
    return index.getGraphQLResponse(
      query,
      serverContext,
      variables,
      operationName,
      maxResolves,
      getTagsFromHeaders(headers)
    );
  }

  @POST
  @Path("/graphql")
  @Consumes("application/graphql")
  public Response getGraphQL(
    String query,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    return index.getGraphQLResponse(
      query,
      serverContext,
      null,
      null,
      maxResolves,
      getTagsFromHeaders(headers)
    );
  }

  @POST
  @Path("/graphql/batch")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getGraphQLBatch(
    List<HashMap<String, Object>> queries,
    @HeaderParam("OTPTimeout") @DefaultValue("10000") int timeout,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    List<Callable<ExecutionResult>> futures = new ArrayList<>();

    for (Map<String, Object> query : queries) {
      Map<String, Object> variables;
      if (query.get("variables") instanceof Map) {
        variables = (Map) query.get("variables");
      } else if (
        query.get("variables") instanceof String && ((String) query.get("variables")).length() > 0
      ) {
        try {
          variables = deserializer.readValue((String) query.get("variables"), Map.class);
        } catch (IOException e) {
          throw new BadRequestException("Variables must be a valid json object");
        }
      } else {
        variables = null;
      }
      String operationName = (String) query.getOrDefault("operationName", null);

      futures.add(() ->
        index.getGraphQLExecutionResult(
          (String) query.get("query"),
          serverContext,
          variables,
          operationName,
          maxResolves,
          getTagsFromHeaders(headers)
        )
      );
    }

    try {
      List<Future<ExecutionResult>> results = index.threadPool.invokeAll(futures);
      return Response
        .status(Response.Status.OK)
        .entity(GraphQLResponseSerializer.serializeBatch(queries, results))
        .build();
    } catch (InterruptedException e) {
      LOG.error("Batch query interrupted", e);
      throw new RuntimeException(e);
    }
  }

  private static Iterable<Tag> getTagsFromHeaders(HttpHeaders headers) {
    return tracingHeaderTags
      .stream()
      .map(header -> {
        String value = headers.getHeaderString(header);
        return Tag.of(header, value == null ? "__UNKNOWN__" : value);
      })
      .collect(Collectors.toList());
  }
}
