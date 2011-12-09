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

package org.opentripplanner.routing.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphSerializationLibrary {
    
    private static Logger _log = LoggerFactory.getLogger(GraphSerializationLibrary.class);
    private ClassLoader classLoader;

    public GraphSerializationLibrary() {
        classLoader =  getClass().getClassLoader();
    }

    /* for org.opentripplanner.customize */
    public GraphSerializationLibrary(ClassLoader loader) {
        classLoader = loader;
    }

    public static void writeGraph(Graph graph, File graphPath) throws IOException {

        if (!graphPath.getParentFile().exists())
            if (!graphPath.getParentFile().mkdirs()) {
                _log.error("Failed to create directories for graph bundle at " + graphPath);
            }

        _log.info("Main graph size: |V|={} |E|={}", graph.countVertices(), graph.countEdges());
        _log.info("Writing graph " + graphPath.getAbsolutePath() + " ...");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(graphPath)));
        out.writeObject(graph);
        out.close();
        _log.info("Graph written.");
    }

    /* for org.opentripplanner.customize */
    class GraphObjectInputStream extends ObjectInputStream {

        public GraphObjectInputStream(InputStream in) throws IOException {
            super(in);
        }
        @Override
        public Class<?> resolveClass(ObjectStreamClass osc) {
            try {
                return Class.forName(osc.getName(), false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
    }
    
    public Graph readGraph(File graphPath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new GraphObjectInputStream(new BufferedInputStream (new FileInputStream(graphPath)));
        _log.info("Reading graph " + graphPath.getAbsolutePath() + " ...");
        try {
        	Graph graph= (Graph) in.readObject();
    		_log.info("Graph read");
    	        _log.info("Main graph size: |V|={} |E|={}", graph.countVertices(), graph.countEdges());
    		return graph;
    	} catch (InvalidClassException ex) {
    		_log.error("Stored graph is incompatible with this version of OTP, please rebuild it.");
    		throw new IllegalStateException("Stored Graph version error", ex);
    	}
    }
}
