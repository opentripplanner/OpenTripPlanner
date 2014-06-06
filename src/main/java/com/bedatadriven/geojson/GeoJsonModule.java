package com.bedatadriven.geojson;

/*
 * NOTE unlike most of OTP, this file was created by Be Data Driven and released under the Apache license 2.0.
 * See https://github.com/bedatadriven/jackson-geojson
 * Adaptations to Jackson2, and some internal methods made public by Conveyal LLC.
 */

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vividsolutions.jts.geom.Geometry;

public class GeoJsonModule extends SimpleModule {

	public GeoJsonModule() {
		super("GeoJson", new Version(1, 0, 0, null,"com.bedatadriven","jackson-geojson"));

		addSerializer(Geometry.class, new GeometrySerializer());
		addDeserializer(Geometry.class, new GeometryDeserializer());
	}
}
