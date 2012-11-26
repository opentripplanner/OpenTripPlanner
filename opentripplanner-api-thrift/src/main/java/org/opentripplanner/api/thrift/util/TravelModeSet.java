package org.opentripplanner.api.thrift.util;

import java.util.Collection;
import java.util.HashSet;

import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;


/**
 * A set of TravelModes.
 * @author avi
 */
public class TravelModeSet extends HashSet<TravelMode> {
	
	/**
	 * Serialization ID is required.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Default constructor.
	 */
	public TravelModeSet() {
		super();
	}
	
	/**
	 * Construct from a pre-initialized collection of modes.
	 * @param modes
	 */
	public TravelModeSet(Collection<TravelMode> modes) {
		super(modes);
	}

	/**
	 * Convert in to a TraverseModeSet.
	 * @return
	 */
	public TraverseModeSet toTraverseModeSet() {
		TraverseModeSet modeSet = new TraverseModeSet();
		for (TravelMode travelMode : this) {
			TraverseMode traverseMode = (new TravelModeWrapper(travelMode)).toTraverseMode();
			modeSet.setMode(traverseMode, true);
		}
		
		return modeSet;
	}
	
}