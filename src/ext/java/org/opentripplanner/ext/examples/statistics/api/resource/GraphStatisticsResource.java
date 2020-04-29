package org.opentripplanner.ext.examples.statistics.api.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.opentripplanner.ext.examples.statistics.api.model.StatisticsGraphQLSchemaFactory;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.util.HttpToGraphQLMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.opentripplanner.util.HttpToGraphQLMapper.mapExecutionResultToHttpResponse;
import static org.opentripplanner.util.HttpToGraphQLMapper.mapHttpQuerryParamsToQLParams;

@Path("/routers/statistics")
@Produces(MediaType.APPLICATION_JSON)
public class GraphStatisticsResource {

    private final ObjectMapper deserializer = new ObjectMapper();
    private GraphQL graphQL;

    @SuppressWarnings("unused")
    public GraphStatisticsResource(@Context OTPServer server) {
        this(server.createRoutingRequestService());
    }

    GraphStatisticsResource(RoutingService routingService) {
        this.graphQL = new GraphQL(StatisticsGraphQLSchemaFactory.createSchema(routingService));
    }

    @POST @Path("/graphql")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphQLAsJson(Map<String, Object> queryParameters) {
        HttpToGraphQLMapper.QlRequestParams req = mapHttpQuerryParamsToQLParams(
                queryParameters, deserializer
        );

        ExecutionResult executionResult = graphQL.execute(
                req.query, req.operationName, null, req.variables
        );
        return mapExecutionResultToHttpResponse(executionResult);
    }

    @POST @Path("/graphql")
    @Consumes("application/graphql")
    public Response getGraphQL(String query) {
        ExecutionResult executionResult = graphQL.execute(query);
        return mapExecutionResultToHttpResponse(executionResult);
    }
}
