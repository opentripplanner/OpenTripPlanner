package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.StopCluster;

import java.util.Collection;
import java.util.List;

/**
 * A representation of a stop cluster for use in an index API response.
 * Can contain references to stops for more detail.
 */
public class StopClusterDetail {

    public String id;
    public String name;
    public double lat;
    public double lon;
    public List<StopShort> stops; // filled in only if detail is requested

    public StopClusterDetail (StopCluster cluster, boolean detail) {
        id = cluster.id;
        lat = cluster.lat;
        lon = cluster.lon;
        name = cluster.name;
        if (detail) {
            stops = Lists.newArrayList();
            for (Stop stop : cluster.children) {
                stops.add(new StopShort(stop));
            }
        }
    }

    public static List<StopClusterDetail> list (Collection<StopCluster> in, boolean detail) {
        List<StopClusterDetail> out = Lists.newArrayList();
        for (StopCluster cluster : in) out.add(new StopClusterDetail(cluster, detail));
        return out;
    }

}
