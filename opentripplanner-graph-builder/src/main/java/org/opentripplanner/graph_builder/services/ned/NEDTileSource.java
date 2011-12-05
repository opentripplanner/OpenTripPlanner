package org.opentripplanner.graph_builder.services.ned;

import java.io.File;
import java.util.List;

import org.opentripplanner.routing.core.Graph;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A source of NED tiles for NEDGridCoverageFactoryImpl -- maybe the USGS streaming
 * server, maybe one-degree tiles, maybe something else.
 * @author novalis
 *
 */
public interface NEDTileSource {

    @Autowired
    public abstract void setGraph(Graph graph);

    /**
     * The cache directory stores NED tiles.  It is crucial that this be somewhere permanent
     * with plenty of disk space.  Don't use /tmp -- the downloading process takes a long time
     * and you don't want to repeat it if at all possible.
     * @param cacheDirectory
     */
    public abstract void setCacheDirectory(File cacheDirectory);

    /**
     * Download all the NED tiles into the cache.
     */
    public abstract List<File> getNEDTiles();

}