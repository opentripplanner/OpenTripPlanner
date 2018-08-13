package org.opentripplanner.routing.core;

import java.util.Arrays;

import org.opentripplanner.routing.graph.Vertex;

@SuppressWarnings("unchecked")
public class VertexMap<T> {
	
	private T[] map;
	
	public VertexMap(int initialCapacity) {
		map = (T[]) new Object[initialCapacity];
	}

	public VertexMap() {
		this(Vertex.getMaxIndex());
	}
	
	public void set(Vertex v, T elem) {
		int index = v.getIndex();
		while (index > map.length)
			map = Arrays.copyOf(map, (int) (map.length * 1.5));
		// T old = map[v.index];
		map[v.getIndex()] = elem;
	}
	          
	public T get(Vertex v) {
		return map[v.getIndex()];
	}
	
}
