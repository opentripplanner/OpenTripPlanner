package org.opentripplanner.api.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.execution.AbortExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class is responsible for serializing a GraphQL {@link ExecutionResult} into a String, which can be returned as 
 * the body of the HTTP response. This differs from the mapper provided by {@link JSONObjectMapperProvider}, by 
 * serializing all fields in the objects, including null fields.
 */
public class GraphQLResponseSerializer {
    static final Logger LOG = LoggerFactory.getLogger(GraphQLResponseSerializer.class);

    static private final ObjectMapper objectMapper = new ObjectMapper();

    static public String serialize(ExecutionResult executionResult) {
        try {
            return objectMapper.writeValueAsString(executionResult.toSpecification());
        } catch (JsonProcessingException e) {
            LOG.error("Unable to serialize response", e);
            throw new RuntimeException(e);
        }
    }

    static public String serializeBatch(List<HashMap<String, Object>> queries, List<Future<ExecutionResult>> futures) {
        List<Map<String, Object>> responses = new LinkedList<>();
        for (int i = 0; i < queries.size(); i++) {
            ExecutionResult executionResult;
            // Try each request separately, returning both completed and failed responses is ok
            try {
                executionResult = futures.get(i).get();
            } catch (InterruptedException | ExecutionException e) {
                executionResult = new AbortExecutionException(e).toExecutionResult();
            }
            responses.add(Map.of(
                    "id", queries.get(i).get("id"),
                    "payload", executionResult.toSpecification()
            ));
        }

        try {
            return objectMapper.writeValueAsString(responses);
        } catch (JsonProcessingException e) {
            LOG.error("Unable to serialize response", e);
            throw new RuntimeException(e);
        }
    }
}
