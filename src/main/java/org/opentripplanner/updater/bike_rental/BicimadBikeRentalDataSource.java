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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a BikeRentalDataSource for the BICIMAD API.
 *
 * @see BikeRentalDataSource
 */
public class BicimadBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

        private static final Logger log = LoggerFactory
                .getLogger(GenericJsonBikeRentalDataSource.class);

        private static final String jsonExternalParsePath = "data";

        private static final String jsonInternalParsePath = "stations";

        private String url;

        List<BikeRentalStation> stations = new ArrayList<>();

        public BicimadBikeRentalDataSource() {
        }

        @Override public boolean update() {
                try {
                        InputStream data;

                        URL url2 = new URL(url);

                        String proto = url2.getProtocol();
                        if (proto.equals("http") || proto.equals("https")) {
                                data = HttpUtils.getData(url);
                        } else {
                                // Local file probably, try standard java
                                data = url2.openStream();
                        }

                        if (data == null) {
                                log.warn("Failed to get data from url " + url);
                                return false;
                        }
                        parseJSON(data);
                        data.close();
                } catch (IllegalArgumentException e) {
                        log.warn("Error parsing bike rental feed from " + url, e);
                        return false;
                } catch (JsonProcessingException e) {
                        log.warn("Error parsing bike rental feed from " + url
                                + "(bad JSON of some sort)", e);
                        return false;
                } catch (IOException e) {
                        log.warn("Error reading bike rental feed from " + url, e);
                        return false;
                }
                return true;
        }

        private void parseJSON(InputStream dataStream)
                throws IllegalArgumentException, IOException {

                ArrayList<BikeRentalStation> out = new ArrayList<>();

                String rentalString = convertStreamToString(dataStream);

                JsonNode rootNode = getJsonNode(rentalString, jsonExternalParsePath);
                rootNode = getJsonNode(rootNode.asText(), jsonInternalParsePath);
                for (int i = 0; i < rootNode.size(); i++) {
                        JsonNode node = rootNode.get(i);
                        if (node == null) {
                                continue;
                        }
                        BikeRentalStation brstation = makeStation(node);
                        if (brstation != null)
                                out.add(brstation);
                }
                synchronized (this) {
                        stations = out;
                }
        }

        private JsonNode getJsonNode(String rentalString, String jsonParsePath) throws IOException {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(rentalString);

                if (!jsonParsePath.equals("")) {
                        String delimiter = "/";
                        String[] parseElement = jsonParsePath.split(delimiter);
                        for (int i = 0; i < parseElement.length; i++) {
                                rootNode = rootNode.path(parseElement[i]);
                        }

                        if (rootNode.isMissingNode()) {
                                throw new IllegalArgumentException(
                                        "Could not find jSON elements " + jsonParsePath);
                        }
                }
                return rootNode;
        }

        private String convertStreamToString(java.io.InputStream is) {
                java.util.Scanner scanner = null;
                String result = "";
                try {

                        scanner = new java.util.Scanner(is).useDelimiter("\\A");
                        result = scanner.hasNext() ? scanner.next() : "";
                        scanner.close();
                } finally {
                        if (scanner != null)
                                scanner.close();
                }
                return result;

        }

        @Override public synchronized List<BikeRentalStation> getStations() {
                return stations;
        }

        public String getUrl() {
                return url;
        }

        public void setUrl(String url) {
                this.url = url;
        }

        public BikeRentalStation makeStation(JsonNode node) {

                if (!node.path("activate").asText().equals("1")) {
                        return null;
                }
                BikeRentalStation station = new BikeRentalStation();
                station.id = String.format("%d", node.path("id").asInt());
                station.x = node.path("longitude").asDouble();
                station.y = node.path("latitude").asDouble();
                station.name = new NonLocalizedString(node.path("name").asText());
                station.bikesAvailable = node.path("dock_bikes").asInt();
                station.spacesAvailable = node.path("free_bases").asInt();
                return station;
        }

        @Override public String toString() {
                return getClass().getName() + "(" + url + ")";
        }

        /**
         * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
         * from the JSON coming in from the update source.
         */
        @Override public void configure(Graph graph, JsonNode jsonNode) {
                String url = jsonNode.path("url").asText(); // path() returns MissingNode not null.
                if (url == null) {
                        throw new IllegalArgumentException(
                                "Missing mandatory 'url' configuration.");
                }
                this.url = url;
        }

}
