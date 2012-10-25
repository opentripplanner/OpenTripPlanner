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

package org.opentripplanner.analyst.request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This cache sits in front of the SampleFactory. It will bin samples by rounded lat/lon 
 * via the SampleRequest class. Because all pixel locations at one zoom level are also
 * present in tiles at the next zoom level, this cache allows tiles to share Sample objects 
 * and spend less time recalculating them when a new zoom level is encountered.
 */
@Component
public class SampleCache implements SampleSource {

    private final Sample emptySample = new Sample(null, 0, null, 0);
    private final Map<SampleRequest, Sample> cache =
            new ConcurrentHashMap<SampleRequest, Sample>();
    
    @Autowired
    private SampleFactory sampleFactory;
    
    // should distinguish between null sample and key not found
    
    @Override
    public Sample getSample(double lon, double lat) {
        SampleRequest sr = new SampleRequest(lon, lat);
        Sample ret = cache.get(sr);
        if (ret == null) {
            ret = sampleFactory.getSample(lon, lat);
            if (ret == null)
                ret = emptySample;
            cache.put(sr, ret);
        }
        return ret;
    }

}
