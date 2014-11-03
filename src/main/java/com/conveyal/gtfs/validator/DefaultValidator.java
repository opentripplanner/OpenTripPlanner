package com.conveyal.gtfs.validator;

import com.conveyal.gtfs.GTFSFeed;

/**
 *
 */
public class DefaultValidator extends GTFSValidator {

    @Override
    public boolean validate(GTFSFeed feed, boolean repair) {

        return false;
    }

}
