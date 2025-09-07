package org.opentripplanner.apis.gtfs.datafetchers;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;

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
class DataFetchingSupport {

  static DataFetchingEnvironment dataFetchingEnvironment(Object source) {
    return dataFetchingEnvironment(source, Map.of());
  }

  static DataFetchingEnvironment dataFetchingEnvironment(
    Object source,
    Map<String, Object> arguments
  ) {
    return dataFetchingEnvironment(
      source,
      arguments,
      new DefaultTransitService(new TimetableRepository())
    );
  }

  static DataFetchingEnvironment dataFetchingEnvironment(
    Object source,
    Map<String, Object> arguments,
    TransitService service
  ) {
    var executionContext = newExecutionContextBuilder()
      .executionId(ExecutionId.from("test"))
      .build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .source(source)
      .arguments(arguments)
      .context(new GraphQLRequestContext(null, service, null, null, null, null, null, null, null))
      .build();
  }
}
