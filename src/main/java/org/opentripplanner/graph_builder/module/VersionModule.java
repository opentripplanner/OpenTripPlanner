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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.model.GraphVersion;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.json_serialization.GraphVersionDeserializer;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Register version information as a graph module.
 */
public class VersionModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(VersionModule.class);

  private File versionFile;

  public VersionModule(File versionFile) {
    this.versionFile = versionFile;
  }

  @Override
  public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(GraphVersion.class, new GraphVersionDeserializer());
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    GraphVersion graphVersion = null;
    try {
      graphVersion = mapper.readValue(versionFile, GraphVersion.class);
      graphVersion.setBuilderVersion(MavenVersion.VERSION);
      LOG.info("graphVersion created=" + graphVersion);
    } catch (IOException ex) {
      LOG.info("Error reading version file: " + ex);
    }
    graph.setGraphVersion(graphVersion);
  }

  @Override
  public void checkInputs() {
    // unused
  }
}