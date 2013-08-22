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

package org.opentripplanner.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.bike_rental.BCycleBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater2;
import org.opentripplanner.updater.bike_rental.BixiBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.CityBikesBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.JCDecauxBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.KeolisRennesBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.OVFietsKMLDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure a graph by creating dynamic bike rental station based on a source type.
 * 
 * Usage example ('bike1' name is an example):
 * 
 * <pre>
 * bike1.type = bike-rental
 * bike1.frequencySec = 60
 * bike1.networks = V3,V3N
 * bike1.sourceType = jcdecaux
 * bike1.url = https://api.jcdecaux.com/vls/v1/stations?contract=Xxx?apiKey=Zzz
 * </pre>
 * 
 */
public class BikeRentalConfigurator implements PreferencesConfigurable {

    private static final String DEFAULT_NETWORK_LIST = "default";

    private static final long DEFAULT_UPDATE_FREQ_SEC = 60;

    private static Logger LOG = LoggerFactory.getLogger(BikeRentalConfigurator.class);

    private static Map<String, Class<? extends BikeRentalDataSource>> bikeRentalSources;

    static {
        // List of all possible dynamic bike rental sources with it's magic 'type' key.
        bikeRentalSources = new HashMap<String, Class<? extends BikeRentalDataSource>>();
        bikeRentalSources.put("jcdecaux", JCDecauxBikeRentalDataSource.class);
        bikeRentalSources.put("b-cycle", BCycleBikeRentalDataSource.class);
        bikeRentalSources.put("bixi", BixiBikeRentalDataSource.class);
        bikeRentalSources.put("keolis-rennes", KeolisRennesBikeRentalDataSource.class);
        bikeRentalSources.put("ov-fiets", OVFietsKMLDataSource.class);
        bikeRentalSources.put("city-bikes", CityBikesBikeRentalDataSource.class);
    }

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        String sourceType = preferences.get("sourceType", null);
        Class<? extends BikeRentalDataSource> clazz = bikeRentalSources.get(sourceType);
        if (clazz == null) {
            LOG.error("Unknown bike rental source type: " + sourceType);
            return;
        }
        BikeRentalDataSource source = clazz.newInstance();
        if (source instanceof PreferencesConfigurable) {
            // If the source itself is a configurable, let's configure it.
            ((PreferencesConfigurable) source).configure(graph, preferences);
        }
        BikeRentalUpdater2 updater = new BikeRentalUpdater2(graph, source);
        updater.setNetwork(preferences.get("networks", DEFAULT_NETWORK_LIST));
        long frequencySec = preferences.getLong("frequencySec", DEFAULT_UPDATE_FREQ_SEC);
        LOG.info("Creating bike-rental updater running every {} seconds : {}", frequencySec,
                source);
        GraphUpdaterManager periodicGraphUpdater = graph
                .getService(GraphUpdaterManager.class);
        periodicGraphUpdater.addUpdater(updater, frequencySec * 1000);
    }
}
