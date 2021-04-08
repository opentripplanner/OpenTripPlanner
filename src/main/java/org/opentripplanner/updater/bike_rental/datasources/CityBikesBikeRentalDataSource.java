package org.opentripplanner.updater.bike_rental.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

// TODO This class could probably inherit from GenericJSONBikeRentalDataSource
class CityBikesBikeRentalDataSource implements BikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(BixiBikeRentalDataSource.class);

    private final String url;

    ArrayList<BikeRentalStation> stations = new ArrayList<>();

    public CityBikesBikeRentalDataSource(BikeRentalDataSourceParameters config) {
        this.url = config.getUrl();
    }

    @Override
    public boolean update() {
        try {
            InputStream stream = HttpUtils.getData(url);
            if (stream == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }

            Reader reader = new BufferedReader(new InputStreamReader(stream,
                    Charset.forName("UTF-8")));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int charactersRead;
            while ((charactersRead = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, charactersRead);
            }
            String data = builder.toString();

            parseJson(data);
        } catch (IOException e) {
            log.warn("Error reading bike rental feed from " + url, e);
            return false;
        }
        return true;
    }

    private void parseJson(String data) throws IOException {
        ArrayList<BikeRentalStation> out = new ArrayList<>();

        // Jackson ObjectMapper to read in JSON
        // TODO: test against real data
        ObjectMapper mapper = new ObjectMapper();
        for (JsonNode stationNode : mapper.readTree(data)) {
            BikeRentalStation brStation = new BikeRentalStation();
            // We need string IDs but they are in JSON as numbers. Avoid null from textValue(). See pull req #1450.
            brStation.id = String.valueOf(stationNode.get("id").intValue());
            brStation.x = stationNode.get("lng").doubleValue() / 1000000.0;
            brStation.y = stationNode.get("lat").doubleValue() / 1000000.0;
            brStation.name = new NonLocalizedString(stationNode.get("name").textValue());
            brStation.bikesAvailable = stationNode.get("bikes").intValue();
            brStation.spacesAvailable = stationNode.get("free").intValue();
            if (brStation.id != null) {
                out.add(brStation);
            }
        }
        synchronized (this) {
            stations = out;
        }
    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }
}
