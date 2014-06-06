package org.opentripplanner.analyst;

import com.google.common.collect.Maps;
import org.opentripplanner.analyst.request.SampleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PointSetCache {

    private static final Logger LOG = LoggerFactory.getLogger(PointSetCache.class);

    public static final String PATH = "/var/otp/pointsets";

    public final Map<String, PointSet> pointSets = Maps.newHashMap();
    public final Map<String, SampleSet> sampleSets = Maps.newHashMap();

    private final SampleFactory sfac;

    public PointSetCache (SampleFactory sfac) {
        this.sfac = sfac;
        reload();
    }

    public PointSet get(String id) {
        return pointSets.get(id);
    }

    public void reload() {
        LOG.info("Loading all PointSets in directory '{}'", PATH);
        File dir = new File(PATH);
        if ( ! (dir.isDirectory() && dir.canRead())) {
            LOG.error("'{}' is not a readable directory.", dir);
            return;
        }
        pointSets.clear();
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (name.endsWith(".csv")) {
                String baseName = name.substring(0, name.length() - 4);
                LOG.info("loading '{}' with ID '{}'", file, baseName);
                try {
                    PointSet pset = PointSet.fromCsv(file.getAbsolutePath());
                    if (pset == null) {
                        LOG.warn("Failure, skipping this pointset.");
                    }
                    pset.samples = new SampleSet(pset, sfac);
                    pointSets.put(baseName, pset);
                } catch (IOException ioex) {
                    LOG.warn("Exception while loading pointset: {}", ioex);
                }
            } else if (name.endsWith(".json")) {
                String baseName = name.substring(0, name.length() - 5);
                LOG.info("loading '{}' with ID '{}'", file, baseName);
                PointSet pset = PointSet.fromGeoJson(file.getAbsolutePath());
                if (pset == null) {
                    LOG.warn("Failure, skipping this pointset.");
                }
                pset.samples = new SampleSet(pset, sfac);
                pointSets.put(baseName, pset);
            }

        }
    }

}
