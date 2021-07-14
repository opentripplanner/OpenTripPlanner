package org.opentripplanner.updater.vehicle_rental.GBFSMappings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response class for the gbfs.json file.
 * See https://github.com/NABSA/gbfs/blob/master/gbfs.md#gbfsjson
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GbfsResponse extends BaseGtfsResponse {
    public Map<String, GbfsFeeds> data;

    public static class GbfsFeeds {
        public List<GbfsFeed> feeds;
    }

    public static class GbfsFeed {
        public String name;
        public String url;
    }
}
