package org.opentripplanner.graph_builder.impl.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

public class CaseBasedTraversalPermissionConverter implements
        SimpleFeatureConverter<StreetTraversalPermission> {

    private String _attributeName;

    private StreetTraversalPermission _defaultPermission = StreetTraversalPermission.ALL;

    private Map<String, StreetTraversalPermission> _permissions = new HashMap<String, StreetTraversalPermission>();

    public CaseBasedTraversalPermissionConverter() {

    }

    public CaseBasedTraversalPermissionConverter(String attributeName) {
        _attributeName = attributeName;
    }
    
    public CaseBasedTraversalPermissionConverter(String attributeName, StreetTraversalPermission defaultPermission) {
        _attributeName = attributeName;
        _defaultPermission = defaultPermission;
    }

    public void setAttributeName(String attributeName) {
        _attributeName = attributeName;
    }

    public void setDefaultPermission(StreetTraversalPermission permission) {
        _defaultPermission = permission;
    }

    public void setPermisssions(Map<String, String> permissions) {
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            String attributeValue = entry.getKey();
            StreetTraversalPermission permission = StreetTraversalPermission.valueOf(entry
                    .getValue());
            addPermission(attributeValue, permission);
        }
    }

    public void addPermission(String attributeValue, StreetTraversalPermission permission) {
        _permissions.put(attributeValue, permission);
    }

    @Override
    public StreetTraversalPermission convert(SimpleFeature feature) {
        String key = feature.getAttribute(_attributeName).toString();
        StreetTraversalPermission permission = _permissions.get(key);
        if (permission == null)
            permission = _defaultPermission;
        return permission;
    }
}
