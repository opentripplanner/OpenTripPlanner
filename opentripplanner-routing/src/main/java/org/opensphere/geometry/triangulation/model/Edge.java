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

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.LineSegment;

/**
 * Edge.
 * 
 * @author Eric Grosso
 *
 */
public class Edge {

	/** ID of the edge */
	private int id;
	
	/** Geometry of the edge */
	private LineSegment geometry;

	/** Indicator to know if the edge is a border edge
	 *  of the triangulation framework */
	private boolean border;

	/** Origin vertex of the edge */
	private Vertex oV;
	
	/** End vertex of the edge */
	private Vertex eV;

	/** Triangles in relationship with this edge */
	private List<Triangle> triangles = new ArrayList<Triangle>();
	
	/** Edges in relationship with this edge */
	private List<Edge> incidentEdges = new ArrayList<Edge>();

	
	/**
	 * Default constructor.
	 */
	public Edge() {
		//
	}
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 */
	public Edge(int id) {
		this.id = id;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param geometry
	 * 		geometry of the edge (segment)
	 */
	public Edge(int id, LineSegment geometry) {
		this.id = id;
		this.geometry = geometry;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param border
	 * 		defines if the edge is a border edge
	 * 		or not in the triangulation framework
	 */
	public Edge(int id, boolean border) {
		this.id = id;
		this.border = border;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param geometry
	 * 		geometry of the edge (segment)
 	 * @param border
	 * 		defines if the edge is a border edge
	 * 		or not in the triangulation framework
	 */
	public Edge(int id, LineSegment geometry, boolean border) {
		this.id = id;
		this.geometry = geometry;
		this.border = border;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param geometry
	 * 		geometry of the edge (segment)
	 * @param oV
	 * 		origin vertex
	 * @param eV
	 * 		end vertex
	 * @param border
	 * 		defines if the edge is a border edge
	 * 		or not in the triangulation framework
	 */
	public Edge(int id, LineSegment geometry, Vertex oV, Vertex eV, boolean border) {
		this.id = id;
		this.geometry = geometry;
		this.oV = oV;
		this.eV = eV;
		this.border = border;
	}

	
	/**
	 * Returns the ID of the edge.
	 * 
	 * @return
	 * 		the ID of the edge
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Defines the ID of the edge.
	 * 
	 * @param id
	 * 		ID of the edge
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the geometry of the edge.
	 * 
	 * @return
	 * 		the geometry of the edge
	 */
	public LineSegment getGeometry() {
		return this.geometry;
	}

	/**
	 * Defines the geometry of the edge.
	 * 
	 * @param geometry
	 * 		geometry of the edge (segment)
	 */
	public void setGeometry(LineSegment geometry) {
		this.geometry = geometry;
	}

	/**
	 * Returns true if the edge is a border edge
	 * of the triangulation framework, false otherwise.
	 * 
	 * @return
	 * 		true if the edge is a border edge,
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
	
	/**
	 * Returns the origin vertex of the edge.
	 * 
	 * @return
	 * 		the origin vertex of the edge
	 */	
	public Vertex getOV() {
		return this.oV;
	}

	/**
	 * Defines the origin vertex of the edge.
	 * 
	 * @param oV
	 * 		origin vertex of the edge
	 */
	public void setOV(Vertex oV) {
		this.oV = oV;
	}

	/**
	 * Returns the end vertex of the edge.
	 * 
	 * @return
	 * 		the end vertex of the edge
	 */	
	public Vertex getEV() {
		return this.eV;
	}

	/**
	 * Defines the end vertex of the edge.
	 * 
	 * @param eV
	 * 		end vertex of the edge
	 */
	public void setEV(Vertex eV) {
		this.eV = eV;
	}
	
	/**
	 * Returns the triangles in relationship with the edge.
	 * 
	 * @return
	 * 		the triangles in relationship with the edge
	 */
	public List<Triangle> getTriangles() {
		return this.triangles;
	}

	/**
	 * Defines the triangles in relationship with the edge.
	 * 
	 * @param triangles
	 * 		the triangles in relationship with the edge
	 */
	public void setTriangles(List<Triangle> triangles) {
		this.triangles = triangles;
	}

	/**
	 * Returns the edges in relationship with the edge.
	 * 
	 * @return
	 * 		the edges in relationship with the edge
	 */
	public List<Edge> getIncidentEdges() {
		return this.incidentEdges;
	}

	/**
	 * Defines the edges in relationship with the edge.
	 * 
	 * @param edges
	 * 		the edges in relationship with the edge
	 */
	public void setIncidentEdges(List<Edge> edges) {
		this.incidentEdges = edges;
	}

	/**
	 * Add a triangle in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addTriangle(Triangle triangle) {
		return getTriangles().add(triangle);
	}

	/**
	 * Add triangles in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addTriangles(List<Triangle> triangles) {
		return getTriangles().addAll(triangles);
	}

	/**
	 * Remove a triangle in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeTriangle(Triangle triangle) {
		return getTriangles().remove(triangle);
	}

	/**
	 * Remove triangles in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeTriangles(List<Triangle> triangles) {
		return getTriangles().removeAll(triangles);
	}

	/**
	 * Add an incident edge in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addIncidentEdge(Edge edge) {
		return getIncidentEdges().add(edge);
	}

	/**
	 * Add incident edges in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addIncidentEdges(List<Edge> edges) {
		return getIncidentEdges().addAll(edges);
	}

	/**
	 * Remove an incident edge in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeIncidentEdge(Edge edge) {
		return getIncidentEdges().remove(edge);
	}

	/**
	 * Remove incident edges in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeAllIncidentEdges(List<Edge> edges) {
		return getIncidentEdges().removeAll(edges);
	}

}