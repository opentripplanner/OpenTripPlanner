package org.opentripplanner.graph_builder.impl.osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WayPropertySet {
	private static Logger _log = LoggerFactory.getLogger(WayPropertySet.class);

	private List<WayPropertyPicker> wayProperties;
	private List<CreativeNamerPicker> creativeNamers;
	private List<SlopeOverridePicker> slopeOverrides;

	public WayProperties defaultProperties;

	public WayPropertySet() {
		/* sensible defaults */
		defaultProperties = new WayProperties();
		defaultProperties.setSafetyFeatures(new P2<Double>(1.0, 1.0));
		defaultProperties.setPermission(StreetTraversalPermission.ALL);
		wayProperties = new ArrayList<WayPropertyPicker>();
		creativeNamers = new ArrayList<CreativeNamerPicker>();
		slopeOverrides = new ArrayList<SlopeOverridePicker>();
	}

	public WayProperties getDataForWay(OSMWay way) {
		WayProperties result = defaultProperties;
		int bestScore = 0;
		for (WayPropertyPicker picker : getWayProperties()) {
			OSMSpecifier specifier = picker.getSpecifier();
			WayProperties wayProperties = picker.getProperties();
			int score = specifier.matchScore(way);
			if (score > bestScore) {
				result = wayProperties;
				bestScore = score;
			}
		}
		if (bestScore == 0) {
			/* generate warning message */
			String all_tags = null;
			Map<String, String> tags = way.getTags();
			for (String key : tags.keySet()) {
				String tag = key + "=" + tags.get(key);
				if (all_tags == null) {
					all_tags = tag;
				} else {
					all_tags += "; " + tag;
				}
			}
			_log.debug("Used default permissions: " + all_tags);
		}
		return result;
	}

	public String getCreativeNameForWay(OSMWay way) {
		CreativeNamer bestNamer = null;
		int bestScore = 0;
		for (CreativeNamerPicker picker : getCreativeNamers()) {
			OSMSpecifier specifier = picker.getSpecifier();
			CreativeNamer namer = picker.getNamer();
			if (specifier.matchScore(way) > bestScore) {
				bestNamer = namer;
			}
		}
		if (bestNamer == null) {
			return null;
		}
		return bestNamer.generateCreativeName(way);
	}

	public boolean getSlopeOverride(OSMWay way) {
		boolean result = false;
		int bestScore = 0;
		for (SlopeOverridePicker picker : slopeOverrides) {
			OSMSpecifier specifier = picker.getSpecifier();
			int score = specifier.matchScore(way);
			if (score > bestScore) {
				result = picker.getOverride();
				bestScore = score;
			}
		}
		return result;
	}

	public void addProperties(OSMSpecifier spec, WayProperties properties) {
		getWayProperties().add(new WayPropertyPicker(spec, properties));
	}

	public void addAddCreativeNamer(OSMSpecifier spec, CreativeNamer namer) {
		getCreativeNamers().add(new CreativeNamerPicker(spec, namer));
	}

	public void setWayProperties(List<WayPropertyPicker> wayProperties) {
		this.wayProperties = wayProperties;
	}

	public List<WayPropertyPicker> getWayProperties() {
		return wayProperties;
	}

	public void setCreativeNamers(List<CreativeNamerPicker> creativeNamers) {
		this.creativeNamers = creativeNamers;
	}

	public List<CreativeNamerPicker> getCreativeNamers() {
		return creativeNamers;
	}

}
