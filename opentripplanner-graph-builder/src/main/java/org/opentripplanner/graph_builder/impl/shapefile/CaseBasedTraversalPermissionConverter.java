/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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

    public void setPermissions(Map<String, String> permissions) {
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
        Object key = feature.getAttribute(_attributeName);
        if (key == null) {
            return _defaultPermission;
        }
        P2<StreetTraversalPermission> permission = _permissions.get(key.toString());
        if (permission == null) {
            return _defaultPermission;
        }
        return permission;
    }
}
