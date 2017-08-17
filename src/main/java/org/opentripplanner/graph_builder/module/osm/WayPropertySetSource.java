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

package org.opentripplanner.graph_builder.module.osm;

/**
 * Interface for populating a {@link WayPropertySet} that determine how OSM
 * streets can be traversed in various modes and named.
 *
 * @author bdferris, novalis, seime
 */
public interface WayPropertySetSource {

	public void populateProperties(WayPropertySet wayPropertySet);

	/**
	 * Return the given WayPropertySetSource or throws IllegalArgumentException
	 * if an unkown type is specified
	 */
	public static WayPropertySetSource fromConfig(String type) {
		// type is set to "default" by GraphBuilderParameters if not provided in
		// build-config.json
		if ("default".equals(type)) {
			return new DefaultWayPropertySetSource();
		} else if ("norway".equals(type)) {
			return new NorwayWayPropertySetSource();
		} else {
			throw new IllegalArgumentException(String.format("Unknown osmWayPropertySet: '%s'", type));
		}
	}

}
