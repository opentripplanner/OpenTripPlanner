package org.opentripplanner.apis.gtfs.datafetchers;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;

import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;

class DataFetchingSupport {

  static DataFetchingEnvironment dataFetchingEnvironment(Object source) {
    var executionContext = newExecutionContextBuilder()
      .executionId(ExecutionId.from("test"))
      .build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .source(source)
      .build();
  }
}
