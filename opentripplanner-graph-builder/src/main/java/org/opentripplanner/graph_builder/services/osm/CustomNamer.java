package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;

/**
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 * 
 * @author novalis
 * 
 */
public interface CustomNamer {
    public String name(OSMWay way, String defaultName);

    public void nameWithEdge(OSMWay way, PlainStreetEdge edge);

    public void postprocess(Graph graph);
}
