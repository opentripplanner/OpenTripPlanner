/* This file is based on code copied from project OpenSphere, see the LICENSE file for further information. */
package org.opensphere.geometry.triangulation;

import java.util.Comparator;
import java.util.Map;

import org.locationtech.jts.triangulate.quadedge.QuadEdge;

/**
 * Comparator of a map containing QuadEdge as key
 * and Double as value (Double comparator).
 * 
 * @author Eric Grosso
 *
 */
public class DoubleComparator implements Comparator<QuadEdge> {
	
	Map<QuadEdge,Double> map;
	
	/**
	 * Constructor.
	 * 
	 * @param map
	 * 		map containing QuadEdge and Double
	 */
	public DoubleComparator(Map<QuadEdge,Double> map) {
		this.map = map;
	}

	/**
	 * Method of comparison.
	 * 
	 * @param qeA
	 * 		quad edge to compare
	 * @param qeB
	 * 		quad edge to compare
	 * @return
	 * 		1 if double value associated to qeA  < double
	 * 		value associated to qeB,
	 * 		0 if values are equals,
	 * 		-1 otherwise
	 */
	@Override
	public int compare(QuadEdge qeA, QuadEdge qeB) {
        final double distanceA = this.map.get(qeA);
        final double distanceB = this.map.get(qeB);
        if (distanceA < distanceB) {
			return 1;
		} else if (distanceA == distanceB) {
			return 0;
		} else {
			return -1;
		}
	}
	
}