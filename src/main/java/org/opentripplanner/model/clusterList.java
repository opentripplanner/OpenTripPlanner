import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class clusterList {
   private ArrayList<cluster> clusters;
    public ckusterList(String jsonPath) {
        ObjectMapper mapper = new ObjectMapper();
        try(InputStream fileStream = new FileInputStream(jsonPath)) {
            this.clusters= mapper.readValue(fileStream, mapper.getTypeFactory().constructCollectionType(ArrayList.class, cluster.class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.clusters = clusters;
    }

    public ArrayList<cluster> getClusters() {
        return clusters;
    }

    public void setClusters(ArrayList<cluster> clusters) {
        this.clusters = clusters;
    }
}
