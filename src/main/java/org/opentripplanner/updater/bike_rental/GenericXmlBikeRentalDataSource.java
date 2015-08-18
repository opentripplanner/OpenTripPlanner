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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.xml.XmlDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericXmlBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(GenericXmlBikeRentalDataSource.class);

    private String url;

    List<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();

    private XmlDataListDownloader<BikeRentalStation> xmlDownloader;


    public GenericXmlBikeRentalDataSource(String path) {
        xmlDownloader = new XmlDataListDownloader<BikeRentalStation>();
        xmlDownloader.setPath(path);
        xmlDownloader.setDataFactory(new XmlDataListDownloader.XmlDataFactory<BikeRentalStation>() {
            @Override
            public BikeRentalStation build(Map<String, String> attributes) {
                /* TODO Do not make this class abstract, but instead make the client
                 * provide itself the factory?
                 */
                return makeStation(attributes);
            }
        });
    }

    @Override
    public boolean update() {
        List<BikeRentalStation> newStations = xmlDownloader.download(url, false);
        if (newStations != null) {
            synchronized(this) {
                stations = newStations;
            }
            return true;
        }
        LOG.info("Can't update bike rental station list from: " + url + ", keeping current list.");
        return false;
    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

    public void setReadAttributes(boolean readAttributes) {
        // if readAttributes is true, read XML attributes of selected elements, instead of children
        xmlDownloader.setReadAttributes(readAttributes);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public abstract BikeRentalStation makeStation(Map<String, String> attributes);

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }
    
    @Override
    public void configure(Graph graph, JsonNode config) {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        setUrl(url);
    }
}
