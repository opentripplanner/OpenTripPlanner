package org.opentripplanner.graph_builder.impl.osm;

public class CreativeNamerPicker {
	private OSMSpecifier specifier;
	private CreativeNamer namer;

	public CreativeNamerPicker() {
	}

	public CreativeNamerPicker(OSMSpecifier specifier, CreativeNamer namer) {
		this.specifier = specifier;
		this.namer = namer;
	}

	public void setSpecifier(OSMSpecifier specifier) {
		this.specifier = specifier;
	}

	public OSMSpecifier getSpecifier() {
		return specifier;
	}

	public void setNamer(CreativeNamer namer) {
		this.namer = namer;
	}

	public CreativeNamer getNamer() {
		return namer;
	}

}
