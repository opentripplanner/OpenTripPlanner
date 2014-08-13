package org.opentripplanner.profile;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Groups stops by geographic proximity and name similarity.
 * This will at least half the number of distinct stop places. In profile routing this means a lot less branching
 * and a lot less transfers to consider.
 *
 * It seems to work quite well for both the Washington DC region and Portland. Locations outside the US would require
 * additional stop name normalizer modules.
 */
public class StopClusterer {

    private static final Logger LOG = LoggerFactory.getLogger(StopClusterer.class);

    private static final int CLUSTER_RADIUS = 400; // meters
    private GraphIndex gidx;
    Multimap<String, Stop> groupedStops = HashMultimap.create();

    // I'm seeing Stops with no agency id and other annoyances.
    // Therefore we will just group without using any GTFS mess, into string-named groups.
    // FIXME OBA parentStation field is a string, not an AgencyAndId, so it has no agency/feed scope
    // But the DC regional graph has no parent stations pre-defined, so no use dealing with them for now.

    // We can't use a similarity comparison, we need exact matches. This is because many street names differ by only
    // one letter or number, e.g. 34th and 35th or Avenue A and Avenue B.
    // Therefore normalizing the names before the comparison is essential.
    // The agency must provide either parent station information or a well thought out stop naming scheme to cluster
    // stops -- no guessing is reasonable without that information.

    public void clusterStops(GraphIndex gidx) {
        int psIdx = 0; // unique index for next parent stop
        this.gidx = gidx; // make graph index available to convenience functions
        LOG.info("Clustering stops by geographic and name proximity...");
        // Each stop without a parent station will greedily claim other stops without a parent station.
        Map<String, String> descriptionForStationId = Maps.newHashMap();
        for (Stop s0 : gidx.stopForId.values()) {
            if (s0.getParentStation() != null) continue; // skip stops that have already been claimed
            String s0normalizedName = StopNameNormalizer.normalize(s0.getName());
            String parentId = String.format("PS%03d", psIdx++);
            s0.setParentStation(parentId);
            groupedStops.put(parentId, s0);
            // LOG.info("stop {}", s0normalizedName);
            for (TransitStop ts1 : gidx.stopSpatialIndex.query(s0.getLon(), s0.getLat(), CLUSTER_RADIUS)) {
                Stop s1 = ts1.getStop();
                double geoDistance = SphericalDistanceLibrary.getInstance().fastDistance(s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
                if (geoDistance < CLUSTER_RADIUS) {
                    String s1normalizedName = StopNameNormalizer.normalize(s1.getName());
                    // LOG.info("   --> {}", s1normalizedName);
                    // LOG.info("       geodist {} stringdist {}", geoDistance, stringDistance);
                    if (s1normalizedName.equals(s0normalizedName)) {
                        s1.setParentStation(parentId);
                        groupedStops.put(parentId, s1);
                    }
                }
            }
        }
        LOG.info("Done clustering stops.");
        for (String parentStationId : groupedStops.keySet()) {
            LOG.info("{}", parentStationId);
            for (Stop stop : groupedStops.get(parentStationId)) {
                LOG.info("   {}", stop.getName());
            }
        }
    }

}
