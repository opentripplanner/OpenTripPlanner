package org.opentripplanner.graph_builder.module.osm;

/**
 * Interface for populating a {@link WayPropertySet} that determine how OSM
 * streets can be traversed in various modes and named.
 *
 * @author bdferris, novalis, seime
 */
public interface WayPropertySetSource {

	public void populateProperties(WayPropertySet wayPropertySet);

	/**
	 * Return the given WayPropertySetSource or throws IllegalArgumentException
	 * if an unkown type is specified
	 */
	public static WayPropertySetSource fromConfig(String type) {
		// type is set to "default" by GraphBuilderParameters if not provided in
		// build-config.json
		if ("default".equals(type)) {
			return new DefaultWayPropertySetSource();
		} else if ("norway".equals(type)) {
			return new NorwayWayPropertySetSource();
		} else if ("uk".equals(type)) {
			return new UKWayPropertySetSource();
		} else {
			throw new IllegalArgumentException(String.format("Unknown osmWayPropertySet: '%s'", type));
		}
	}

}
