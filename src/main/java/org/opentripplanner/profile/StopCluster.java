package org.opentripplanner.profile;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Lists;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Groups stops by geographic proximity and name similarity.
 * This will at least half the number of distinct stop places. In profile routing this means a lot less branching
 * and a lot less transfers to consider.
 *
 * It seems to work quite well for both the Washington DC region and Portland. Locations outside the US would require
 * additional stop name normalizer modules.
 */
public class StopCluster {

    private static final Logger LOG = LoggerFactory.getLogger(StopCluster.class);

    public final String id;
    public final String name;
    public double lon;
    public double lat;
    public final List<Stop> children = Lists.newArrayList();

    public StopCluster(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void computeCenter() {
        double lonSum = 0, latSum = 0;
        for (Stop stop : children) {
            lonSum += stop.getLon();
            latSum += stop.getLat();
        }
        lon = lonSum / children.size();
        lat = latSum / children.size();
    }

    @Override
    public String toString() {
        return name;
    }

}
