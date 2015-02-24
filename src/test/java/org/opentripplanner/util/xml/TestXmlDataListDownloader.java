/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
