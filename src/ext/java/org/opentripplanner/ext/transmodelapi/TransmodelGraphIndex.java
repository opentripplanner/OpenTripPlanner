package org.opentripplanner.ext.transmodelapi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class TransmodelGraphIndex {

    static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphIndex.class);

    private final GraphQLSchema indexSchema;

    final ExecutorService threadPool;

    TransmodelGraphIndex(Graph graph, RoutingRequest defaultRoutingRequest) {
        threadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-%d").build()
        );
        indexSchema = new TransmodelIndexGraphQLSchema(graph, defaultRoutingRequest).indexSchema;
    }

    HashMap<String, Object> getGraphQLExecutionResult(
            String query,
            Router router,
            Map<String, Object> variables,
            String operationName,
            int maxResolves
    ) {
        MaxQueryComplexityInstrumentation instrumentation = new MaxQueryComplexityInstrumentation(maxResolves);
        GraphQL graphQL = GraphQL.newGraphQL(indexSchema).instrumentation(instrumentation).build();

        if (variables == null) {
            variables = new HashMap<>();
        }

        TransmodelRequestContext transmodelRequestContext =
            new TransmodelRequestContext(router, new RoutingService(router.graph));

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                                                .query(query)
                                                .operationName(operationName)
                                                .context(transmodelRequestContext)
                                                .root(router)
                                                .variables(variables)
                                                .build();
        HashMap<String, Object> content = new HashMap<>();
        ExecutionResult executionResult;
        try {
            executionResult = graphQL.execute(executionInput);
            if (!executionResult.getErrors().isEmpty()) {
                content.put("errors", mapErrors(executionResult.getErrors()));
            }
            if (executionResult.getData() != null) {
                content.put("data", executionResult.getData());
            }
        } catch (RuntimeException ge) {
            LOG.warn("Exception during graphQL.execute: " + ge.getMessage(), ge);
            content.put("errors", mapErrors(Arrays.asList(ge)));
        }
        return content;
    }

    Response getGraphQLResponse(String query, Router router, Map<String, Object> variables, String operationName, int maxResolves) {
        Response.ResponseBuilder res = Response.status(Response.Status.OK);
        HashMap<String, Object> content = getGraphQLExecutionResult(
                query, router, variables, operationName, maxResolves
        );
        return res.entity(content).build();
    }

    private List<Map<String, Object>> mapErrors(Collection<?> errors) {
        return errors.stream().map(e -> {
            HashMap<String, Object> response = new HashMap<>();

            if (e instanceof GraphQLError) {
                GraphQLError graphQLError=(GraphQLError) e;
                response.put("message", graphQLError.getMessage());
                response.put("errorType", graphQLError.getErrorType());
                response.put("locations", graphQLError.getLocations());
                response.put("path", graphQLError.getPath());
            } else {
                if (e instanceof Exception) {
                    response.put("message", ((Exception) e).getMessage());
                }
                response.put("errorType", e.getClass().getSimpleName());
            }

            return response;
        }).collect(Collectors.toList());
    }

}
