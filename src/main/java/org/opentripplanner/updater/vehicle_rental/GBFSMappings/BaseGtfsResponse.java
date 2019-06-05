package org.opentripplanner.updater.vehicle_rental.GBFSMappings;

/**
 * Class for parsing base response info that will be present in all GBFS responses.
 * See https://github.com/NABSA/gbfs/blob/master/gbfs.md#output-format
 */
public class BaseGtfsResponse {
    public Integer last_updated;
    public Integer ttl;
}
