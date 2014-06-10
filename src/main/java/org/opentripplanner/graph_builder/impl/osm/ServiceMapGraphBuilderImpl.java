package org.opentripplanner.graph_builder.impl.osm;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

public class ServiceMapGraphBuilderImpl implements GraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceMapGraphBuilderImpl.class);

    private File path;

    private URL url;

    public ServiceMapGraphBuilderImpl(File path) {
        this.setPath(path);
    }

    public ServiceMapGraphBuilderImpl(URL url) {
        this.setUrl(url);
    }

    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    public List<String> provides() {
        return Arrays.asList("poi");
    }

    void setPath(File path) {
        this.path = path;
    }

    void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        BufferedReader reader = null;
        JSONArray jsonArray = new JSONArray();

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

        JSONParser jsonParser = new JSONParser();
        try {
            jsonArray = (JSONArray) jsonParser.parse(reader);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        for (Object jsonObject : jsonArray) {
            try {
                JSONObject object = (JSONObject) jsonObject;
                new PoiVertex(graph, "tprek-" + object.get("id"), (double) object.get("longitude"),
                        (double) object.get("latitude"), (String) object.get("name_fi"));
                LOG.info("Added POI with id tprek-" + object.get("id"));
            } catch (Exception e) {
                LOG.warn("Error in parsing POI {}", jsonObject);
            }
        }
    }

    @Override
    public void checkInputs() {
        if (path != null) {
            if (!path.exists()) {
                throw new RuntimeException("ServiceMap Path " + path + " does not exist.");
            }
            if (!path.canRead()) {
                throw new RuntimeException("ServiceMap Path " + path + " cannot be read.");
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
