package org.opentripplanner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpToGraphQLMapper {
    private static final Logger LOG = LoggerFactory.getLogger(HttpToGraphQLMapper.class);

    public static Response mapExecutionResultToHttpResponse(ExecutionResult executionResult) {

        Response.ResponseBuilder res = Response.status(Response.Status.OK);
        HashMap<String, Object> content = new HashMap<>();

        if (!executionResult.getErrors().isEmpty()) {
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            content.put("errors", executionResult.getErrors());
        }

        if (executionResult.getData() != null) {
            content.put("data", executionResult.getData());
        }

        return res.entity(content).build();
    }

    public static QlRequestParams mapHttpQuerryParamsToQLParams(Map<String, Object> queryParameters, ObjectMapper deserializer) {
        String query = (String) queryParameters.get("query");
        Object queryVariables = queryParameters.getOrDefault("variables", null);
        String operationName = (String) queryParameters.getOrDefault("operationName", null);
        Map<String, Object> variables;

        if (queryVariables instanceof Map) {
            //noinspection unchecked
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
        return new QlRequestParams(query, operationName, variables);
    }

    public static class QlRequestParams {
        public final String query ;
        public final String operationName;
        public final Map<String, Object> variables;

        private QlRequestParams(String query, String operationName, Map<String, Object> variables) {
            this.query = query;
            this.operationName = operationName;
            this.variables = variables;
        }
    }
}
