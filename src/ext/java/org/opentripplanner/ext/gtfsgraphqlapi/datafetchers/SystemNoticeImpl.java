package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.model.SystemNotice;

public class SystemNoticeImpl implements GraphQLDataFetchers.GraphQLSystemNotice {

  @Override
  public DataFetcher<String> tag() {
    return environment -> getSource(environment).tag;
  }

  @Override
  public DataFetcher<String> text() {
    return environment -> getSource(environment).text;
  }

  private SystemNotice getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
