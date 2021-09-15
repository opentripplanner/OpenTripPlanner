package org.opentripplanner.ext.transmodelapi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TransmodelGraph {

    static final Logger LOG = LoggerFactory.getLogger(TransmodelGraph.class);

    private final GraphQLSchema indexSchema;

    final ExecutorService threadPool;

    TransmodelGraph(GraphQLSchema schema) {
        this.threadPool = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-%d").build()
        );
        this.indexSchema = schema;
    }

    ExecutionResult getGraphQLExecutionResult(
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
        return graphQL.execute(executionInput);
    }

    Response getGraphQLResponse(String query, Router router, Map<String, Object> variables, String operationName, int maxResolves) {
        ExecutionResult result = getGraphQLExecutionResult(query, router, variables, operationName, maxResolves);
        return Response.status(Response.Status.OK).entity(GraphQLResponseSerializer.serialize(result)).build();
    }
}
