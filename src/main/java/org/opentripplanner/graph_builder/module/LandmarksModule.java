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
package org.opentripplanner.graph_builder.module;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.Landmark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class LandmarksModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(LandmarksModule.class);

    private File landmarksFile;

    public LandmarksModule(File landmarksFile) {
        this.landmarksFile = landmarksFile;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TransitStationStop.class, new JsonDeserializer<TransitStationStop>() {
            @Override
            public TransitStationStop deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String label = jsonParser.getValueAsString();
                return (TransitStationStop) graph.getVertex(label);
            }
        });
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Landmark[] landmarks = new Landmark[0];
        try {
            landmarks = mapper.readValue(landmarksFile, Landmark[].class);
        } catch (IOException ex) {
            LOG.info("Error reading landmarks file: " + ex);
        }
        for (Landmark landmark : landmarks) {
            graph.addLandmark(landmark);
        }
    }

    @Override
    public void checkInputs() {
        // unused
    }
}