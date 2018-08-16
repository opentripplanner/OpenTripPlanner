/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.impl;

import org.opentripplanner.model.FeedId;

public class MultipleCalendarsForServiceIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MultipleCalendarsForServiceIdException(FeedId serviceId) {
        super("multiple calendars found for serviceId=" + serviceId);
    }
}
