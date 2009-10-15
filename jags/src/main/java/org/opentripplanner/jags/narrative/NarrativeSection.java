package org.opentripplanner.jags.narrative;

import java.util.Vector;

import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.Street;
import org.opentripplanner.jags.edgetype.Walkable;
import org.opentripplanner.jags.spt.SPTEdge;

import com.vividsolutions.jts.geom.Geometry;


/**
 * A narrative section represents the portion of a trip which takes place on a
 * single conveyance. For example, a trip from Avenue H on the Q to Prince St on
 * the R transferring at Canal St would be represented as a NarrativeSection
 * from Avenue H to Canal and another from Canal to Prince. Normally, there
 * would also be a NarrativeSection for the walk to the Avenue H station and
 * from the Prince St station to the final destination.
 */
public class NarrativeSection {
	public Vector<NarrativeItem> items;
	
	TransportationMode mode;
	String name;
	String direction;	
	
	public Vector<NarrativeItem> getItems() {
		return items;
	}

	public TransportationMode getMode() {
		return mode;
	}

	public String getName() {
		return name;
	}

	public String getDirection() {
		return direction;
	}

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
		if (mode == TransportationMode.TRANSFER) {
			return "transfer";
		}
		return direction + " on " + name + " via " + mode + "(" + items.size() + " items)";
	}
}
