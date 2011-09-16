package org.opentripplanner.graph_builder.impl.osm;

import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;

/**
 * These rules were developed in consultation with Grant Humphries, PJ Houser,
 * and Mele Sax-Barnett. They describe which sidewalks and paths in the Portland
 * area should be specially designated in the narrative.
 * 
 * @author novalis
 * 
 */
public class PortlandCustomNamer implements CustomNamer {

	public static String[] STREET_SUFFIXES = { "Avenue", "Street", "Drive",
			"Court", "Highway", "Lane", "Way", "Place", "Road", "Boulevard",
			"Alley" };

	public static String[] PATH_WORDS = { "Trail", "Trails", "Greenway",
			"Esplanade", "Spur", "Loop" };

	@Override
	public String name(OSMWay way, String defaultName) {
		if (!way.hasTag("name")) {
			// this is already a generated name, so there's no need to add any
			// additional data
			return defaultName;
		}
		if (way.isTag("footway", "sidewalk") || way.isTag("path", "sidewalk")) {
			if (isStreet(defaultName)) {
				return sidewalk(defaultName);
			}
		}
		String highway = way.getTag("highway");
		if ("footway".equals(highway) || "path".equals(highway)
				|| "cycleway".equals(highway)) {
			if (!isObviouslyPath(defaultName)) {
				return path(defaultName);
			}
		}
		return defaultName;
	}

	private boolean isStreet(String defaultName) {
		for (String suffix : STREET_SUFFIXES) {
			if (defaultName.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	private boolean isObviouslyPath(String defaultName) {
		for (String word : PATH_WORDS) {
			if (defaultName.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private String path(String name) {
		if (!name.toLowerCase().contains("path")) {
			name = name + " (path)".intern();
		}
		return name;

	}

	private String sidewalk(String name) {
		if (!name.toLowerCase().contains("sidewalk")) {
			name = name + " (sidewalk)".intern();
		}
		return name;

	}

}
