package org.opentripplanner.graph_builder.module;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NoFutureDates;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * This validator creates warnings for feeds that don't have any transit on future dates.
 */
public class TransitWithFutureDateValidator {

  public static void validate(
    CalendarServiceData data,
    DataImportIssueStore issueStore,
    ZoneId timeZone
  ) {
    Instant now = Instant.now();
    HashSet<String> agenciesWithFutureDates = new HashSet<>();
    HashSet<String> agencies = new HashSet<>();

    for (FeedScopedId sid : data.getServiceIds()) {
      agencies.add(sid.getFeedId());
      for (LocalDate sd : data.getServiceDatesForServiceId(sid)) {
        var t = ServiceDateUtils.asStartOfService(sd, timeZone).toInstant();
        if (t.isAfter(now)) {
          agenciesWithFutureDates.add(sid.getFeedId());
        }
      }
    }
    for (String agency : agencies) {
      if (!agenciesWithFutureDates.contains(agency)) {
        issueStore.add(new NoFutureDates(agency));
      }
    }
  }
}
