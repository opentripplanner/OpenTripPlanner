/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.calendar.build;

import org.opentripplanner.core.model.id.FeedScopedId;

public class MultipleCalendarsForServiceIdException extends RuntimeException {

  public MultipleCalendarsForServiceIdException(FeedScopedId serviceId) {
    super("multiple calendars found for serviceId=" + serviceId);
  }
}
