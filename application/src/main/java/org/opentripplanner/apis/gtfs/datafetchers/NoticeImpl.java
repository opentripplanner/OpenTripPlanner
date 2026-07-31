package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.basic.Notice;

public class NoticeImpl implements GraphQLDataFetchers.GraphQLNotice {

  @Override
  public DataFetcher<String> text() {
    return env -> source(env).text();
  }

  private static Notice source(DataFetchingEnvironment env) {
    return env.getSource();
  }
}
