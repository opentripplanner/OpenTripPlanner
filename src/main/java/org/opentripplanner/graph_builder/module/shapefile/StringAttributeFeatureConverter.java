package org.opentripplanner.graph_builder.module.shapefile;

import org.opengis.feature.simple.SimpleFeature;

/**
 * A converter for extracting string attributes from features. The converter supports the
 * specification of a default value, which is useful if you are reading from data sources that might
 * contain blank (or null) values (e.g., if you're reading street name values, you might want to
 * default to "Unnamed street" instead of null or " ").
 * 
 * @author nicholasbs
 * 
 */
public class StringAttributeFeatureConverter extends AttributeFeatureConverter<String> {

    private String defaultValue;

    public StringAttributeFeatureConverter() {

    }

    public StringAttributeFeatureConverter(String attributeName, String defaultValue) {
        super(attributeName);
        this.defaultValue = defaultValue;
    }

    public StringAttributeFeatureConverter(String attributeName) {
        super(attributeName);
        defaultValue = null;
    }

    /**
     * The default value to assign to features with null or empty (" ") values.
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String convert(SimpleFeature feature) {
        String attr = (String) feature.getAttribute(getAttributeName());
        // Since dBase (used in shapefiles) has poor/no null support, null strings are sometimes
        // stored as a single space " "
        if (attr == null || attr.equals(" ")) {
            attr = defaultValue;
        }
        return attr;
    }

}
