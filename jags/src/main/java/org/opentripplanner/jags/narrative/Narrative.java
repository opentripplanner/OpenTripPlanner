package org.opentripplanner.jags.narrative;

import org.opentripplanner.jags.spt.SPTEdge;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.edgetype.Street;
import org.opentripplanner.jags.edgetype.Walkable;
import org.opentripplanner.jags.edgetype.Hop;

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

/**
 * A narrative section represents the portion of a trip which takes place on a
 * single conveyance. For example, a trip from Avenue H on the Q to Prince St on
 * the R transferring at Canal St would be represented as a NarrativeSection
 * from Avenue H to Canal and another from Canal to Prince. Normally, there
 * would also be a NarrativeSection for the walk to the Avenue H station and
 * from the Prince St station to the final destination.
 */
class NarrativeSection {
	public Vector<NarrativeItem> items;
	
	TransportationMode mode;
	String name;
	String direction;	
	
	public NarrativeSection(Vector<SPTEdge> edges) {
		items = new Vector<NarrativeItem>();
		Walkable walkable = edges.elementAt(0).payload;
		mode = walkable.getMode(); 
		if (walkable instanceof Hop) {
			name = walkable.getName();
			direction = walkable.getDirection();
			BasicNarrativeItem item = new BasicNarrativeItem();
			item.setDirection(walkable.getDirection());
			item.setDirection(walkable.getDirection());
			item.setName(walkable.getName());
			String end = walkable.getEnd();
			Geometry geom = walkable.getGeometry();
			
			double totalDistance = walkable.getDistanceKm();
			for (SPTEdge edge : edges.subList(1, edges.size())) {
				walkable = edge.payload;
				totalDistance += walkable.getDistanceKm();
				geom = geom.union(walkable.getGeometry());			
				end = walkable.getEnd();
			}
			item.setGeometry(geom);
			item.setDistance(totalDistance);
			item.setEnd(end);
			items.add(item);
		} else if (walkable instanceof Street) {
			name = "walk";
			direction = "FIXME: compute from start/end geometry"; 
			double totalDistance = 0;
			String lastStreet = null;
			BasicNarrativeItem item = null;
			for (SPTEdge edge : edges) {
				walkable = edge.payload;
				String streetName = walkable.getName();
				if (streetName == lastStreet) {
					totalDistance += walkable.getDistanceKm(); 
					item.setDistance(totalDistance);
					item.setGeometry(item.getGeometry().union(walkable.getGeometry()));
					item.setEnd(walkable.getStart());
					continue;
				}
				item = new BasicNarrativeItem();
				item.setName(streetName);
				item.setDirection(walkable.getDirection());
				item.setGeometry(walkable.getGeometry());
				item.setStart(walkable.getStart());
			}
		}
	}

	public String asText() {
		return direction + " on " + name + " via " + mode + "(" + items.size() + " items)";
	}
}

public class Narrative {
	protected GraphPath path;
	protected Vector<NarrativeSection> sections = null;
	
	public Narrative(GraphPath path) {
		this.path = path;
		if (path.edges.size() == 0) {
			return;
		}
		this.sections = new Vector<NarrativeSection>();
		
		String lastName = path.edges.elementAt(0).payload.getName();
		Vector<SPTEdge> currentSection = new Vector<SPTEdge>();
		for (SPTEdge edge : path.edges) {
			if (edge.payload.getName() == lastName) { 
				currentSection.add(edge);
			} else {
				sections.add(new NarrativeSection(currentSection));
				currentSection.clear();
			}
		}
		if (currentSection.size() > 0) {
			sections.add(new NarrativeSection(currentSection));
		}
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
