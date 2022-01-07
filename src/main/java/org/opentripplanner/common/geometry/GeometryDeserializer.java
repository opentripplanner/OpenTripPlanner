package org.opentripplanner.common.geometry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;

public class GeometryDeserializer extends JsonDeserializer<Geometry> {

	private static GeometryFactory gf = new GeometryFactory();

	@Override
	public Geometry deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {

		ObjectCodec oc = jp.getCodec();
		JsonNode root = oc.readTree(jp);
		return parseGeometry(root);
	}

	public static Geometry parseGeometry(JsonNode root) {
		String typeName = root.get("type").asText();
		if (typeName.equals("Point")) {
			return gf.createPoint(parseCoordinate(root.get("coordinates")));
		} else if(typeName.equals("MultiPoint")) {
			return gf.createMultiPoint(parseLineString(root.get("coordinates")));
		} else if(typeName.equals("LineString")) {
			return gf.createLineString(parseLineString(root.get("coordinates")));
		} else if (typeName.equals("MultiLineString")) {
			return gf.createMultiLineString(parseLineStrings(root.get("coordinates")));
		} else if(typeName.equals("Polygon")) {
			JsonNode arrayOfRings = root.get("coordinates");
			return parsePolygonCoordinates(arrayOfRings);
		} else if (typeName.equals("MultiPolygon")) {
			JsonNode arrayOfPolygons = root.get("coordinates");
			return gf.createMultiPolygon(parsePolygons(arrayOfPolygons));
		} else if (typeName.equals("GeometryCollection")) {
			return gf.createGeometryCollection(parseGeometries(root.get("geometries")));
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private static Geometry[] parseGeometries(JsonNode arrayOfGeoms) {
		Geometry[] items = new Geometry[arrayOfGeoms.size()];
		for(int i=0;i!=arrayOfGeoms.size();++i) {
			items[i] = parseGeometry(arrayOfGeoms.get(i));
		}
		return items;
	}

	private static Polygon parsePolygonCoordinates(JsonNode arrayOfRings) {
		return gf.createPolygon(parseExteriorRing(arrayOfRings),
				parseInteriorRings(arrayOfRings));
	}

	private static Polygon[] parsePolygons(JsonNode arrayOfPolygons) {
		Polygon[] polygons = new Polygon[arrayOfPolygons.size()];
		for (int i = 0; i != arrayOfPolygons.size(); ++i) {
			polygons[i] = parsePolygonCoordinates(arrayOfPolygons.get(i));
		}
		return polygons;
	}

	private static LinearRing parseExteriorRing(JsonNode arrayOfRings) {
		return gf.createLinearRing(parseLineString(arrayOfRings.get(0)));
	}

	private static LinearRing[] parseInteriorRings(JsonNode arrayOfRings) {
		LinearRing rings[] = new LinearRing[arrayOfRings.size() - 1];
		for (int i = 1; i < arrayOfRings.size(); ++i) {
			rings[i - 1] = gf.createLinearRing(parseLineString(arrayOfRings
					.get(i)));
		}
		return rings;
	}

	private static Coordinate parseCoordinate(JsonNode array) {
		return new Coordinate(array.get(0).asDouble(), array.get(1).asDouble());
	}

	private static Coordinate[] parseLineString(JsonNode array) {
		Coordinate[] points = new Coordinate[array.size()];
		for (int i = 0; i != array.size(); ++i) {
			points[i] = parseCoordinate(array.get(i));
		}
		return points;
	}

	private static LineString[] parseLineStrings(JsonNode array) {
		LineString[] strings = new LineString[array.size()];
		for (int i = 0; i != array.size(); ++i) {
			strings[i] = gf.createLineString(parseLineString(array.get(i)));
		}
		return strings;
	}

}
