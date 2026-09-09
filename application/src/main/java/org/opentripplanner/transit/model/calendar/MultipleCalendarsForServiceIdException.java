/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.calendar;

import org.opentripplanner.core.model.id.FeedScopedId;

class MultipleCalendarsForServiceIdException extends RuntimeException {

  MultipleCalendarsForServiceIdException(FeedScopedId serviceId) {
    super("multiple calendars found for serviceId=" + serviceId);
  }
}
