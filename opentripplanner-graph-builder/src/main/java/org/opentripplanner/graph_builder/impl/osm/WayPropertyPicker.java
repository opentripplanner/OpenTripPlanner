package org.opentripplanner.graph_builder.impl.osm;

public class WayPropertyPicker {
	private OSMSpecifier specifier;
	private WayProperties properties;

	public WayPropertyPicker() {
	}
	
	public WayPropertyPicker(OSMSpecifier specifier, WayProperties properties) {
		this.specifier = specifier;
		this.properties = properties;
}

	public void setSpecifier(OSMSpecifier specifier) {
		this.specifier = specifier;
	}

	public OSMSpecifier getSpecifier() {
		return specifier;
	}

	public void setProperties(WayProperties properties) {
		this.properties = properties;
	}

	public WayProperties getProperties() {
		return properties;
	}
}
