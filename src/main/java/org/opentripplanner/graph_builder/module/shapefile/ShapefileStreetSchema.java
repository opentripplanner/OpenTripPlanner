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

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

public class ShapefileStreetSchema {

    private SimpleFeatureConverter<String> idConverter = new FeatureIdConverter();

    private SimpleFeatureConverter<String> nameConverter;

    private SimpleFeatureConverter<P2<StreetTraversalPermission>> permissionConverter = new CaseBasedTraversalPermissionConverter();

    private SimpleFeatureConverter<P2<Double>> bicycleSafetyConverter = null;

    private SimpleFeatureConverter<Boolean> slopeOverrideConverter = new CaseBasedBooleanConverter();

    private SimpleFeatureConverter<Boolean> featureSelector = null;

    private SimpleFeatureConverter<String> noteConverter = null;

    public SimpleFeatureConverter<String> getIdConverter() {
        return idConverter;
    }

    /**
     * Sets the converter which gets IDs from features.
     * 
     * @{see setIdAttribute}
     */
    public void setIdConverter(SimpleFeatureConverter<String> idConverter) {
        this.idConverter = idConverter;
    }

    /**
     * The ID attribute is used to uniquely identify street segments. This is useful if a given
     * street segment appears multiple times in a shapefile.
     */
    public void setIdAttribute(String attributeName) {
        this.idConverter = new AttributeFeatureConverter<String>(attributeName);
    }

    public SimpleFeatureConverter<String> getNameConverter() {
        return nameConverter;
    }

    public void setNameAttribute(String attributeName) {
        this.nameConverter = new AttributeFeatureConverter<String>(attributeName);
    }

    /**
     * The name converter gets the street name from a feature.
     */
    public void setNameConverter(SimpleFeatureConverter<String> nameConverter) {
        this.nameConverter = nameConverter;
    }

    /**
     * The permission converter gets the {@link StreetTraversalPermission} for a street segment and
     * its reverse.
     * 
     * @return
     */
    public SimpleFeatureConverter<P2<StreetTraversalPermission>> getPermissionConverter() {
        return permissionConverter;
    }

    public void setPermissionConverter(
            SimpleFeatureConverter<P2<StreetTraversalPermission>> permissionConverter) {
        this.permissionConverter = permissionConverter;
    }

    /**
     * The permission converter gets the bicycle safety factor for a street segment and its reverse.
     * The safety factor is 1.0 for an ordinary street. For streets which are more or less safe for
     * bicycles, the safety factor is the number of miles you would have to bike on an ordinary
     * street to have the same odds of dying as if you biked one mile on this street. For example,
     * if bike lanes reduce risk by a factor of 3, the safety factor would be 0.33...
     * 
     * @return
     */
    public void setBicycleSafetyConverter(SimpleFeatureConverter<P2<Double>> safetyConverter) {
        this.bicycleSafetyConverter = safetyConverter;
    }

    public SimpleFeatureConverter<P2<Double>> getBicycleSafetyConverter() {
        return bicycleSafetyConverter;
    }

    /**
     * @see setSlopeOverrideConverter
     * @return
     */
    public SimpleFeatureConverter<Boolean> getSlopeOverrideConverter() {
        return slopeOverrideConverter;
    }

    /**
     * The slope override converter returns true if the slope found from NED is should be ignored
     * (for instance, on bridges and tunnels)
     * 
     * @param slopeOverrideConverter
     */
    public void setSlopeOverrideConverter(SimpleFeatureConverter<Boolean> slopeOverrideConverter) {
        this.slopeOverrideConverter = slopeOverrideConverter;
    }

    /**
     * @param featureSelector
     *            A featureSelector returns true if a feature is a street, and false otherwise.
     *            Useful for centerline files that also have non-streets, such as political
     *            boundaries or coastlines
     */
    public void setFeatureSelector(SimpleFeatureConverter<Boolean> featureSelector) {
        this.featureSelector = featureSelector;
    }

    /**
     * @see setFeatureSelector
     * @return the current feature selector
     */
    public SimpleFeatureConverter<Boolean> getFeatureSelector() {
        return featureSelector;
    }

	public void setNoteConverter(SimpleFeatureConverter<String> noteConverter) {
		this.noteConverter = noteConverter;
	}

	public SimpleFeatureConverter<String> getNoteConverter() {
		return noteConverter;
	}
}
