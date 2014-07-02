package org.opentripplanner.analyst.pointset;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Category extends Structured implements Serializable {

	private static final long serialVersionUID = -1976567868590201059L;
	
	public Map<String, Attribute> attributes = new ConcurrentHashMap<String, Attribute>();
	
	public Category(){
		// blank constructor for deserialization
		super();
	}

	public Category(String id) {
		super(id);
	}

	/** Deep copy constructor. */
	public Category(Category other) {
		super(other);
		for (String key : other.attributes.keySet()) {
			attributes.put(key, new Attribute(other.attributes.get(key)));
		}
	}

	public Category slice(int start, int end) {
		Category ret = new Category(this.id);
		ret.description = this.description;
		ret.label = this.label;
		ret.style = this.style;
		
		for(Entry<String,Attribute> attr : this.attributes.entrySet()){
			ret.attributes.put( attr.getKey(), attr.getValue().slice(start,end) );
		}
		
		return ret;
	}
	
	public Map<String, Attribute> getAttributes(){
		return this.attributes;
	}
}