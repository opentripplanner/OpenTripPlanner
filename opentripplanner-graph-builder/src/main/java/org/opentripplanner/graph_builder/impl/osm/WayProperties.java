package org.opentripplanner.graph_builder.impl.osm;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * The common data for ways with a given set of tags: * the safety features *
 * the slope override
 * 
 * @author novalis
 * 
 */
public class WayProperties {


	private StreetTraversalPermission permission;

	/**
	 * How much safer (less safe) this way is than the default, represented in
	 * terms of something like DALYs lost per meter. The first element safety in
	 * the direction of the way and the second is safety in the opposite
	 * direction.
	 */
	private static final P2<Double> defaultSafetyFeatures = new P2<Double>(1.0, 1.0);

	private P2<Double> safetyFeatures = defaultSafetyFeatures;

	public void setSafetyFeatures(P2<Double> safetyFeatures) {
		this.safetyFeatures = safetyFeatures;
	}

	public P2<Double> getSafetyFeatures() {
		return safetyFeatures;
	}

	public void setPermission(StreetTraversalPermission permission) {
		this.permission = permission;
	}

	public StreetTraversalPermission getPermission() {
		return permission;
	}

}
