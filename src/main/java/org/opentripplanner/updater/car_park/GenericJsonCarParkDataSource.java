package org.opentripplanner.updater.car_park;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetch car park JSON feeds and pass each record on to the specific car park subclass
 *
 * @see CarParkDataSource
 */
public abstract class GenericJsonCarParkDataSource implements CarParkDataSource,
    JsonConfigurable {

    private static final Logger log = LoggerFactory.getLogger(GenericJsonCarParkDataSource.class);
    private String url;
    private String apiKey;

    private String jsonParsePath;

    ArrayList<CarPark> carParks = new ArrayList<>();

    /**
     * Construct superclass
     *
     * @param JSON path to get from enclosing elements to nested park list.
     *        Separate path levels with '/' For example "d/list"
     *
     */
    public GenericJsonCarParkDataSource(String jsonPath) {
        jsonParsePath = jsonPath;
        apiKey = null;
    }

    /**
     * Construct superclass
     *
     * @param JSON path to get from enclosing elements to nested park list.
     *        Separate path levels with '/' For example "d/list"
     * @param Api key, when used by car park type
     *
     */
    public GenericJsonCarParkDataSource(String jsonPath, String apiKeyValue) {
        jsonParsePath = jsonPath;
        apiKey = apiKeyValue;
    }

    /**
     * Construct superclass where park list is on the top level of JSON code
     *
     */
    public GenericJsonCarParkDataSource() {
        jsonParsePath = "";
    }

    @Override
    public boolean update() {
        try {
            InputStream data = null;

            URL url2 = new URL(url);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                data = HttpUtils.getData(url, "ApiKey", apiKey);
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
            log.warn("Error parsing car park feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing car park feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading car park feed from " + url, e);
            return false;
        }
        return true;
    }

    private void parseJSON(InputStream dataStream) throws JsonProcessingException,
        IllegalArgumentException, IOException {

        ArrayList<CarPark> out = new ArrayList<>();

        String parkString = convertStreamToString(dataStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(parkString);

        if (!jsonParsePath.equals("")) {
            String delimiter = "/";
            String[] parseElement = jsonParsePath.split(delimiter);
            for(int i =0; i < parseElement.length ; i++) {
                rootNode = rootNode.path(parseElement[i]);
            }

            if (rootNode.isMissingNode()) {
                throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
            }
        }

        for (int i = 0; i < rootNode.size(); i++) {
            JsonNode node = rootNode.get(i);
            if (node == null) {
                continue;
            }
            CarPark carPark = makeCarPark(node);
            if (carPark != null)
                out.add(carPark);
        }
        synchronized(this) {
            carParks = out;
        }
    }

    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner scanner = null;
        String result="";
        try {

            scanner = new java.util.Scanner(is).useDelimiter("\\A");
            result = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        finally
        {
            if(scanner!=null)
                scanner.close();
        }
        return result;

    }

    @Override
    public synchronized List<CarPark> getCarParks() {
        return carParks;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public abstract CarPark makeCarPark(JsonNode carParkNode);

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        String url = jsonNode.path("url").asText(); // path() returns MissingNode not null.
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        setUrl(url);
    }
}

