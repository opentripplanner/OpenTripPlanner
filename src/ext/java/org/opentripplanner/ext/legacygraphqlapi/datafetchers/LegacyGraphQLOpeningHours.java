package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;

public record LegacyGraphQLOpeningHours(OHCalendar cal)
  implements LegacyGraphQLDataFetchers.LegacyGraphQLOpeningHours {
  @Override
  public DataFetcher<Iterable<Object>> dates() {
    return env -> getSource(env).openingHours();
  }

  @Override
  public DataFetcher<String> osm() {
    return environment -> getSource(environment).osmFormat();
  }

  private OHCalendar getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
