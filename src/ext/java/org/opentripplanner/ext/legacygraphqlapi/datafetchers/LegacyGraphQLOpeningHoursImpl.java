package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.model.calendar.openinghours.OsmOpeningHoursSupport;

public final class LegacyGraphQLOpeningHoursImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLOpeningHours {

  private final OHCalendar cal;

  public LegacyGraphQLOpeningHoursImpl(OHCalendar cal) {
    this.cal = cal;
  }

  @Override
  public DataFetcher<Iterable<Object>> dates() {
    //TODO: implement
    return env -> null;
  }

  @Override
  public DataFetcher<String> osm() {
    return environment -> OsmOpeningHoursSupport.osmFormat(getSource(environment));
  }

  private OHCalendar getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
