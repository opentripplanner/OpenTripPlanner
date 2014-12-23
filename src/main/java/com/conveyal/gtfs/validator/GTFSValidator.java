package com.conveyal.gtfs.validator;

import com.conveyal.gtfs.GTFSFeed;

/**
 * A GTFSValidator examines a GTFS feed in the form of a set of maps, one map per table in the GTFS feed.
 * It adds error messages to that feed, optionally repairing the problems it encounters.
 */
public abstract class GTFSValidator {

    /**
     * The main extension point.
     * @param feed the feed to validate and optionally repair
     * @param repair if this is true, repair any errors encountered
     * @return whether any errors were encountered
     */
    public abstract boolean validate (GTFSFeed feed, boolean repair);

    // TODO return errors themselves?

}
