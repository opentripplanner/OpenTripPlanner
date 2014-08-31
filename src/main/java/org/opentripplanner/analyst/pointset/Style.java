package org.opentripplanner.analyst.pointset;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Style implements Serializable{
	public Map<String, String> attributes = new ConcurrentHashMap<String, String>();
}