package org.opentripplanner.graph_builder.impl.osm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.graph_builder.model.osm.OSMWithTags;

public class TemplateLibrary {
	private static final Matcher matcher = Pattern.compile("\\{(.*?)\\}")
			.matcher("");

	public static String generate(String pattern, OSMWithTags way) {

		if (pattern == null) {
			return null;
		}
		StringBuffer gen_name = new StringBuffer();

		matcher.reset(pattern);
		int lastEnd = 0;
		while (matcher.find()) {
			// add the stuff before the match
			gen_name.append(pattern, lastEnd, matcher.start());
			lastEnd = matcher.end();
			// and then the value for the match
			String key = matcher.group(1);
			String tag = way.getTag(key);
			if (tag != null) {
				gen_name.append(tag);
			}
		}
		gen_name.append(pattern, lastEnd, pattern.length());

		return gen_name.toString();
	}
}
