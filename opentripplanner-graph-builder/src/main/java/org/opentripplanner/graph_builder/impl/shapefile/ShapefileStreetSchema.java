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
