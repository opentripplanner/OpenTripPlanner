package org.opentripplanner.api.thrift.util;

import java.util.List;

import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.api.thrift.definition.TripPaths;
import org.opentripplanner.routing.spt.GraphPath;

/**
 * Extends the Thrift TripPaths
 * 
 * @author avi
 * 
 */
public class TripPathsExtension extends TripPaths {

    /**
     * Required for serialization.
     */
    private static final long serialVersionUID = 3024640775481728306L;

    /**
     * Construct from a list of GraphPaths.
     * 
     * @param paths
     */
    public TripPathsExtension(List<GraphPath> paths) {
        super();

        if (paths == null || paths.size() == 0) {
            setNo_paths_found(true);
        } else {
            setNo_paths_found(false);
            for (GraphPath path : paths) {
                addToPaths(new PathExtension(path));
            }
        }
    }

    /**
     * Construct from TripParameters and a list of GraphPaths.
     * 
     * @param trip
     * @param paths
     */
    public TripPathsExtension(TripParameters trip, List<GraphPath> paths) {
        this(paths);
        setTrip(trip);
    }
}