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

package org.opentripplanner.graph_builder.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import lombok.Setter;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embed inside the graph a default configuration used when decorating the graph during load.
 * 
 */
public class EmbeddedConfigGraphBuilderImpl implements GraphBuilder {

    @Setter
    private File propertiesFile;

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedConfigGraphBuilderImpl.class);

    public void setPropertiesPath(String propertiesPath) {
        propertiesFile = new File(propertiesPath);
    }

    /**
     * An set of ids which identifies what stages this graph builder provides (i.e. streets,
     * elevation, transit)
     */
    public List<String> provides() {
        return Collections.emptyList();
    }

    /** A list of ids of stages which must be provided before this stage */
    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        try {
            LOG.info("Bundling config '" + propertiesFile.getPath() + "' into graph.");
            Properties props = new Properties();
            props.load(new FileInputStream(propertiesFile));
            graph.setEmbeddedPreferences(props);
        } catch (IOException e) {
            LOG.error("Can't load properties from '" + propertiesFile.getAbsolutePath() + "'", e);
        }
    }

    @Override
    public void checkInputs() {
        if (!propertiesFile.canRead()) {
            throw new IllegalArgumentException("Configuration '" + propertiesFile.getAbsolutePath()
                    + "' can't be read.");
        }
    }
}
