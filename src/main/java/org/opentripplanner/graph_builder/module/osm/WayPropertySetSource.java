package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;

/**
 * Interface for populating a {@link WayPropertySet} that determine how OSM
 * streets can be traversed in various modes and named.
 *
 * @author bdferris, novalis, seime
 */
public interface WayPropertySetSource {

	void populateProperties(WayPropertySet wayPropertySet);

	/**
	 * Return the given WayPropertySetSource or throws IllegalArgumentException
	 * if an unknown type is specified
	 */
	static WayPropertySetSource fromConfig(String type) {
		// type is set to "default" by GraphBuilderParameters if not provided in
		// build-config.json
		if ("default".equals(type)) {
			return new DefaultWayPropertySetSource();
		} else if ("norway".equals(type)) {
			return new NorwayWayPropertySetSource();
		} else if ("uk".equals(type)) {
			return new UKWayPropertySetSource();
		} else if ("finland".equals(type)) {
			return new FinlandWayPropertySetSource();
		} else if ("germany".equals(type)) {
			return new GermanyWayPropertySetSource();
		}
		else {
			throw new IllegalArgumentException(String.format("Unknown osmWayPropertySet: '%s'", type));
		}
	}

	enum DrivingDirection {
		/**
		 * Specifies that cars go on the right hand side of the road. This is true for the US
		 * mainland Europe.
		 */
		RIGHT_HAND_TRAFFIC,
		/**
		 * Specifies that cars go on the left hand side of the road. This is true for the UK, Japan
		 * and Australia.
		 */
		LEFT_HAND_TRAFFIC
	}

	DrivingDirection drivingDirection();

	IntersectionTraversalCostModel getIntersectionTraversalCostModel();

	/**
	 * Returns true if through traffic for motor vehicles is not allowed.
	 */
	default boolean isMotorVehicleThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
		String motorVehicle = way.getTag("motor_vehicle");
		return isGeneralNoThroughTraffic(way) || "destination".equals(motorVehicle);
	}

	default boolean isBicycleNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
		String bicycle = way.getTag("bicycle");
		return isGeneralNoThroughTraffic(way) || "destination".equals(bicycle);
	}

	/**
	 * Returns true if through traffic for bicycle is not allowed.
	 */
	default boolean isGeneralNoThroughTraffic(OSMWithTags way) {
		String access = way.getTag("access");
		return "destination".equals(access) || "private".equals(access)
				|| "customers".equals(access) || "delivery".equals(access);
	}
}
