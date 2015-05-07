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

package org.opentripplanner.updater.bike_rental;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load bike rental stations from a KML placemarks. Use name as bike park name and point
 * coordinates. Rely on: 1) bike park to be KML Placemarks, 2) geometry to be Point.
 */
public class GenericKmlBikeRentalDataSource extends GenericXmlBikeRentalDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GenericKmlBikeRentalDataSource.class);

    private String namePrefix = null;

    private Set<String> networks = null;

    private boolean allowDropoff = true;

    /**
     * @param namePrefix A string to prefix all station names coming from this source (for example:
     *        "OV-fietspunt "). Please add a space at the end if needed.
     */
    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * @param networks A network, or a comma-separated list of networks, to set to all stations from
     *        the dataSource. Default to null (compatible with all).
     */
    public void setNetworks(String networks) {
        this.networks = new HashSet<String>();
        this.networks.addAll(Arrays.asList(networks.split(",")));
    }

    /**
     * @param allowDropoff True if the bike rental stations coming from this source allows bike
     *        dropoff. True by default.
     */
    public void setAllowDropoff(boolean allowDropoff) {
        this.allowDropoff = allowDropoff;
    }

    public GenericKmlBikeRentalDataSource() {
        super("//*[local-name()='kml']/*[local-name()='Document']/*[local-name()='Placemark']");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        if (!attributes.containsKey("name")) {
            LOG.warn("Missing name in KML Placemark, cannot create bike rental.");
            return null;
        }
        if (!attributes.containsKey("Point")) {
            LOG.warn("Missing Point geometry in KML Placemark, cannot create bike rental.");
            return null;
        }
        BikeRentalStation brStation = new BikeRentalStation();
        brStation.name = new NonLocalizedString(attributes.get("name").trim());
        if (namePrefix != null)
            brStation.name = new NonLocalizedString(namePrefix + brStation.name);
        String[] coords = attributes.get("Point").trim().split(",");
        brStation.x = Double.parseDouble(coords[0]);
        brStation.y = Double.parseDouble(coords[1]);
        // There is no ID in KML, assume unique names and location
        brStation.id = String.format(Locale.US, "%s[%.3f-%.3f]", brStation.name.toString().replace(" ", "_"),
                brStation.x, brStation.y);
        brStation.realTimeData = false;
        brStation.bikesAvailable = 1; // Unknown, always 1
        brStation.spacesAvailable = 1; // Unknown, always 1
        brStation.networks = networks;
        brStation.allowDropoff = allowDropoff;
        return brStation;
    }

    @Override
    public void configure(Graph graph, JsonNode config) {
        super.configure(graph, config);
        setNamePrefix(config.path("namePrefix").asText());
    }

}
