package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.model.calendar.openinghours.OsmOpeningHoursSupport;

public class OpeningHoursImpl implements GraphQLDataFetchers.GraphQLOpeningHours {

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
