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
package org.opensphere.geometry.triangulation.model;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Vertex.
 * 
 * @author Eric Grosso
 *
 */
public class Vertex {

	/** ID of the vertex */
	private int id;

	/** Coordinate of the vertex */
	private Coordinate coordinate;

	/** Indicator to know if the vertex is a border vertex
	 * of the triangulation framework */
	private boolean border;

	/**
	 * Default constructor.
	 */
	public Vertex() {
		//
	}
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the vertex
	 */
	public Vertex(int id) {
		this.id = id;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the vertex
	 * @param coordinate
	 * 		coordinate of the vertex
	 */
	public Vertex(int id, Coordinate coordinate) {
		this.id = id;
		this.setCoordinate(coordinate);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the vertex
	 * @param border
	 * 		defines if the vertex is a border vertex
	 * 		or not in the triangulation framework
	 */
	public Vertex(int id, boolean border) {
		this.id = id;
		this.border = border;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the vertex
	 * @param coordinate
	 * 		coordinate of the vertex
	 * @param border
	 * 		defines if the vertex is a border vertex
	 * 		or not in the triangulation framework
	 */
	public Vertex(int id, Coordinate coordinate, boolean border) {
		this.id = id;
		this.border = border;
		this.setCoordinate(coordinate);
	}

	/**
	 * Returns the ID of the vertex.
	 * 
	 * @return
	 * 		the ID of the vertex
	 */	
	public int getId() {
		return this.id;
	}

	/**
	 * Defines the ID of the vertex.
	 * 
	 * @param id
	 * 		the ID of the vertex
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the coordinate of the vertex.
	 * 
	 * @return
	 * 		the coordinate of the vertex
	 */
	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	/**
	 * Defines the coordinate of the vertex.
	 * 
	 * @param c
	 * 		the coordinate of the vertex
	 */
	public void setCoordinate(Coordinate c) {
		this.coordinate = c;
	}

	/**
	 * Returns true if the vertex is a border vertex
	 * of the triangulation framework, false otherwise.
	 * 
	 * @return
	 * 		true if the vertex is a border vertex,
	 * 		false otherwise
	 */
	public boolean isBorder() {
		return this.border;
	}

	/**
	 * Defines the indicator to know if the edge
	 * is a border edge of the triangulation framework.
	 * 
	 * @param border
	 * 		true if the edge is a border edge,
	 * 		false otherwise
	 */
	public void setBorder(boolean border) {
		this.border = border;
	}

}