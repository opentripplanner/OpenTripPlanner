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
        xmlDataListDownloader
                .setPath("//*[local-name()='kml']/*[local-name()='Document']/*[local-name()='Placemark']");
        xmlDataListDownloader.setDataFactory(new XmlDataFactory<DataTest>() {
            @Override
            public DataTest build(Map<String, String> attributes) {
                DataTest t = new DataTest();
                t.name = attributes.get("name");
                String[] coords = attributes.get("Point").trim().split(",");
                t.lon = Double.parseDouble(coords[0]);
                t.lat = Double.parseDouble(coords[1]);
                return t;
            }
        });
        List<DataTest> data = xmlDataListDownloader
                .download("file:src/test/resources/bike/NSFietsenstallingen.kml");
        assertEquals(5, data.size());
        for (DataTest dt : data) {
            System.out.println(String.format("%s (%.6f,%.6f)", dt.name, dt.lat, dt.lon));
        }
        DataTest alkmaar = data.get(0);
        DataTest zwolle = data.get(4);
        assertEquals("Station Alkmaar", alkmaar.name);
        assertEquals("Station Zwolle", zwolle.name);
        assertTrue(alkmaar.lat >= 52.637531 && alkmaar.lat <= 52.637532);
        assertTrue(alkmaar.lon >= 4.739850 && alkmaar.lon <= 4.739851);
        assertTrue(zwolle.lat >= 52.504990 && zwolle.lat <= 52.504991);
        assertTrue(zwolle.lon >= 6.091060 && zwolle.lon <= 6.091061);
    }
}
