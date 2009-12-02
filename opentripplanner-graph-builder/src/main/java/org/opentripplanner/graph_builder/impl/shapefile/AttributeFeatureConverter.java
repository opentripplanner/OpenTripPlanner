package org.opentripplanner.graph_builder.impl.shapefile;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

public class AttributeFeatureConverter<T> implements SimpleFeatureConverter<T> {

    private String _attributeName;

    public AttributeFeatureConverter(String attributeName) {
        _attributeName = attributeName;
    }
    
    public AttributeFeatureConverter() {
        
    }
    
    public void setAttributeName(String attributeName){
        _attributeName = attributeName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(SimpleFeature feature) {
        return (T) feature.getAttribute(_attributeName);
    }

}
