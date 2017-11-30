package org.opentripplanner.common.walk;

import java.util.HashSet;
import java.util.Set;

/**
 * A single rule for use in calculating walk comfort scores. Maps an osm tag
 * key/value combination to a factor (relative to 1.0) that is used in computing
 * the score.
 *
 * Created by demory on 11/20/17.
 */

public class WalkComfortRule {

    /** the name (key) of an OSM tag to match **/
    private String tagKey;

    /** a set of one or more values to match **/
    private Set<String> values;

    /** the factor to apply to the composite comfort score if this rule matches a given key/value pair */
    private float factor;

    public WalkComfortRule(String osmKey, String value, float factor) {
        this.tagKey = osmKey;
        this.values = new HashSet<>();
        this.values.add(value);
        this.factor = factor;
    }


    public String getTagKey () { return this.tagKey; }

    public float getFactor () {
        return this.factor;
    }

    /** apply rule to given value; returns true if value matches **/
    public boolean apply (String val) {
        return values.contains(val);
    }

    /** computes factor for given value; returns this rule's factor if matches, or 1.0 if not */
    public float computeFactor (String val) {
        return this.apply(val) ? factor : 1.0f;
    }
}
