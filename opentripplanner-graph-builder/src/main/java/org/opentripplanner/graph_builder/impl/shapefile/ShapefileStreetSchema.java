package org.opentripplanner.graph_builder.impl.shapefile;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

public class ShapefileStreetSchema {

    private SimpleFeatureConverter<String> idConverter;

    private SimpleFeatureConverter<String> nameConverter;

    private SimpleFeatureConverter<P2<StreetTraversalPermission>> permissionConverter = new CaseBasedTraversalPermissionConverter();

    public SimpleFeatureConverter<String> getIdConverter() {
        return idConverter;
    }

    public void setIdConverter(SimpleFeatureConverter<String> idConverter) {
        this.idConverter = idConverter;
    }

    public void setIdAttribute(String attributeName) {
        this.idConverter = new AttributeFeatureConverter<String>(attributeName);
    }

    public SimpleFeatureConverter<String> getNameConverter() {
        return nameConverter;
    }

    public void setNameAttribute(String attributeName) {
        this.nameConverter = new AttributeFeatureConverter<String>(attributeName);
    }

    public void setNameConverter(SimpleFeatureConverter<String> nameConverter) {
        this.nameConverter = nameConverter;
    }

    public SimpleFeatureConverter<P2<StreetTraversalPermission>> getPermissionConverter() {
        return permissionConverter;
    }

    public void setPermissionConverter(
            SimpleFeatureConverter<P2<StreetTraversalPermission>> permissionConverter) {
        this.permissionConverter = permissionConverter;
    }
}
