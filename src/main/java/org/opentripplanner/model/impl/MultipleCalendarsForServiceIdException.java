/* This file is copied from OneBusAway project. */
package org.opentripplanner.model.impl;

import org.opentripplanner.model.FeedId;

public class MultipleCalendarsForServiceIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MultipleCalendarsForServiceIdException(FeedId serviceId) {
        super("multiple calendars found for serviceId=" + serviceId);
    }
}
