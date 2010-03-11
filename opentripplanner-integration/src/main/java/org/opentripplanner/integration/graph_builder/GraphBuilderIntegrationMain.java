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

package org.opentripplanner.integration.graph_builder;

import java.io.IOException;

import org.opentripplanner.graph_builder.GraphBuilderMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphBuilderIntegrationMain {

  private static Logger _log = LoggerFactory.getLogger(GraphBuilderIntegrationMain.class);

  private static final String GRAPH_BUILDER_CONFIG_PATH = "graph_builder.config.path";

  public static void main(String[] args) throws IOException {

    String configPath = "src/main/resources/graph-builder.xml";

    String fromProperty = System.getProperty(GRAPH_BUILDER_CONFIG_PATH);
    if (fromProperty != null) {
      _log.info("reading graph builder config path from property "
          + GRAPH_BUILDER_CONFIG_PATH);
      configPath = fromProperty;
    }
    _log.info("graph builder config path: " + configPath);

    GraphBuilderMain.main(new String[] {configPath});
  }
}
