package org.opentripplanner.common.geometry;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeoJsonTest {
	private GeometryFactory gf = new GeometryFactory();
	private ObjectWriter writer;
	private ObjectMapper mapper;

	@Before
	public void setupMapper() {
		
		mapper = new ObjectMapper();
		mapper.registerModule(new GeoJsonModule());

		writer = mapper.writer();
	}

	@Test
	public void point() throws Exception {
		Point point = gf.createPoint(new Coordinate(1.2345678, 2.3456789));
		assertRoundTrip(point);
		assertThat(
				toJson(point),
				is("{\"type\":\"Point\",\"coordinates\":[1.2345678,2.3456789]}"));
	}

	private String toJson(Object value) throws JsonGenerationException,
            JsonMappingException, IOException {
		return writer.writeValueAsString(value);
	}

	@Test
	public void multiPoint() throws Exception {
		MultiPoint multiPoint = gf.createMultiPoint(new Point[] { gf
				.createPoint(new Coordinate(1.2345678, 2.3456789)) });
		assertRoundTrip(multiPoint);
		assertThat(
				toJson(multiPoint),
				is("{\"type\":\"MultiPoint\",\"coordinates\":[[1.2345678,2.3456789]]}"));
	}

	@Test
	public void lineString() throws Exception {
		LineString lineString = gf.createLineString(new Coordinate[] {
				new Coordinate(100.0, 0.0), new Coordinate(101.0, 1.0) });
		assertRoundTrip(lineString);
		assertThat(
				toJson(lineString),
				is("{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,1.0]]}"));
	}

	@Test
	public void multiLineString() throws Exception {

		MultiLineString multiLineString = gf
				.createMultiLineString(new LineString[] {
						gf.createLineString(new Coordinate[] {
								new Coordinate(100.0, 0.0),
								new Coordinate(101.0, 1.0) }),
						gf.createLineString(new Coordinate[] {
								new Coordinate(102.0, 2.0),
								new Coordinate(103.0, 3.0) }) });

		assertRoundTrip(multiLineString);
		assertThat(
				toJson(multiLineString),
				is("{\"type\":\"MultiLineString\",\"coordinates\":[[[100.0,0.0],[101.0,1.0]],[[102.0,2.0],[103.0,3.0]]]}"));
	}

	@Test
	public void polygon() throws Exception {
		LinearRing shell = gf.createLinearRing(new Coordinate[] {
				new Coordinate(102.0, 2.0), new Coordinate(103.0, 2.0),
				new Coordinate(103.0, 3.0), new Coordinate(102.0, 3.0),
				new Coordinate(102.0, 2.0) });
		LinearRing[] holes = new LinearRing[0];
		Polygon polygon = gf.createPolygon(shell, holes);

		assertRoundTrip(polygon);
		assertThat(
				toJson(polygon),
				is("{\"type\":\"Polygon\",\"coordinates\":[[[102.0,2.0],[103.0,2.0],[103.0,3.0],[102.0,3.0],[102.0,2.0]]]}"));
	}

	@Test
	public void polygonWithNoHoles() throws Exception {
		LinearRing shell = gf.createLinearRing(new Coordinate[] {
				new Coordinate(102.0, 2.0), new Coordinate(103.0, 2.0),
				new Coordinate(103.0, 3.0), new Coordinate(102.0, 3.0),
				new Coordinate(102.0, 2.0) });
		LinearRing[] holes = new LinearRing[] { gf
				.createLinearRing(new Coordinate[] {
						new Coordinate(100.2, 0.2), new Coordinate(100.8, 0.2),
						new Coordinate(100.8, 0.8), new Coordinate(100.2, 0.8),
						new Coordinate(100.2, 0.2) }) };
		assertThat(
				toJson(gf.createPolygon(shell, holes)),
				is("{\"type\":\"Polygon\",\"coordinates\":[[[102.0,2.0],[103.0,2.0],[103.0,3.0],[102.0,3.0],[102.0,2.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]}"));
	}

	@Test
	public void multiPolygon() throws Exception {
		LinearRing shell = gf.createLinearRing(new Coordinate[] {
				new Coordinate(102.0, 2.0), new Coordinate(103.0, 2.0),
				new Coordinate(103.0, 3.0), new Coordinate(102.0, 3.0),
				new Coordinate(102.0, 2.0) });
		MultiPolygon multiPolygon = gf.createMultiPolygon(new Polygon[] { gf
				.createPolygon(shell, null) });

		assertRoundTrip(multiPolygon);
		assertThat(
				toJson(multiPolygon),
				is("{\"type\":\"MultiPolygon\",\"coordinates\":[[[[102.0,2.0],[103.0,2.0],[103.0,3.0],[102.0,3.0],[102.0,2.0]]]]}"));
	}

	@Test
	public void geometryCollection() throws Exception {
		GeometryCollection collection = gf
				.createGeometryCollection(new Geometry[] { gf
						.createPoint(new Coordinate(1.2345678, 2.3456789)) });
		assertRoundTrip(collection);
		assertThat(
				toJson(collection),
				is("{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"Point\",\"coordinates\":[1.2345678,2.3456789]}]}"));
	}

	private void assertRoundTrip(Geometry geom) throws JsonGenerationException,
			JsonMappingException, IOException {
		String json = writer.writeValueAsString(geom);
		Geometry regeom = mapper.reader(Geometry.class).readValue(json);
		assertThat(regeom, equalTo(geom));
	}

}
