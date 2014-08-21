package org.opentripplanner.profile;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import jersey.repackaged.com.google.common.collect.Lists;
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
    private static final int CLUSTER_RADIUS = 400; // meters

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

    /**
     * I'm seeing Stops with no agency id and other annoyances.
     * Therefore we will just group without using any GTFS mess, into string-named groups.
     * FIXME OBA parentStation field is a string, not an AgencyAndId, so it has no agency/feed scope
     * But the DC regional graph has no parent stations pre-defined, so no use dealing with them for now.
     *
     * We can't use a similarity comparison, we need exact matches. This is because many street names differ by only
     * one letter or number, e.g. 34th and 35th or Avenue A and Avenue B.
     * Therefore normalizing the names before the comparison is essential.
     * The agency must provide either parent station information or a well thought out stop naming scheme to cluster
     * stops -- no guessing is reasonable without that information.
     */
    public static List<StopCluster> clusterStops(GraphIndex gidx) {
        List<StopCluster> clusters = Lists.newArrayList();
        int psIdx = 0; // unique index for next parent stop
        LOG.info("Clustering stops by geographic and name proximity...");
        // Each stop without a parent station will greedily claim other stops without a parent station.
        Map<String, String> descriptionForStationId = Maps.newHashMap();
        for (Stop s0 : gidx.stopForId.values()) {
            s0.setParentStation(null); // FIXME some trimet stops have "landmark" parent stations, clear them
        }
        for (Stop s0 : gidx.stopForId.values()) {
            if (s0.getParentStation() != null) continue; // skip stops that have already been claimed
            String s0normalizedName = StopNameNormalizer.normalize(s0.getName());
            StopCluster cluster = new StopCluster(String.format("C%03d", psIdx++), s0normalizedName);
            // LOG.info("stop {}", s0normalizedName);
            // No need to explicitly add s0 to the cluster. It will be found in the spatial index query below.
            for (TransitStop ts1 : gidx.stopSpatialIndex.query(s0.getLon(), s0.getLat(), CLUSTER_RADIUS)) {
                Stop s1 = ts1.getStop();
                double geoDistance = SphericalDistanceLibrary.getInstance().fastDistance(s0.getLat(), s0.getLon(), s1.getLat(), s1.getLon());
                if (geoDistance < CLUSTER_RADIUS) {
                    String s1normalizedName = StopNameNormalizer.normalize(s1.getName());
                    // LOG.info("   --> {}", s1normalizedName);
                    // LOG.info("       geodist {} stringdist {}", geoDistance, stringDistance);
                    if (s1normalizedName.equals(s0normalizedName)) {
                        cluster.children.add(s1);
                        s1.setParentStation(cluster.id);
                    }
                }
            }
            cluster.computeCenter();
            clusters.add(cluster);
        }
//        LOG.info("Done clustering stops.");
//        for (StopCluster cluster : clusters) {
//            LOG.info("{} at {} {}", cluster.name, cluster.lat, cluster.lon);
//            for (Stop stop : cluster.children) {
//                LOG.info("   {}", stop.getName());
//            }
//        }
        return clusters;
    }

}
