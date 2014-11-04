package com.conveyal.gtfs.validator;

import com.conveyal.gtfs.GTFSFeed;

/** The GTFS validator that will be used if no others are supplied. */
public class DefaultValidator extends GTFSValidator {

    @Override
    public boolean validate(GTFSFeed feed, boolean repair) {

        return false;
    }

}
