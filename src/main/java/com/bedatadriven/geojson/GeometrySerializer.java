package com.bedatadriven.geojson;

/*
 * NOTE unlike most of OTP, this file was created by Be Data Driven and released under the Apache license 2.0.
 * See https://github.com/bedatadriven/jackson-geojson
 * Adaptations to Jackson2, and some internal methods made public by Conveyal LLC.
 */

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeometrySerializer extends JsonSerializer<Geometry> {

	@Override
	public void serialize(Geometry value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
            JsonProcessingException {

		writeGeometry(jgen, value);
	}

	public void writeGeometry(JsonGenerator jgen, Geometry value)
			throws JsonGenerationException, IOException {
		if (value instanceof Polygon) {
			writePolygon(jgen, (Polygon) value);

		} else if(value instanceof Point) {
			writePoint(jgen, (Point) value);

		} else if (value instanceof MultiPoint) {
			writeMultiPoint(jgen, (MultiPoint) value);

		} else if (value instanceof MultiPolygon) {
			writeMultiPolygon(jgen, (MultiPolygon) value);

		} else if (value instanceof LineString) {
			writeLineString(jgen, (LineString) value);

		} else if (value instanceof MultiLineString) {
			writeMultiLineString(jgen, (MultiLineString) value);
		} else if (value instanceof GeometryCollection) {
			writeGeometryCollection(jgen, (GeometryCollection) value);

		} else {
			throw new UnsupportedOperationException("not implemented: "
					+ value.getClass().getName());
		}
	}

	private void writeGeometryCollection(JsonGenerator jgen,
			GeometryCollection value) throws JsonGenerationException,
			IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "GeometryCollection");
		jgen.writeArrayFieldStart("geometries");

		for (int i = 0; i != value.getNumGeometries(); ++i) {
			writeGeometry(jgen, value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	private void writeMultiPoint(JsonGenerator jgen, MultiPoint value)
			throws JsonGenerationException, IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "MultiPoint");
		jgen.writeArrayFieldStart("coordinates");

		for (int i = 0; i != value.getNumGeometries(); ++i) {
			writePointCoords(jgen, (Point) value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	private void writeMultiLineString(JsonGenerator jgen, MultiLineString value)
			throws JsonGenerationException, IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "MultiLineString");
		jgen.writeArrayFieldStart("coordinates");

		for (int i = 0; i != value.getNumGeometries(); ++i) {
			writeLineStringCoords(jgen, (LineString) value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	@Override
	public Class<Geometry> handledType() {
		return Geometry.class;
	}

	private void writeMultiPolygon(JsonGenerator jgen, MultiPolygon value)
			throws JsonGenerationException, IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "MultiPolygon");
		jgen.writeArrayFieldStart("coordinates");

		for (int i = 0; i != value.getNumGeometries(); ++i) {
			writePolygonCoordinates(jgen, (Polygon) value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	private void writePolygon(JsonGenerator jgen, Polygon value)
			throws JsonGenerationException, IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "Polygon");
		jgen.writeFieldName("coordinates");
		writePolygonCoordinates(jgen, value);

		jgen.writeEndObject();
	}

	private void writePolygonCoordinates(JsonGenerator jgen, Polygon value)
			throws IOException, JsonGenerationException {
		jgen.writeStartArray();
		writeLineStringCoords(jgen, value.getExteriorRing());

		for (int i = 0; i != value.getNumInteriorRing(); ++i) {
			writeLineStringCoords(jgen, value.getInteriorRingN(i));
		}
		jgen.writeEndArray();
	}

	private void writeLineStringCoords(JsonGenerator jgen, LineString ring)
			throws JsonGenerationException, IOException {
		jgen.writeStartArray();
		for (int i = 0; i != ring.getNumPoints(); ++i) {
			Point p = ring.getPointN(i);
			writePointCoords(jgen, p);
		}
		jgen.writeEndArray();
	}

	private void writeLineString(JsonGenerator jgen, LineString lineString)
			throws JsonGenerationException, IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "LineString");
		jgen.writeFieldName("coordinates");
		writeLineStringCoords(jgen, lineString);
		jgen.writeEndObject();
	}

	private void writePoint(JsonGenerator jgen, Point p)
			throws JsonGenerationException, IOException {
		jgen.writeStartObject();
		jgen.writeStringField("type", "Point");
		jgen.writeFieldName("coordinates");
		writePointCoords(jgen, p);
		jgen.writeEndObject();
	}

	private void writePointCoords(JsonGenerator jgen, Point p)
			throws IOException, JsonGenerationException {
		jgen.writeStartArray();
		jgen.writeNumber(p.getX());
		jgen.writeNumber(p.getY());
		jgen.writeEndArray();
	}

}
