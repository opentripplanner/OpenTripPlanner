package org.opentripplanner.graph_builder.module.osm;

/**
 * Describes how unnamed OSM ways are to be named based on the tags they possess.
 * The CreativeNamer will be applied to ways that match the OSMSpecifier.
 * @author novalis
 */
public class CreativeNamerPicker {
    public OSMSpecifier specifier;
    public CreativeNamer namer;
    
    public CreativeNamerPicker(){
    	specifier=null;
    	namer=null;
    }
    
    public CreativeNamerPicker(OSMSpecifier specifier, CreativeNamer namer){
    	this.specifier = specifier;
    	this.namer = namer;
    }
}
