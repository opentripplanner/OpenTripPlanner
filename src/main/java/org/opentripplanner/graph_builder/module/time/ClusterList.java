package org.opentripplanner.graph_builder.module.time;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ClusterList {

    private List<Cluster> clusters;

    public ClusterList(String jsonPath) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream fileStream = new FileInputStream(jsonPath)) {
            this.clusters = mapper.readValue(fileStream, mapper.getTypeFactory().constructCollectionType(ArrayList.class, Cluster.class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public ClusterList(File file) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream fileStream = new FileInputStream(file)) {
            this.clusters = mapper.readValue(fileStream, mapper.getTypeFactory().constructCollectionType(ArrayList.class, Cluster.class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Cluster c : this.getclusters()){
            if (Array.getLength(c.gettimetable())==0)
                c.settimetable(null);
        }
    }

    public List<Cluster> getclusters() {
        return clusters;
    }

    public void setclusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }
}
