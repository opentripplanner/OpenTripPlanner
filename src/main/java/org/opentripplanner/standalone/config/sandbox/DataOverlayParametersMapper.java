package org.opentripplanner.standalone.config.sandbox;

import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.standalone.config.NodeAdapter;


public class DataOverlayParametersMapper {

    public static DataOverlayParameters map(NodeAdapter c) {
        var dataOverlay = new DataOverlayParameters();

        for (String param : DataOverlayParameters.parametersAsString()) {
            c.asDoubleOptional(param).ifPresent(it -> dataOverlay.put(param, it));
        }
        return dataOverlay;
    }
}
