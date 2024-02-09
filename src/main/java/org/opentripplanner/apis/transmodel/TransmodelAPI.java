package org.opentripplanner.apis.transmodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.BadRequestException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/transmodel/v3")
@Produces(MediaType.APPLICATION_JSON)
public class TransmodelAPI {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelAPI.class);

  private static GraphQLSchema schema;
  private static Collection<String> tracingHeaderTags;

  private final OtpServerRequestContext serverContext;
  private final TransmodelGraph index;
  private final ObjectMapper deserializer = new ObjectMapper();

  public TransmodelAPI(@Context OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.index = new TransmodelGraph(schema);
  }

  /**
   * This class is only here for backwards-compatibility. It will be removed in the future.
   */
  @Path("/routers/{ignoreRouterId}/transmodel/index/graphql")
  public static class TransmodelAPIOldPath extends TransmodelAPI {

    public TransmodelAPIOldPath(
      @Context OtpServerRequestContext serverContext,
      @PathParam("ignoreRouterId") String ignore
    ) {
      super(serverContext);
    }
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

  @POST
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

    if (!(queryParameters.get("query") instanceof String query)) {
      throw new BadRequestException("Invalid format for query");
    }

    Object queryVariables = queryParameters.getOrDefault("variables", null);
    Map<String, Object> variables;
    if (queryVariables instanceof Map queryVariablesAsMap) {
      variables = queryVariablesAsMap;
    } else if (
      queryVariables instanceof String queryVariablesAsString && !queryVariablesAsString.isEmpty()
    ) {
      try {
        variables = deserializer.readValue(queryVariablesAsString, Map.class);
      } catch (IOException e) {
        throw new BadRequestException("Variables must be a valid json object");
      }
    } else {
      variables = Collections.emptyMap();
    }
    String operationName = (String) queryParameters.getOrDefault("operationName", null);
    return index.executeGraphQL(
      query,
      serverContext,
      variables,
      operationName,
      maxResolves,
      getTagsFromHeaders(headers)
    );
  }

  @POST
  @Consumes("application/graphql")
  public Response getGraphQL(
    String query,
    @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
    @Context HttpHeaders headers
  ) {
    return index.executeGraphQL(
      query,
      serverContext,
      null,
      null,
      maxResolves,
      getTagsFromHeaders(headers)
    );
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
