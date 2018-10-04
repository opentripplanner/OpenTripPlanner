package org.opentripplanner.graph_builder.module.osm;

/**
 * Associates an OSMSpecifier with some WayProperties. The WayProperties will be applied an OSM way when the OSMSpecifier
 * matches it better than any other OSMSpecifier in the same WayPropertySet. WayPropertyPickers may be mixins, in which
 * case they do not need to beat out all the other WayPropertyPickers. Instead, their safety values will be
 * applied to all ways that they match multiplicatively.
 */
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
     * If this value is true, and this picker's specifier applies to a given way, then this picker is never
     * chosen as the most applicable value, and the final safety will be multiplied by this value.
     * More than one mixin may apply.
     */
    public boolean isSafetyMixin() {
        return safetyMixin;
    }
}
