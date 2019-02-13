package org.opentripplanner.graph_builder.module.shapefile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/**
 * A converter which is a composite of other converters. It can combine them with an "and" or "or"
 * strategy. The orPermissions variable controls that.
 * 
 * @author rob, novalis
 * 
 */
public class CompositeBooleanConverter implements SimpleFeatureConverter<Boolean> {

    private Collection<SimpleFeatureConverter<Boolean>> converters = new ArrayList<SimpleFeatureConverter<Boolean>>();

    private boolean orPermissions = false;

    public CompositeBooleanConverter() {
    }

    public CompositeBooleanConverter(SimpleFeatureConverter<Boolean>... converters) {
        this.converters = new ArrayList<SimpleFeatureConverter<Boolean>>(Arrays.asList(converters));
    }
    
    /**
     * Is the or combination strategy being used?
     * 
     * @return whether the or combination strategy is used
     */
    public boolean isOrPermissions() {
        return orPermissions;
    }

    public void setOrPermissions(boolean orPermissions) {
        this.orPermissions = orPermissions;
    }

    /**
     * set the list of converters used to the passed in parameter
     * 
     * @param converters
     *            list of converters to use
     */
    public void setConverters(
            Collection<SimpleFeatureConverter<Boolean>> converters) {
        this.converters = converters;
    }

    /**
     * use the permission combination strategy to combine the results of the list of converters
     */
    @Override
    public Boolean convert(SimpleFeature feature) {
        Boolean result = null;
        for (SimpleFeatureConverter<Boolean> converter : converters) {
            Boolean value = converter.convert(feature);
            if (result == null) {
                result = value;
            } else {
                if (orPermissions) {
                    result = result || value;
                } else {
                    result = result && value;
                }
            }

        }
        return result;
    }

    /**
     * add a converter to the list to be applied
     * @param converter the new converter
     */
    public void add(SimpleFeatureConverter<Boolean> converter) {
        converters.add(converter);
    }

}
