package com.conveyal.gtfs.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * This table does not exist in GTFS. It is a join of fare_attributes and fare_rules on fare_id.
 * There should only be one fare_attribute per fare_id, but there can be many fare_rules per fare_id.
 */
public class Fare {

    String         fare_id;
    FareAttribute  fare_attribute;
    List<FareRule> fare_rules = Lists.newArrayList();

    public Fare(String fare_id) {
        this.fare_id = fare_id;
    }

}
