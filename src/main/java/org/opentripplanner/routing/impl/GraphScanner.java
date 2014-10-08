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
import java.util.List;

import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan for graphs under the base directory and auto-register them.
 * 
 * @see GraphServiceImpl
 */
public class GraphScanner {

    private static final Logger LOG = LoggerFactory.getLogger(GraphScanner.class);

    /** Where to look for graphs */
    public File basePath = new File("/var/otp/graphs");

    /** A list of routerIds to automatically register and load at startup */
    public List<String> autoRegister;

    /** The default router, "" by default */
    public String defaultRouterId = "";

    /** If true, on startup register the graph in the location defaultRouterId. */
    public boolean attemptRegisterDefault = true;

    /** Load level */
    public LoadLevel loadLevel = LoadLevel.FULL;

    /** The GraphService where register graphs to */
    private GraphService graphService;

    public GraphScanner(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Based on the autoRegister list, automatically register all routerIds for which we can find a
     * graph file in a subdirectory of the resourceBase path. Also register and load the graph for
     * the defaultRouterId and warn if no routerIds are registered.
     */
    public void startup() {
        graphService.setDefaultRouterId(defaultRouterId);
        if (autoRegister != null && !autoRegister.isEmpty()) {
            LOG.info("attempting to automatically register routerIds {}", autoRegister);
            LOG.info("graph files will be sought in paths relative to {}", basePath);
            for (String routerId : autoRegister) {
                FileGraphSource graphSource = new FileGraphSource(routerId, getBasePath(routerId),
                        loadLevel);
                if (graphSource.getGraph() != null)
                    graphService.registerGraph(routerId, graphSource);
            }
        } else {
            LOG.info("no list of routerIds was provided for automatic registration.");
        }
        if (attemptRegisterDefault
                && (autoRegister == null || !autoRegister.contains(defaultRouterId))) {
            LOG.info("Attempting to load graph for default routerId '{}'.", defaultRouterId);
            FileGraphSource graphSource = new FileGraphSource(defaultRouterId,
                    getBasePath(defaultRouterId), loadLevel);
            if (graphSource.getGraph() != null)
                graphService.registerGraph(defaultRouterId, graphSource);
        }
    }

    private File getBasePath(String routerId) {
        return new File(basePath, routerId);
    }
}
