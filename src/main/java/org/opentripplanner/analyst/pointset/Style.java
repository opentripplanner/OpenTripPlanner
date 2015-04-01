package org.opentripplanner.analyst.pointset;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** TODO: clarify, does this contain CSS attribute-value pairs or...? */
public class Style implements Serializable{
	public Map<String, String> attributes = new ConcurrentHashMap<String, String>();
}