package org.opentripplanner.ext.dataoverlay.configure;

import java.io.File;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.dataoverlay.GenericDataFile;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayConfig;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataOverlayFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DataOverlayFactory.class);

    public static GraphBuilderModule create(DataOverlayConfig config) {
        File dataFile = new File(config.getFileName());
        if (dataFile.exists()) {
            return new EdgeUpdaterModule(
                    new GenericDataFile(dataFile, config),
                    config.getTimeFormat()
            );
        }
        else {
            LOG.error("No data input {} found!", dataFile);
            return null;
        }
    }
}
