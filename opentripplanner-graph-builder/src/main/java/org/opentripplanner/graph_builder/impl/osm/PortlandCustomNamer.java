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

package org.opentripplanner.graph_builder.impl.osm;

import java.util.HashSet;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * These rules were developed in consultation with Grant Humphries, PJ Houser, and Mele Sax-Barnett.
 * They describe which sidewalks and paths in the Portland area should be specially designated in
 * the narrative.
 * 
 * @author novalis
 * 
 */
public class PortlandCustomNamer implements CustomNamer {

    public static String[] STREET_SUFFIXES = { "Avenue", "Street", "Drive", "Court", "Highway",
            "Lane", "Way", "Place", "Road", "Boulevard", "Alley" };

    public static String[] PATH_WORDS = { "Trail", "Trails", "Greenway", "Esplanade", "Spur",
            "Loop" };

    private HashSet<PlainStreetEdge> nameByOrigin = new HashSet<PlainStreetEdge>();

    private HashSet<PlainStreetEdge> nameByDestination = new HashSet<PlainStreetEdge>();

    @Override
    public String name(OSMWay way, String defaultName) {
        if (!way.hasTag("name")) {
            // this is already a generated name, so there's no need to add any
            // additional data
            return defaultName;
        }
        if (way.isTag("footway", "sidewalk") || way.isTag("path", "sidewalk")) {
            if (isStreet(defaultName)) {
                return sidewalk(defaultName);
            }
        }
        String highway = way.getTag("highway");
        if ("footway".equals(highway) || "path".equals(highway) || "cycleway".equals(highway)) {
            if (!isObviouslyPath(defaultName)) {
                return path(defaultName);
            }
        }
        if ("pedestrian".equals(highway)) {
            return pedestrianStreet(defaultName);
        }
        return defaultName;
    }

    private boolean isStreet(String defaultName) {
        for (String suffix : STREET_SUFFIXES) {
            if (defaultName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isObviouslyPath(String defaultName) {
        for (String word : PATH_WORDS) {
            if (defaultName.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String path(String name) {
        if (!name.toLowerCase().contains("path")) {
            name = name + " (path)".intern();
        }
        return name;
    }

    private String pedestrianStreet(String name) {
        if (!name.toLowerCase().contains("pedestrian street")) {
            name = name + " (pedestrian street)".intern();
        }
        return name;

    }

    private String sidewalk(String name) {
        if (!name.toLowerCase().contains("sidewalk")) {
            name = name + " (sidewalk)".intern();
        }
        return name;

    }

    @Override
    public void nameWithEdge(OSMWay way, PlainStreetEdge edge) {
        if (!edge.hasBogusName()) {
            return; // this edge already has a real name so there is nothing to do
        }
        String highway = way.getTag("highway");
        if ("motorway_link".equals(highway) || "trunk_link".equals(highway)) {
            if (edge.back) {
                nameByDestination.add(edge);
            } else {
                nameByOrigin.add(edge);
            }
        } else if ("secondary_link".equals(highway) || "primary_link".equals(highway)
                || "tertiary_link".equals(highway)) {
            if (edge.back) {
                nameByOrigin.add(edge);
            } else {
                nameByDestination.add(edge);
            }
        }
    }

    @Override
    public void postprocess(Graph graph) {
        for (PlainStreetEdge e : nameByOrigin) {
            nameAccordingToOrigin(graph, e, 15);
        }
        for (PlainStreetEdge e : nameByDestination) {
            nameAccordingToDestination(graph, e, 15);
        }
    }

    private String nameAccordingToDestination(Graph graph, PlainStreetEdge e, int maxDepth) {
        if (maxDepth == 0) {
            return null;
        }
        Vertex toVertex = e.getToVertex();
        for (PlainStreetEdge out : IterableLibrary.filter(toVertex.getOutgoing(), PlainStreetEdge.class)) {
            if (out.hasBogusName()) {
                String name = nameAccordingToDestination(graph, out, maxDepth - 1);
                if (name == null) {
                    continue;
                }
                e.setName(name);
                return name;
            } else {
                String name = out.getName();
                e.setName(name);
                return name;
            }
        }
        return null;
    }

    private String nameAccordingToOrigin(Graph graph, PlainStreetEdge e, int maxDepth) {
        if (maxDepth == 0) {
            return null;
        }
        Vertex fromVertex = e.getFromVertex();
        for (PlainStreetEdge in : IterableLibrary.filter(fromVertex.getIncoming(), PlainStreetEdge.class)) {
            if (in.hasBogusName()) {
                String name = nameAccordingToOrigin(graph, in, maxDepth - 1);
                if (name == null) {
                    continue;
                }
                e.setName(name);
                return name;
            } else {
                String name = in.getName();
                e.setName(name);
                return name;
            }
        }
        return null;
    }

}
