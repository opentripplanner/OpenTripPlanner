package org.opentripplanner.routing.algorithm;

import java.util.Iterator;
import java.util.Map;

import jersey.repackaged.com.google.common.collect.Maps;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

/** A RAPTOR state store that preserves the paths in their entirety */
public class StatePreservingRaptorStateStore implements RaptorStateStore {
	private Map<Vertex, State> current = Maps.newHashMap();
	private Map<Vertex, State> prev;
	
	@Override
	public boolean put(State s) {
		Vertex v = s.getVertex();
		if (!(v instanceof TransitStop))
			throw new UnsupportedOperationException("Vertex is not a transit stop!");
		
		if (current.containsKey(v) && current.get(v).getElapsedTimeSeconds() <= s.getElapsedTimeSeconds())
			return false;
		else {
			current.put(s.getVertex(), s);
			return true;
		}
	}

	@Override
	public State getCurrent(TransitStop t) {
		return current.get(t);
	}

	@Override
	public State getPrev(TransitStop t) {
		return prev.get(t);
	}

	@Override
	public void proceed() {
		prev = Maps.newHashMapWithExpectedSize(current.size());
		prev.putAll(current);
	}

	@Override
	public Iterator<State> currentIterator() {
		return current.values().iterator();
	}

	@Override
	public Iterator<State> prevIterator() {
		return prev.values().iterator();
	}

}
