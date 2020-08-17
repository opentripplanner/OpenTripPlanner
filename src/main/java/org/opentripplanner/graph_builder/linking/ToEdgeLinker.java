package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.*;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;

import java.util.ArrayList;
import java.util.List;

/**
 * Links vertex to edge in graph at a given fraction
 */
public class ToEdgeLinker {

    private final StreetEdgeFactory edgeFactory;

    private final StreetSplitter splitter;

    private final EdgesMaker edgesMaker;

    private final LinkingGeoTools linkingGeoTools;

    private final boolean addExtraEdgesToAreas;

    public ToEdgeLinker(StreetEdgeFactory edgeFactory, StreetSplitter splitter, EdgesMaker edgesMaker,
                        LinkingGeoTools linkingGeoTools, boolean addExtraEdgesToAreas) {
        this.edgeFactory = edgeFactory;
        this.splitter = splitter;
        this.edgesMaker = edgesMaker;
        this.linkingGeoTools = linkingGeoTools;
        this.addExtraEdgesToAreas = addExtraEdgesToAreas;
    }

    /**
     * Temporarily split edge at given location and link vertex to that split (make connection from `origin` or to `destination`)
     */
    public void linkVertexToEdgeTemporarily(TemporaryStreetLocation temporaryVertex, StreetEdge edge, LinearLocation ll) {
        SplitterVertex splitterVertex = splitter.splitTemporarily(edge, ll, temporaryVertex.isEndVertex());
        edgesMaker.makeTemporaryEdges(temporaryVertex, splitterVertex);
    }

    /**
     * Temporarily split edge at given location and link vertex to that split both ways (make connections both to and from `temporaryVertex`)
     */
    public void linkVertexToEdgeBothWaysTemporarily(TemporaryRentVehicleVertex temporaryVertex, StreetEdge edge, LinearLocation ll) {
        SplitterVertex splitterVertex = splitter.splitTemporarilyWithRentVehicleSplitterVertex(edge, ll);
        edgesMaker.makeTemporaryEdgesBothWays(temporaryVertex, splitterVertex);
    }

    /**
     * Permanently split edge at given location and link vertex to that split (make connections both to and from
     * `vertex` and remove original edge)
     */
    public void linkVertexToEdgePermanently(Vertex vertex, StreetEdge edge, LinearLocation ll) {
        SplitterVertex splitterVertex = splitter.splitPermanently(edge, ll);
        edgesMaker.makePermanentEdges(vertex, splitterVertex);

        // If splitter vertex is part of area; link splittervertex to all other vertexes in area, this creates
        // edges that were missed by WalkableAreaBuilder
        if (edge instanceof AreaEdge && vertex instanceof TransitStop && this.addExtraEdgesToAreas) {
            linkVertexToAreaVertices(splitterVertex, ((AreaEdge) edge).getArea());
        }
    }

    /**
     * Link vertex to all vertices in area/platform
     */
    private void linkVertexToAreaVertices(Vertex splitterVertex, AreaEdgeList area) {
        List<Vertex> vertices = new ArrayList<>();

        for (AreaEdge areaEdge : area.getEdges()) {
            if (!vertices.contains(areaEdge.getToVertex())) vertices.add(areaEdge.getToVertex());
            if (!vertices.contains(areaEdge.getFromVertex())) vertices.add(areaEdge.getFromVertex());
        }

        for (Vertex vertex : vertices) {
            if (vertex instanceof StreetVertex && !vertex.equals(splitterVertex)) {
                LineString line = linkingGeoTools.createLineString(splitterVertex, vertex);
                double length = SphericalDistanceLibrary.distance(splitterVertex.getCoordinate(), vertex.getCoordinate());
                I18NString name = new LocalizedString("", new OSMWithTags());
                edgeFactory.createAreaEdge((IntersectionVertex) splitterVertex, (IntersectionVertex) vertex, line, name, length, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area);
                edgeFactory.createAreaEdge((IntersectionVertex) vertex, (IntersectionVertex) splitterVertex, line, name, length, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area);
            }
        }
    }
}
