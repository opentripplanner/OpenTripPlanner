package org.opentripplanner.graph_builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphBuilderUtils {

	/**
	 * An extremely common pattern: add an list in a hash value, creating that
	 * list if necessary
	 */
	public static <T, U> void addToMapList(Map<T, List<U>> mapList,
			T key, U value) {
		List<U> list = mapList.get(key);
		if (list == null) {
			list = new ArrayList<U>();
			mapList.put(key, list);
		}
		list.add(value);
	}
}
