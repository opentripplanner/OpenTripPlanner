package org.opentripplanner.updater.vehicle_rental.GBFSMappings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class for parsing base response info that will be present in all GBFS responses.
 * See https://github.com/NABSA/gbfs/blob/master/gbfs.md#output-format
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseGtfsResponse {
    public Integer last_updated;
    public Integer ttl;
}
