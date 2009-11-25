package org.opentripplanner.api.model;

import java.util.Date;
import java.util.logging.Logger;

/**
 *
 */
public class TimeDistance {

    protected static final Logger LOGGER = Logger.getLogger(TimeDistance.class.getCanonicalName());

    public Double duration = null;
    public Date start = null;
    public Date end = null;

    public Double walk = null;
    public Double transit = null;
    public Double waiting = null;

    public Integer transfers = null;
    public Integer legs = null;

    public TimeDistance() {
        duration = 1.1;
        walk = 1.2;
        transit = 1.2;
        legs = 2;
        transfers = 3;
        start = new Date();
        end = new Date();
    }
}
