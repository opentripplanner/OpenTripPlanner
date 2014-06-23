package org.opentripplanner.graph_builder.impl;


import lombok.Setter;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.PoiVertex;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceMapAccessibilityGraphBuilderImpl implements GraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceMapAccessibilityGraphBuilderImpl.class);

    public static final String POI_PREFIX = "poi:tprek:";

    @Setter
    private File path;

    @Setter
    private URL url;

    public ServiceMapAccessibilityGraphBuilderImpl(File path) {
        this.setPath(path);
    }

    public ServiceMapAccessibilityGraphBuilderImpl(URL url) {
        this.setUrl(url);
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("poi");
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("poi_accessibility");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        BufferedReader reader = getBufferedReader();

        JSONParser jsonParser = new JSONParser();
        JSONArray jsonArray = new JSONArray();

        try {
            jsonArray = (JSONArray) jsonParser.parse(reader);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        Map<String, Map> pois = new HashMap<>();

        for (Object jsonObject : jsonArray) {
            try {
                JSONObject object = (JSONObject) jsonObject;
                String id = object.get("unit_id").toString();
                Map<String, String> map;
                if ((map = pois.get(id)) == null){
                    map = new HashMap<>();
                    pois.put(id, map);
                }
                map.put((String) object.get("variable_name"), (String) object.get("value"));
            } catch (Exception e) {
                LOG.warn("Error in parsing POI {}", jsonObject);
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, Map> entry : pois.entrySet()) {
            PoiVertex pv = (PoiVertex) graph.getVertex(POI_PREFIX + entry.getKey());
            if (pv != null){
                pv.setAccessibilityViewpoints(entry.getValue());
                LOG.debug(String.valueOf(entry.getValue()));
            } else {
                LOG.warn("Could not find: " + entry.getKey());
            }
        }
    }

    private BufferedReader getBufferedReader() {
        BufferedReader reader = null;
        if (path != null) {
            try {
                reader = new BufferedReader(new FileReader(path));

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                InputStream is = url.openConnection().getInputStream();
                reader = new BufferedReader(new InputStreamReader(is));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reader;
    }

    @Override
    public void checkInputs() {
        if (path != null) {
            if (!path.exists()) {
                throw new RuntimeException("ServiceMap path " + path + " does not exist.");
            }
            if (!path.canRead()) {
                throw new RuntimeException("ServiceMap path " + path + " cannot be read.");
            }
        } else if (url != null) {
            try {
                HttpUtils.testUrl(url.toExternalForm());
            } catch (ClientProtocolException e) {
                throw new RuntimeException("Error connecting to " + url.toExternalForm() + "\n" + e);
            } catch (IOException e) {
                throw new RuntimeException("ServiceMap url " + url.toExternalForm()
                        + " cannot be read.\n" + e);
            }
        }
    }
}
