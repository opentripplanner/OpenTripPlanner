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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

/**
 * A stubbed out simple feature used for tests. Currently only the getAttribute is actually stubbed
 * out, and the return can be controlled with the addAttribute method.
 * 
 * @author rob
 * 
 */
public class StubSimpleFeature implements SimpleFeature {

    private Map<String, Object> attributeMap = new HashMap<String, Object>();

    public void addAttribute(String name, Object value) {
        attributeMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    @Override
    public Object getAttribute(Name name) {
        return null;
    }

    @Override
    public Object getAttribute(int index) throws IndexOutOfBoundsException {
        return null;
    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public List<Object> getAttributes() {
        return null;
    }

    @Override
    public Object getDefaultGeometry() {
        return null;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return null;
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public SimpleFeatureType getType() {
        return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
    }

    @Override
    public void setAttribute(Name name, Object value) {
    }

    @Override
    public void setAttribute(int index, Object value) throws IndexOutOfBoundsException {
    }

    @Override
    public void setAttributes(List<Object> values) {
    }

    @Override
    public void setAttributes(Object[] values) {
    }

    @Override
    public void setDefaultGeometry(Object geometry) {
    }

    @Override
    public BoundingBox getBounds() {
        return null;
    }

    @Override
    public GeometryAttribute getDefaultGeometryProperty() {
        return null;
    }

    @Override
    public FeatureId getIdentifier() {
        return null;
    }

    @Override
    public void setDefaultGeometryProperty(GeometryAttribute geometryAttribute) {

    }

    @Override
    public Collection<Property> getProperties() {
        return null;
    }

    @Override
    public Collection<Property> getProperties(Name name) {
        return null;
    }

    @Override
    public Collection<Property> getProperties(String name) {
        return null;
    }

    @Override
    public Property getProperty(Name name) {
        return null;
    }

    @Override
    public Property getProperty(String name) {
        return null;
    }

    @Override
    public Collection<? extends Property> getValue() {
        return null;
    }

    @Override
    public void setValue(Collection<Property> values) {
    }

    @Override
    public void validate() throws IllegalAttributeException {
    }

    @Override
    public AttributeDescriptor getDescriptor() {
        return null;
    }

    @Override
    public Name getName() {
        return null;
    }

    @Override
    public Map<Object, Object> getUserData() {
        return null;
    }

    @Override
    public boolean isNillable() {
        return false;
    }

    @Override
    public void setValue(Object newValue) {
    }

}
