package org.opentripplanner.graph_builder.impl.osm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.graph_builder.model.osm.OSMWithTags;

public class CreativeNamer {
	private static final Matcher matcher = Pattern.compile("\\{(.*?)\\}").matcher("");
	
	/** A creative name pattern is a template which may contain variables of the form 
	 * {{tag_name}}.  When a way's creative name is created, the value of its tag tag_name
	 * is substituted for the variable.
	 * 
	 * For example, 
	 * "Highway with surface {{surface}}"
	 * might become
	 * "Highway with surface gravel"
	 */
	private String creativeNamePattern;
	
	public CreativeNamer(String pattern) {
		this.creativeNamePattern = pattern;
	}

	public CreativeNamer() {
	}

	public String generateCreativeName(OSMWithTags way) {
		if (getCreativeNamePattern() == null) {
			return null;
		}
		StringBuffer gen_name = new StringBuffer();

		matcher.reset(getCreativeNamePattern());
		int lastEnd = 0;
		while (matcher.find()) {
			//add the stuff before the match
			gen_name.append(getCreativeNamePattern(), lastEnd, matcher.start());
			lastEnd = matcher.end();
			//and then the value for the match
			String key = matcher.group(1);
			String tag = way.getTag(key);
			if (tag != null) {
				gen_name.append(tag);
			}
		}
		gen_name.append(getCreativeNamePattern(), lastEnd, getCreativeNamePattern().length());
		
        return gen_name.toString();
	}

	public void setCreativeNamePattern(String creativeNamePattern) {
		this.creativeNamePattern = creativeNamePattern;
	}

	public String getCreativeNamePattern() {
		return creativeNamePattern;
	}


}
