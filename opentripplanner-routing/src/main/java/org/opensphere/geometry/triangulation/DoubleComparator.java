/*
 * This file is part of the OpenSphere project which aims to
 * develop geospatial algorithms.
 * 
 * Copyright (C) 2012 Eric Grosso
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * For more information, contact:
 * Eric Grosso, eric.grosso.os@gmail.com
 * 
 */
package org.opensphere.geometry.triangulation;

import java.util.Comparator;
import java.util.Map;

import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;

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