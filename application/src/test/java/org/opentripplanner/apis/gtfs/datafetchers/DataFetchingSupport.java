package org.opentripplanner.apis.gtfs.datafetchers;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;

import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;

/**
 * Support class for building data fetching environments for testing data fetchers.
 */
class DataFetchingSupport {

  static DataFetchingEnvironment dataFetchingEnvironment(Object source) {
    return dataFetchingEnvironment(source, Map.of());
  }

  static DataFetchingEnvironment dataFetchingEnvironment(
    Object source,
    Map<String, Object> arguments
  ) {
    var executionContext = newExecutionContextBuilder()
      .executionId(ExecutionId.from("test"))
      .build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .source(source)
      .arguments(arguments)
      .build();
  }
}
