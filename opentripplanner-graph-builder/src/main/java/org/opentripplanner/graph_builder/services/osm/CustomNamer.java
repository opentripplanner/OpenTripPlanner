package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.graph_builder.model.osm.OSMWay;

/**
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 * @author novalis
 *
 */
public interface CustomNamer {
	public String name (OSMWay way, String defaultName);
}
