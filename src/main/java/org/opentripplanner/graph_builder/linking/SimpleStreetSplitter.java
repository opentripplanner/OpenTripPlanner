package org.opentripplanner.graph_builder.linking;

import java.util.Collection;

import com.google.common.collect.Iterables;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.PartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/** A class that links transit stops to streets by splitting the streets */
public class SimpleStreetSplitter {
	
	public static final int MAX_SEARCH_RADIUS_METERS = 1000;
	
	// if there is a vertex (intersection or existing split) within this many meters of 
	// where a split would occur, don't make another split.
	public static final int MAX_CORNER_DISTANCE_METERS = 10;
	
	private Graph graph;
	
	private HashGridSpatialIndex<StreetEdge> idx;
	
	private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
	
	public SimpleStreetSplitter (Graph graph) {
		this.graph = graph;
	}
	
	public void link () {
		// build a nice private spatial index, since we're adding and removing edges
		idx = new HashGridSpatialIndex<StreetEdge>();
		
		for (StreetEdge se : Iterables.filter(graph.getEdges(), StreetEdge.class)) {
			idx.insert(se.getGeometry().getEnvelopeInternal(), se);
		}
		
		for (TransitStop tstop : Iterables.filter(graph.getVertices(), TransitStop.class)) {
			// find nearby street edges
			// TODO: we used to use an expanding-envelope search, which is more efficient in
			// dense areas. but first let's see how inefficient this is. I suspect it's not too
			// bad and the gains in simplicity are considerable.
			double radiusDeg = SphericalDistanceLibrary.metersToDegrees(MAX_SEARCH_RADIUS_METERS);
			
			Envelope env = new Envelope(tstop.getCoordinate());
			
			// local equirectangular projection
			double xscale = Math.cos(tstop.getLat() * Math.PI / 180);
			
			env.expandBy(radiusDeg / xscale, radiusDeg);
			
			// find the best edge
			double bestEdgeDist = Double.POSITIVE_INFINITY;
			StreetEdge bestEdge = null;
			
			for (StreetEdge edge : idx.query(env)) {
				// only link to edges that are still in the graph
				if (!edge.getToVertex().getIncoming().contains(edge))
					continue;
			
				double dist = distance(tstop, edge, xscale);
				
				if (dist < radiusDeg && dist < bestEdgeDist) {
					bestEdgeDist = dist;
					bestEdge = edge;
				}
			}
			
			if (bestEdge != null)
				link(tstop, bestEdge, xscale);
		}
	}
	
	/** split the edge (and backEdge, if applicable) and link in the transit stop */
	private void link (TransitStop tstop, StreetEdge edge, double xscale) {
		// TODO: we've already built this line string, we should save it
		LineString orig = edge.getGeometry();
		LineString transformed = equirectangularProject(orig, xscale);
		LocationIndexedLine il = new LocationIndexedLine(transformed);
		LinearLocation ll = il.project(new Coordinate(tstop.getLon() * xscale, tstop.getLat()));
		
		// if we're very close to one end of the line or the other, or is endwise, don't bother to split,
		// cut to the chase and link directly
		if (ll.getSegmentIndex() == 0 && ll.getSegmentFraction() < 0.05) {
			makeLinkEdges(tstop, (StreetVertex) edge.getFromVertex());
			return;
		}
		else if (ll.getSegmentIndex() == orig.getNumPoints()) {
			makeLinkEdges(tstop, (StreetVertex) edge.getToVertex());
			return;
		}
		else if (ll.getSegmentIndex() == orig.getNumPoints() - 1 && ll.getSegmentFraction() > 0.95) {
			makeLinkEdges(tstop, (StreetVertex) edge.getToVertex());
		}
				
		// split the edge, get the split vertex
		SplitterVertex v0 = split(edge, ll);
		
		// check for a back edge
		Vertex fromv = edge.getFromVertex();
		
		// already split links have differing from and to vertices
		if (fromv instanceof SplitterVertex)
			fromv = ((SplitterVertex) fromv).opposite;
		
		Vertex tov = edge.getToVertex();
		
		if (tov instanceof SplitterVertex)
			tov = ((SplitterVertex) tov).opposite;
		
		// if either of these are null we have a one-way edge that has been split
		// FIXME: pedestrians can walk in both directions on every edge, no?
		if (fromv != null && tov != null) {
			for (StreetEdge back : Iterables.filter(tov.getOutgoing(), StreetEdge.class)) {
				if (back.getToVertex() == fromv) {
					orig = back.getGeometry();
					transformed = equirectangularProject(orig, xscale);
					il = new LocationIndexedLine(transformed);
					ll = il.project(new Coordinate(tstop.getLon() * xscale, tstop.getLat()));
					
					SplitterVertex v1 = split(back, ll);
					
					v1.opposite = v0;
					v0.opposite = v1;
				}
			}
		}
		
		// if there was a back edge, this will make link edge to it as well
		makeLinkEdges(tstop, v0);
	}
	
	/** Split the street edge at the given fraction */
	private SplitterVertex split (StreetEdge edge, LinearLocation ll) {
		LineString geometry = edge.getGeometry();
		
		// create the geometries
		Coordinate splitPoint = ll.getCoordinate(geometry);
		
		// every edge can be split exactly once, so this is a valid label
		SplitterVertex v = new SplitterVertex(graph, "split from " + edge.getId(), splitPoint.x, splitPoint.y);
		
		// make the edges
		// TODO this is using the StreetEdge implementation of split, which will discard elevation information
		// on edges that have it
		P2<StreetEdge> edges = edge.split(v);
		
		// update indices
		idx.insert(edges.first.getGeometry().getEnvelopeInternal(), edges.first);
		idx.insert(edges.second.getGeometry().getEnvelopeInternal(), edges.second);
		
		// (no need to remove original edge, we filter it when it comes out of the index)
		
		// remove original edge
		edge.getToVertex().removeIncoming(edge);
		edge.getFromVertex().removeOutgoing(edge);
		
		return v;
	}
	
	/** 
	 * Make link edges. Also handles ensuring that if we snap to an existing split we get linked to both sides
	 * of it.
	 */
	private void makeLinkEdges (TransitStop tstop, StreetVertex v) {
		// TODO is this the proper source for the wheelchair info?
		new StreetTransitLink(tstop, v, tstop.hasWheelchairEntrance());
		new StreetTransitLink(v, tstop, tstop.hasWheelchairEntrance());
		
		if (v instanceof SplitterVertex) {
			SplitterVertex sv = (SplitterVertex) v;
			
			if (sv.opposite != null) {
				new StreetTransitLink(tstop, sv.opposite, tstop.hasWheelchairEntrance());
				new StreetTransitLink(sv.opposite, tstop, tstop.hasWheelchairEntrance());
			}				
		}
	}
	
	/** projected distance from stop to edge */
	private static double distance (TransitStop tstop, StreetEdge edge, double xscale) {
		// use JTS internal tools wherever possible
		LineString transformed = equirectangularProject(edge.getGeometry(), xscale);
		return transformed.distance(geometryFactory.createPoint(new Coordinate(tstop.getLon() * xscale, tstop.getLat())));
	}
	
	private static LineString equirectangularProject(LineString geometry, double xscale) {
		Coordinate[] coords = new Coordinate[geometry.getNumPoints()];
		
		for (int i = 0; i < coords.length; i++) {
			Coordinate c = geometry.getCoordinateN(i);
			c = (Coordinate) c.clone();
			c.x *= xscale;
			coords[i] = c;
		}
		
		return geometryFactory.createLineString(coords);
	}
}
