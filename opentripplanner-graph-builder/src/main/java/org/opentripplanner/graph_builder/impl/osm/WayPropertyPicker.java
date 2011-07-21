package org.opentripplanner.graph_builder.impl.osm;

public class WayPropertyPicker {
	private OSMSpecifier specifier;
	private WayProperties properties;
	private boolean safetyMixin;

	public WayPropertyPicker() {
	}
	
	public WayPropertyPicker(OSMSpecifier specifier, WayProperties properties, boolean mixin) {
		this.specifier = specifier;
		this.properties = properties;
		this.safetyMixin = mixin;
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

	public void setSafetyMixin(boolean mixin) {
		this.safetyMixin = mixin;
	}
	
	/** 
	 * If this value is true, and this picker's specifier applies to a given way, then
	 * this is never chosen as the most applicable value, and the final safety should be 
	 * multiplied by this value.  More than one mixin may apply.
	 */
	public boolean isSafetyMixin() {
		return safetyMixin;
	}
}
