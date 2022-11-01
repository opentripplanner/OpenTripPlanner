package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.TripPattern;

public class LegacyGraphQLStoptimesInPatternImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLStoptimesInPattern {

  @Override
  public DataFetcher<TripPattern> pattern() {
    return environment -> getSource(environment).pattern;
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stoptimes() {
    return environment -> getSource(environment).times;
  }

  private StopTimesInPattern getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
