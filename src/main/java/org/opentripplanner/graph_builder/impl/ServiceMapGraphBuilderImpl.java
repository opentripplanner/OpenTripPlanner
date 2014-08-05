package org.opentripplanner.graph_builder.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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

    public static final String POI_PREFIX = "tprek:";

    private File path;

    private URL url;

    public ServiceMapGraphBuilderImpl(File path) {
        this.setPath(path);
    }

    public ServiceMapGraphBuilderImpl(URL url) {
        this.setUrl(url);
    }

    void setPath(File path) {
        this.path = path;
    }

    void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("poi");
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
        for (Object jsonObject : jsonArray) {
            try {
                JSONObject object = (JSONObject) jsonObject;
                PoiVertex pv = new PoiVertex(graph, POI_PREFIX + object.get("id"), (double) object.get("longitude"),
                        (double) object.get("latitude"), (String) object.get("name_fi"));
                List<Object> serviceIdList = new LinkedList<>();
                for (Object s :(JSONArray)object.get("service_ids")){
                    serviceIdList.add(POI_PREFIX + s.toString());
                }
                pv.setCategories(serviceIdList);
            } catch (Exception e) {
                LOG.warn("Error in parsing POI {}", jsonObject);
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
