package org.opentripplanner.graph_builder.impl.osm;

import lombok.Data;

/**
 * Choose a speed that should be applied to a given segment
 */
@Data
public class SpeedPicker {
    private OSMSpecifier specifier;
    private float speed;
}
