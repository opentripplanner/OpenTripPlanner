package org.opentripplanner.apis.support.graphql;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionContextBuilder;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;

/**
 * Support class for building data fetching environments for testing data fetchers.
 */
public class DataFetchingSupport {

  public static DataFetchingEnvironment dataFetchingEnvironment(Object source) {
    return dataFetchingEnvironment(source, Map.of());
  }

  public static DataFetchingEnvironment dataFetchingEnvironment(
    Object source,
    Map<String, Object> arguments
  ) {
    return dataFetchingEnvironment(
      source,
      arguments,
      new DefaultTransitService(new TimetableRepository())
    );
  }

  public static DataFetchingEnvironment dataFetchingEnvironment(
    Object source,
    Map<String, Object> arguments,
    TransitService service
  ) {
    final var executionContext = executionContext();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .source(source)
      .arguments(arguments)
      .context(
        new GraphQLRequestContext(null, service, null, null, null, null, null, null, null, null)
      )
      .build();
  }

  public static ExecutionContext executionContext() {
    return executionContextBuilder().build();
  }

  public static ExecutionContext executionContext(ExecutionInput executionInput) {
    return executionContextBuilder().executionInput(executionInput).build();
  }

  private static ExecutionContextBuilder executionContextBuilder() {
    return newExecutionContextBuilder()
      .executionId(ExecutionId.from("test"))
      .graphQLContext(GraphQLContext.getDefault());
  }
}
