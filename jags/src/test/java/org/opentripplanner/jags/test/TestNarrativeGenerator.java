package org.opentripplanner.jags.test;

import java.util.GregorianCalendar;
import java.util.Vector;

import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.edgetype.loader.NetworkLinker;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.gtfs.PackagedFeed;
import org.opentripplanner.jags.narrative.Narrative;
import org.opentripplanner.jags.narrative.NarrativeSection;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.SPTEdge;
import org.opentripplanner.jags.spt.ShortestPathTree;

import junit.framework.TestCase;

public class TestNarrativeGenerator extends TestCase {

	Graph graph;
	
	public void setUp() {
		try {
			Feed feed = new Feed(new PackagedFeed( "google_transit.zip" ));
			graph = new Graph();
			GTFSHopLoader hl = new GTFSHopLoader(graph, feed);
			hl.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		NetworkLinker nl = new NetworkLinker(graph);
		nl.createLinkage();
	}
	
	public void testNarrativeGenerator() {

		Vertex airport = graph.getVertex("10579");
		ShortestPathTree spt = Dijkstra.getShortestPathTree(graph, 
				   "6876", 
				   airport.label, 
				   new State(new GregorianCalendar(2009,10,15,12,36,0)), 
				   new WalkOptions());

		
		Narrative narrative = new Narrative(path);
		Vector<NarrativeSection> sections = narrative.getSections();
		assertEquals (3, sections.size());
		assertEquals (TransportationMode.BUS, sections.elementAt(0).getMode());
		assertEquals (TransportationMode.TRAM, sections.elementAt(1).getMode());
	}
}
