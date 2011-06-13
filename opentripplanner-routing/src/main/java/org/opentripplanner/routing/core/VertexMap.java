package org.opentripplanner.routing.core;

import java.util.Arrays;

@SuppressWarnings("unchecked")
public class VertexMap<T> {
	
	private T[] map;
	
	public VertexMap(int initialCapacity) {
		map = (T[]) new Object[initialCapacity];
	}

	public VertexMap() {
		this(GenericVertex.maxIndex);
	}
	
	// If GraphVertex is eliminated, then every vertex will be a GenericVertex
	public void set(GenericVertex v, T elem) {
		int index = v.index;
		while (index > map.length)
			map = Arrays.copyOf(map, (int) (map.length * 1.5));
		// T old = map[v.index];
		map[v.index] = elem;
	}
	          
	public T get(GenericVertex v) {
		return map[v.index];
	}
	
}
