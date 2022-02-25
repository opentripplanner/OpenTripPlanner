package org.opentripplanner.graph_builder.module.shapefile;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A converter which is a composite of other converters. It can combine them with an "and" or "or"
 * strategy. The orPermissions variable controls that.
 * 
 * @author rob
 * 
 */
public class CompositeStreetTraversalPermissionConverter implements SimpleFeatureConverter<P2<StreetTraversalPermission>> {

    private Collection<SimpleFeatureConverter<P2<StreetTraversalPermission>>> converters = new ArrayList<SimpleFeatureConverter<P2<StreetTraversalPermission>>>();

    private boolean orPermissions = false;

    public CompositeStreetTraversalPermissionConverter() {
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
            Collection<SimpleFeatureConverter<P2<StreetTraversalPermission>>> converters) {
        this.converters = converters;
    }

    /**
     * use the permission combination strategy to combine the results of the list of converters
     */
    @Override
    public P2<StreetTraversalPermission> convert(SimpleFeature feature) {
        P2<StreetTraversalPermission> result = null;
        for (SimpleFeatureConverter<P2<StreetTraversalPermission>> converter : converters) {
            P2<StreetTraversalPermission> value = converter.convert(feature);
            if (result == null) {
                result = value;
            } else {
                StreetTraversalPermission first, second;
                if (orPermissions) {
                    first = result.first.add(value.first);
                    second = result.second.add(value.second);
                } else {
                    first = StreetTraversalPermission.get(result.first.code & value.first.code);
                    second = StreetTraversalPermission.get(result.second.code & value.second.code);
                }
                result = new P2<StreetTraversalPermission>(first, second);
            }

        }
        return result;
    }

    /**
     * add a converter to the list to be applied
     * @param converter the new converter
     */
    public void add(SimpleFeatureConverter<P2<StreetTraversalPermission>> converter) {
        converters.add(converter);
    }

}
