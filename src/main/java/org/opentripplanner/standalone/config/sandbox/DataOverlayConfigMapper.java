package org.opentripplanner.standalone.config.sandbox;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayConfig;
import org.opentripplanner.ext.dataoverlay.configuration.IndexVariable;
import org.opentripplanner.ext.dataoverlay.configuration.ParameterBinding;
import org.opentripplanner.ext.dataoverlay.configuration.TimeUnit;
import org.opentripplanner.standalone.config.NodeAdapter;

public class DataOverlayConfigMapper {

    public static DataOverlayConfig map(NodeAdapter c) {
        if(c.isEmpty()) {
            return null;
        }
        return new DataOverlayConfig(
                c.asText("fileName"),
                c.asText("latitudeVariable"),
                c.asText("longitudeVariable"),
                c.asText("timeVariable"),
                c.asEnum("timeFormat", TimeUnit.class),
                mapIndexVariables(c.path("indexVariables")),
                mapRequestParameters(c.path("requestParameters"))
        );
    }

    private static List<IndexVariable> mapIndexVariables(NodeAdapter c) {
        return c.asList().stream()
                .map(DataOverlayConfigMapper::mapIndexVariable)
                .collect(Collectors.toList());
    }

    private static IndexVariable mapIndexVariable(NodeAdapter c) {
        return new IndexVariable(
                c.asText("name"),
                c.asText("displayName"),
                c.asText("variable")
        );
    }

    private static List<ParameterBinding> mapRequestParameters(NodeAdapter c) {
        return c.asList().stream()
                .map(DataOverlayConfigMapper::mapRequestParameter)
                .collect(Collectors.toList());
    }

    private static ParameterBinding mapRequestParameter(NodeAdapter c) {
        return new ParameterBinding(
                c.asEnum("name", ParameterName.class),
                c.asText("variable"),
                c.asText("formula")
        );
    }
}
