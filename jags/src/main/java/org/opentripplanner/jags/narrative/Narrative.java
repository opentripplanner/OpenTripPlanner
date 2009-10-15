package org.opentripplanner.jags.narrative;

import org.opentripplanner.jags.spt.SPTEdge;
import org.opentripplanner.jags.spt.GraphPath;

import com.vividsolutions.jts.geom.Geometry;

import java.util.Vector;


/* A NarrativeItem represents a particular line in turn-by-turn directions */
interface NarrativeItem {
	String getName(); // 2, Q, Red Line, R5, Rector St.
	String getDirection(); // 241 St-Wakefield Sta, Inbound, Northwest
	String getTowards();
	Geometry getGeometry();
	String getStart();
	String getEnd();
	double getDistanceKm();
}

class BasicNarrativeItem implements NarrativeItem {
	private String name;
	private String direction;
	private Geometry geometry;
	private String start;
	private String end;
	private double distance;
	
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public String getDirection() {
		return direction;
	}
	public String getTowards() {
		return null;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public Geometry getGeometry() {
		return geometry;
	}
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	public void addGeometry(Geometry geometry) {
		this.geometry = this.geometry.union(geometry);
	}
	public String getStart() {
		return start;
	}
	public String getEnd() {
		return end;
	}
	public void setStart(String start) {
		this.start = start;
	}
	public void setEnd(String end) {
		this.end = end;
	}
	public double getDistanceKm() {
		return distance;
	}
}

public class Narrative {
	protected GraphPath path;
	protected Vector<NarrativeSection> sections = null;
	
	public Vector<NarrativeSection> getSections() {
		return sections;
	}

	public Narrative(GraphPath path) {
		this.path = path;
		if (path.edges.size() == 0) {
			return;
		}
		this.sections = new Vector<NarrativeSection>();
		
		String lastName = path.edges.elementAt(0).payload.getName();
		Vector<SPTEdge> currentSection = new Vector<SPTEdge>();
		for (SPTEdge edge : path.edges) {
			String edgeName = edge.payload.getName();
			if (!edgeName.equals(lastName)) { 
				sections.add(new NarrativeSection(currentSection));
				currentSection.clear();
				lastName = edgeName;
			}
			currentSection.add(edge);
		}
		sections.add(new NarrativeSection(currentSection));
	}

	public String asText() {
		if (sections == null) 
			return "No path";
		String out = "";
		for (NarrativeSection section : sections) {
			out += section.asText() + "\n";
		}
		return out;
	}

}
