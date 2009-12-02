package org.opentripplanner.graph_builder.impl.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

public class CaseBasedTraversalPermissionConverter implements
        SimpleFeatureConverter<P2<StreetTraversalPermission>> {

    private String _attributeName;

    private P2<StreetTraversalPermission> _defaultPermission = P2.createPair(
            StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);

    private Map<String, P2<StreetTraversalPermission>> _permissions = new HashMap<String, P2<StreetTraversalPermission>>();

    public CaseBasedTraversalPermissionConverter() {

    }

    public CaseBasedTraversalPermissionConverter(String attributeName) {
        _attributeName = attributeName;
    }

    public CaseBasedTraversalPermissionConverter(String attributeName,
            StreetTraversalPermission defaultPermission) {
        _attributeName = attributeName;
        _defaultPermission = P2.createPair(defaultPermission, defaultPermission);
    }

    public void setAttributeName(String attributeName) {
        _attributeName = attributeName;
    }

    public void setDefaultPermission(StreetTraversalPermission permission) {
        _defaultPermission = P2.createPair(permission, permission);
    }

    public void setPermisssions(Map<String, String> permissions) {
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            String attributeValue = entry.getKey();
            String perms = entry.getValue();
            String[] tokens = perms.split(",");
            if (tokens.length != 2)
                throw new IllegalArgumentException("invalid street traversal permissions: " + perms);

            StreetTraversalPermission forward = StreetTraversalPermission.valueOf(tokens[0]);
            StreetTraversalPermission reverse = StreetTraversalPermission.valueOf(tokens[1]);
            addPermission(attributeValue, forward, reverse);
        }
    }

    public void addPermission(String attributeValue, StreetTraversalPermission forward,
            StreetTraversalPermission reverse) {
        _permissions.put(attributeValue, P2.createPair(forward, reverse));
    }

    @Override
    public P2<StreetTraversalPermission> convert(SimpleFeature feature) {
        String key = feature.getAttribute(_attributeName).toString();
        P2<StreetTraversalPermission> permission = _permissions.get(key);
        if (permission == null)
            permission = _defaultPermission;
        return permission;
    }
}
