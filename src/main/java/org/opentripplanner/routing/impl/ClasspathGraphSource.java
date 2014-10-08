/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.io.File;
import java.io.InputStream;

import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A GraphSource where the graph is loaded from a serialized graph file in the classpath.
 * 
 * @author laurent
 */
public class ClasspathGraphSource extends FileGraphSource {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathGraphSource.class);

    public ClasspathGraphSource(String routerId, File path, LoadLevel loadLevel) {
        super(routerId, path, loadLevel);
    }

    @Override
    protected InputStream getGraphInputStream() {
        File graphFile = new File(path, GRAPH_FILENAME);
        LOG.debug("Loading graph from classpath at '{}'", graphFile.getPath());
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(graphFile.getPath());
    }

    @Override
    protected InputStream getConfigInputStream() {
        File configFile = new File(path, CONFIG_FILENAME);
        LOG.debug("Trying to load config on classpath at '{}'", configFile.getPath());
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(configFile.getPath());
    }

    @Override
    protected long getLastModified() {
        return 0L;
    }

}
