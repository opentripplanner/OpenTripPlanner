package org.opentripplanner.jags.gtfs;

import java.util.HashMap;
import java.util.Map;

public class TableHeader {
	Map<String, Integer> columnIndices = new HashMap<String, Integer>();
	
	public TableHeader(String[] columns) {
		for(int i=0; i<columns.length; i++) {
			columnIndices.put(columns[i], new Integer(i));
		}
	}
	
	public int index(String column) {
		Integer indexObj = columnIndices.get(column);
		if(indexObj==null) {
			return -1;
		} else {
			return indexObj.intValue();
		}
	}
}
