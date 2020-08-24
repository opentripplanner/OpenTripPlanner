package org.opentripplanner.graph_builder.module.time;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class clusterlist {

    private List<cluster> clusters;

    public clusterlist(String jsonPath) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream fileStream = new FileInputStream(jsonPath)) {
            this.clusters = mapper.readValue(fileStream, mapper.getTypeFactory().constructCollectionType(ArrayList.class, cluster.class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public clusterlist(File file) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream fileStream = new FileInputStream(file)) {
            this.clusters = mapper.readValue(fileStream, mapper.getTypeFactory().constructCollectionType(ArrayList.class, cluster.class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<cluster> getclusters() {
        return clusters;
    }

    public void setclusters(List<cluster> clusters) {
        this.clusters = clusters;
    }
}
