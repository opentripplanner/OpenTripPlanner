package org.opentripplanner.util.xml;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class TestXmlDataListDownloader extends TestCase {

    private static class DataTest {
        private String name;

        private double lat;

        private double lon;
    }

    public void testKML() {
        XmlDataListDownloader<DataTest> xmlDataListDownloader = new XmlDataListDownloader<DataTest>();
        xmlDataListDownloader.setPath("//document/data/element");
        xmlDataListDownloader.setDataFactory(new XmlDataListDownloader.XmlDataFactory<DataTest>() {
            @Override
            public DataTest build(Map<String, String> attributes) {
                DataTest t = new DataTest();
                t.name = attributes.get("name");
                t.lat = Double.parseDouble(attributes.get("lat"));
                t.lon = Double.parseDouble(attributes.get("lon"));
                return t;
            }
        });
        List<DataTest> data = xmlDataListDownloader
                .download("file:src/test/resources/xml/test-data.xml", false);
        assertEquals(3, data.size());
        for (DataTest dt : data) {
            System.out.println(String.format("%s (%.6f,%.6f)", dt.name, dt.lat, dt.lon));
        }
        DataTest a = data.get(0);
        DataTest c = data.get(2);
        assertEquals("A", a.name);
        assertEquals(45.0, a.lat);
        assertEquals(1.0, a.lon);
        assertEquals("C", c.name);
        assertEquals(45.2, c.lat);
        assertEquals(1.1, c.lon);
    }
}
