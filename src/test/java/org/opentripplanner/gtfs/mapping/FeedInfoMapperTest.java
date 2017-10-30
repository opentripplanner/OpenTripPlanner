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

package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FeedInfoMapperTest {
    private static final org.onebusaway.gtfs.model.FeedInfo FEED_INFO = new org.onebusaway.gtfs.model.FeedInfo();

    private static final Integer ID = 45;

    private static final ServiceDate START_DATE = new ServiceDate(2016, 10, 5);

    private static final ServiceDate END_DATE = new ServiceDate(2017, 12, 7);

    private static final String LANG = "US";

    private static final String PUBLISHER_NAME = "Name";

    private static final String PUBLISHER_URL = "www.url.pub";

    private static final String VERSION = "Version";

    static {
        FEED_INFO.setId(ID);
        FEED_INFO.setStartDate(START_DATE);
        FEED_INFO.setEndDate(END_DATE);
        FEED_INFO.setLang(LANG);
        FEED_INFO.setPublisherName(PUBLISHER_NAME);
        FEED_INFO.setPublisherUrl(PUBLISHER_URL);
        FEED_INFO.setVersion(VERSION);
    }

    private FeedInfoMapper subject = new FeedInfoMapper();

    @Test
    public void testMapCollection() throws Exception {
        assertNull(subject.map((Collection<FeedInfo>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(FEED_INFO)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.FeedInfo result = subject.map(FEED_INFO);

        assertEquals(ID, result.getId());
        assertEquals("20161005", result.getStartDate().getAsString());
        assertEquals("20171207", result.getEndDate().getAsString());
        assertEquals(LANG, result.getLang());
        assertEquals(PUBLISHER_NAME, result.getPublisherName());
        assertEquals(PUBLISHER_URL, result.getPublisherUrl());
        assertEquals(VERSION, result.getVersion());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        org.opentripplanner.model.FeedInfo result = subject.map(new FeedInfo());

        assertNotNull(result.getId());
        assertNull(result.getStartDate());
        assertNull(result.getEndDate());
        assertNull(result.getLang());
        assertNull(result.getPublisherName());
        assertNull(result.getPublisherUrl());
        assertNull(result.getVersion());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.FeedInfo result1 = subject.map(FEED_INFO);
        org.opentripplanner.model.FeedInfo result2 = subject.map(FEED_INFO);

        assertTrue(result1 == result2);
    }

}