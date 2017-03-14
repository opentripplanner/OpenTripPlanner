package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.services.notes.NoteMatcher;

public final class WayPropertySetHelper {
	public static void createNames(WayPropertySet propset, String spec, String patternKey) {
		String pattern = patternKey;
		CreativeNamer namer = new CreativeNamer(pattern);
		propset.addCreativeNamer(new OSMSpecifier(spec), namer);
	}

	public static void createNotes(WayPropertySet propset, String spec, String patternKey, NoteMatcher matcher) {
		String pattern = patternKey;
		// TODO: notes aren't localized
		NoteProperties properties = new NoteProperties(pattern, matcher);
		propset.addNote(new OSMSpecifier(spec), properties);
	}

	public static void setProperties(WayPropertySet propset, String spec,
			StreetTraversalPermission permission) {
		setProperties(propset, spec, permission, 1.0, 1.0);
	}

	/**
	 * Note that the safeties here will be adjusted such that the safest street
	 * has a safety value of 1, with all others scaled proportionately.
	 */
	public static void setProperties(WayPropertySet propset, String spec,
			StreetTraversalPermission permission, double safety, double safetyBack) {
		setProperties(propset, spec, permission, safety, safetyBack, false);
	}

	public static void setProperties(WayPropertySet propset, String spec,
			StreetTraversalPermission permission, double safety, double safetyBack, boolean mixin) {
		WayProperties properties = new WayProperties();
		properties.setPermission(permission);
		properties.setSafetyFeatures(new P2<Double>(safety, safetyBack));
		propset.addProperties(new OSMSpecifier(spec), properties, mixin);
	}

	public static void setCarSpeed(WayPropertySet propset, String spec, float speed) {
		SpeedPicker picker = new SpeedPicker();
		picker.specifier = new OSMSpecifier(spec);
		picker.speed = speed;
		propset.addSpeedPicker(picker);
	}

}
