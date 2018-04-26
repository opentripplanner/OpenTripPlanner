/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import java.util.ArrayList;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedId;

public class AgencyAndIdArrayListAdapter extends XmlAdapter<ArrayList<AgencyAndIdType>, ArrayList<FeedId>> {

    @Override
    public ArrayList<FeedId> unmarshal(ArrayList<AgencyAndIdType> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<FeedId> result = new ArrayList<FeedId>();
        for (AgencyAndIdType a : arg)
            result.add(new FeedId(a.agency, a.id));
        return result;
    }

    @Override
    public ArrayList<AgencyAndIdType> marshal(ArrayList<FeedId> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<AgencyAndIdType> result = new ArrayList<AgencyAndIdType>();
        for(FeedId a:arg) result.add(new AgencyAndIdType(a.getAgencyId(), a.getId()));
        return result;
    }

}
