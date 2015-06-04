package org.opentripplanner.updater.traffic;

import com.beust.jcommander.internal.Maps;
import com.conveyal.traffic.data.ExchangeFormat;
import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.traffic.Segment;
import org.opentripplanner.traffic.SpeedSample;
import org.opentripplanner.traffic.StreetSpeedSource;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Update the graph with traffic data from OpenTraffic.
 */
public class OpenTrafficUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTrafficUpdater.class);

    private Graph graph;
    private GraphUpdaterManager graphUpdaterManager;

    /** the tile directory to search through */
    private File tileDirectory;

    private boolean hasAlreadyRun = false;

    @Override
    protected void runPolling() throws Exception {
        if (hasAlreadyRun) {
            LOG.error("Real-time traffic data is not yet supported; refusing to update graph");
            return;
        }

        hasAlreadyRun = true;

        LOG.info("Loading speed data");

        // Build a speed index now while we're running in our own thread. We'll swap it out
        // at the appropriate time with a graphwriterrunnable, but no need to synchronize yet.
        Map<Segment, SpeedSample> speedIndex = Maps.newHashMap();

        // search through the tile directory
        for (File z : tileDirectory.listFiles()) {
            for (File x : z.listFiles()) {
                for (File y : x.listFiles()) {
                    if (!y.getName().endsWith(".traffic.pbf")) {
                        LOG.warn("Skipping non-traffic file {} in tile directory", y);
                        continue;
                    }

                    // Deserialize it
                    InputStream in = new BufferedInputStream(new FileInputStream(y));
                    ExchangeFormat.BaselineTile tile = ExchangeFormat.BaselineTile.parseFrom(in);
                    in.close();

                    // TODO: handle metadata

                    for (int i = 0; i < tile.getSegmentsCount(); i++) {
                        ExchangeFormat.BaselineStats stats = tile.getSegments(i);
                        SpeedSample sample;
                        try {
                            sample = new SpeedSample(stats);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        Segment segment = new Segment(stats.getSegment());
                        speedIndex.put(segment, sample);
                    }
                }
            }
        }

        LOG.info("Indexed {} speed samples", speedIndex.size());

        graphUpdaterManager.execute(graph -> {
            graph.streetSpeedSource.setSamples(speedIndex);
        });
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        this.graph = graph;
        tileDirectory = new File(config.get("tileDirectory").asText());
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        updaterManager.addUpdater(this);
        this.graphUpdaterManager = updaterManager;
    }

    @Override
    public void setup() throws Exception {
        graphUpdaterManager.execute(graph -> {
            graph.streetSpeedSource = new StreetSpeedSource();
        });
    }

    @Override
    public void teardown() {
        graphUpdaterManager.execute(graph -> {
            graph.streetSpeedSource = null;
        });
    }
}
