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

import com.conveyal.geojson.GeoJsonModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.notes.OutOfAreaNotesService;

import java.io.File;;
import java.io.IOException;
import java.util.HashMap;

public class OutOfAreaModule implements GraphBuilderModule {

    private File geojsonFile;

    private String startMessage, endMessage;

    public OutOfAreaModule(File geojsonFile, String startMessage, String endMessage) {
        this.geojsonFile = geojsonFile;
        this.startMessage = startMessage;
        this.endMessage = endMessage;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        Geometry area;
        try {
            area = new ObjectMapper().registerModule(new GeoJsonModule())
                    .readValue(geojsonFile, Geometry.class);
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }
        OutOfAreaNotesService svc = new OutOfAreaNotesService();
        svc.setArea(area);
        svc.setStartLocationMessage(startMessage);
        svc.setEndLocationMessage(endMessage);
        graph.outOfAreaNotesService = svc;
    }

    @Override
    public void checkInputs() {
    }
}
