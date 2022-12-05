/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar.impl;

import org.opentripplanner.transit.model.framework.FeedScopedId;

class MultipleCalendarsForServiceIdException extends RuntimeException {

  MultipleCalendarsForServiceIdException(FeedScopedId serviceId) {
    super("multiple calendars found for serviceId=" + serviceId);
  }
}
