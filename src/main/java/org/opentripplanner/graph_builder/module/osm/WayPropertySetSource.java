package org.opentripplanner.graph_builder.module.osm;

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
}
