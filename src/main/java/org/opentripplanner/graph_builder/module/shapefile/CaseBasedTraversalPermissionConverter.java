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

package org.opentripplanner.graph_builder.module.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates street traversal permissions based upon a fixed set of cases.
 * 
 * For example, given a shapefile that includes a DIRECTION column with data as follows:
 * <pre>
 * | DIRECTION | NAME    | 
 * | ONE_WAY_F | Side St | 
 * | TWO_WAY   | Main St |
 * | ONE_WAY_B | Foo St. |
 * </pre>
 * You could use a CaseBasedTraversalPermissionConverter to implement the following rules:
 *
 *      <p>By default, all streets should be traversable by pedestrians and bicycles in both directions.</p>
 *      
 *      <p>If a street's DIRECTION attribute is ONE_WAY_F, it should be traversable by cars and bikes in
 *      only the forward direction and traversable by pedestrians in both directions.</p>
 * 
 *      <p>If a street's DIRECTION attribute is ONE_WAY_B, it should be traversable by cars and bikes in
 *      only the backward direction and traversable by pedestrians in both directions.</p>
 *      
 *      <p>If a street's DIRECTION attribute is TWO_WAY, it should be traversable by everyone in both 
 *      directions.</p>
 *      
 * 
 * These rules could be implemented by configuring the converter bean as follows:
 * <pre>
 * {@code
 * <bean class="org.opentripplanner.graph_builder.module.shapefile.CaseBasedTraversalPermissionConverter">
 *   <property name="attributeName"     value="DIRECTION" /> 
 *   <property name="defaultPermission" value="PEDESTRIAN_AND_BICYCLE" /> 
 *   <property name="permissions"> 
 *       <map> 
 *           <entry key="ONE_WAY_F" value="PEDESTRIAN,ALL" /> 
 *           <entry key="ONE_WAY_B" value="ALL,PEDESTRIAN" /> 
 *           <entry key="TWO_WAY" value="ALL,ALL" /> 
 *      </map> 
 *  </property> 
 * </bean>}
 * </pre>
 * @see org.opentripplanner.routing.edgetype.StreetTraversalPermission
 * 
 */
public class CaseBasedTraversalPermissionConverter implements
        SimpleFeatureConverter<P2<StreetTraversalPermission>> {

    private static Logger log = LoggerFactory.getLogger(CaseBasedBicycleSafetyFeatureConverter.class);

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
    
    /**
     * The name of the feature attribute to use when calculating the traversal permissions.
     */
    public void setAttributeName(String attributeName) {
        _attributeName = attributeName;
    }

    /**
     * The default permission to use when no matching case is found for a street.
     */
    public void setDefaultPermission(StreetTraversalPermission permission) {
        _defaultPermission = P2.createPair(permission, permission);
    }

    /**
     * The mapping from attribute values to permissions to use when determining a street's traversal
     * permission.
     */
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
        if (_attributeName == null) {
            return _defaultPermission;
        }
        Object key = feature.getAttribute(_attributeName);
        if (key == null) {
            return _defaultPermission;
        }
        P2<StreetTraversalPermission> permission = _permissions.get(key.toString());
        if (permission == null) {
            log.info("unexpected permission " + key.toString());
            return _defaultPermission;
        }
        return permission;
    }
}
