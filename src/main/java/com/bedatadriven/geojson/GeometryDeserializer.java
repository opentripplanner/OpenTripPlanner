package com.bedatadriven.geojson;

/*
 * NOTE unlike most of OTP, this file was created by Be Data Driven and released under the Apache license 2.0.
 * See https://github.com/bedatadriven/jackson-geojson
 * Adaptations to Jackson2, and some internal methods made public by Conveyal LLC.
 */

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.*;

import java.io.IOException;

public class GeometryDeserializer extends JsonDeserializer<Geometry> {

	private GeometryFactory gf = new GeometryFactory();

	@Override
	public Geometry deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {

		ObjectCodec oc = jp.getCodec();
		JsonNode root = oc.readTree(jp);
		return parseGeometry(root);
	}

	public Geometry parseGeometry(JsonNode root) {
		String typeName = root.get("type").asText();
		if (typeName.equals("Point")) {
			return gf.createPoint(parseCoordinate(root
					.get("coordinates")));

		} else if(typeName.equals("MultiPoint")) {
			return gf.createMultiPoint(parseLineString(root.get("coordinates")));
			
		} else if(typeName.equals("LineString")) {
			return gf.createLineString(parseLineString(root.get("coordinates")));

		} else if (typeName.equals("MultiLineString")) {
			return gf.createMultiLineString(parseLineStrings(root
					.get("coordinates")));

		} else if(typeName.equals("Polygon")) {
			JsonNode arrayOfRings = root.get("coordinates");
			return parsePolygonCoordinates(arrayOfRings);

		} else if (typeName.equals("MultiPolygon")) {
			JsonNode arrayOfPolygons = root.get("coordinates");
			return gf.createMultiPolygon(parsePolygons(arrayOfPolygons));

		} else if (typeName.equals("GeometryCollection")) {
			return gf.createGeometryCollection(parseGeometries(root
					.get("geometries")));
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private Geometry[] parseGeometries(JsonNode arrayOfGeoms) {
		Geometry[] items = new Geometry[arrayOfGeoms.size()];
		for(int i=0;i!=arrayOfGeoms.size();++i) {
			items[i] = parseGeometry(arrayOfGeoms.get(i));
		}
		return items;
	}

	private Polygon parsePolygonCoordinates(JsonNode arrayOfRings) {
		return gf.createPolygon(parseExteriorRing(arrayOfRings),
				parseInteriorRings(arrayOfRings));
	}

	private Polygon[] parsePolygons(JsonNode arrayOfPolygons) {
		Polygon[] polygons = new Polygon[arrayOfPolygons.size()];
		for (int i = 0; i != arrayOfPolygons.size(); ++i) {
			polygons[i] = parsePolygonCoordinates(arrayOfPolygons.get(i));
		}
		return polygons;
	}

	private LinearRing parseExteriorRing(JsonNode arrayOfRings) {
		return gf.createLinearRing(parseLineString(arrayOfRings.get(0)));
	}

	private LinearRing[] parseInteriorRings(JsonNode arrayOfRings) {
		LinearRing rings[] = new LinearRing[arrayOfRings.size() - 1];
		for (int i = 1; i < arrayOfRings.size(); ++i) {
			rings[i - 1] = gf.createLinearRing(parseLineString(arrayOfRings
					.get(i)));
		}
		return rings;
	}

	private Coordinate parseCoordinate(JsonNode array) {
		return new Coordinate(array.get(0).asDouble(), array.get(1).asDouble());
	}

	private Coordinate[] parseLineString(JsonNode array) {
		Coordinate[] points = new Coordinate[array.size()];
		for (int i = 0; i != array.size(); ++i) {
			points[i] = parseCoordinate(array.get(i));
		}
		return points;
	}

	private LineString[] parseLineStrings(JsonNode array) {
		LineString[] strings = new LineString[array.size()];
		for (int i = 0; i != array.size(); ++i) {
			strings[i] = gf.createLineString(parseLineString(array.get(i)));
		}
		return strings;
	}

}
