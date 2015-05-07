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

package org.opentripplanner.updater.bike_park;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.xml.XmlDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load bike park from a KML placemarks. Use name as bike park name and point coordinates. Rely on:
 * 1) bike park to be KML Placemarks, 2) geometry to be Point.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public class KmlBikeParkDataSource implements BikeParkDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(KmlBikeParkDataSource.class);

    private String url;

    private String namePrefix = null;

    private boolean zip;

    private XmlDataListDownloader<BikePark> xmlDownloader;

    private List<BikePark> bikeParks;

    public KmlBikeParkDataSource() {
        xmlDownloader = new XmlDataListDownloader<BikePark>();
        xmlDownloader
                .setPath("//*[local-name()='kml']/*[local-name()='Document']/*[local-name()='Placemark']|//*[local-name()='kml']/*[local-name()='Document']/*[local-name()='Folder']/*[local-name()='Placemark']");
        xmlDownloader.setDataFactory(new XmlDataListDownloader.XmlDataFactory<BikePark>() {
            @Override
            public BikePark build(Map<String, String> attributes) {
                BikePark bikePark = new BikePark();
                if (!attributes.containsKey("name")) {
                    LOG.warn("Missing name in KML Placemark, cannot create bike park.");
                    return null;
                }
                if (!attributes.containsKey("Point")) {
                    LOG.warn("Missing Point geometry in KML Placemark, cannot create bike park.");
                    return null;
                }
                bikePark.name = attributes.get("name").trim();
                if (namePrefix != null)
                    bikePark.name = namePrefix + bikePark.name;
                String[] coords = attributes.get("Point").trim().split(",");
                bikePark.x = Double.parseDouble(coords[0]);
                bikePark.y = Double.parseDouble(coords[1]);
                // There is no ID in KML, assume unique names and location.
                bikePark.id = String.format(Locale.US, "%s[%.3f-%.3f]",
                        bikePark.name.replace(" ", "_"), bikePark.x, bikePark.y);
                return bikePark;
            }
        });
    }

    /**
     * Update the data from the source;
     * 
     * @return true if there might have been changes
     */
    @Override
    public boolean update() {
        List<BikePark> newBikeParks = xmlDownloader.download(url, zip);
        if (newBikeParks != null) {
            synchronized (this) {
                // Update atomically
                bikeParks = newBikeParks;
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized List<BikePark> getBikeParks() {
        return bikeParks;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }

    @Override
    public void configure (Graph graph, JsonNode config) {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        this.url = url;
        this.namePrefix = config.path("namePrefix").asText();
        this.zip = "true".equals(config.path("zip").asText());
    }

    public void setUrl (String url) {this.url = url;}
}
