package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.model.calendar.openinghours.OsmOpeningHoursSupport;

public class LegacyGraphQLOpeningHoursImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLOpeningHours {

  @Override
  public DataFetcher<Iterable<Object>> dates() {
    //TODO: implement
    return env -> null;
  }

  @Override
  public DataFetcher<String> osm() {
    return environment -> {
      var cal = getSource(environment);
      if (cal == null) {
        return null;
      } else {
        return OsmOpeningHoursSupport.osmFormat(cal);
      }
    };
  }

  private OHCalendar getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
