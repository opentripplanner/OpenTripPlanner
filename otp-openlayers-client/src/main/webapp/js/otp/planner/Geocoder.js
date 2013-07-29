/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.planner");


/** TODO: this code needs refactoring
 *   1. the geocode code will need to be OBJECTS...no more if _from branching, etc...   ESPECIALLY important with intermediatePlaces ...
 *   2. again, geocode (and a lot of other stuff) feels like it could use a bit of OOP ... reduce the number of from/to branches, etc...
 *   3. documentation is lacking in many parts 
 */

/**
  * otp/planner/Geocoder.js 
  * 
  * Purpose is to handle form data that needs to be geocoded
  */
otp.planner.GeocoderStatic = {

    enabled  : false,
    isSolr   : false,
    form     : null,
    url      : null,
    addressParamName : null,
    geocoder_cfg : {},    // geocoder reference from config.js

    // hold state for whether geocoding is currently active
    m_fromGeocoding : false,
    m_toGeocoding   : false,

    initialize : function(config)
    {
        otp.configure(this, config);
        otp.configure(this, config.geocoder_cfg);
    },

    /** */
    fromChanged : function(comboBox, value) {
        if (otp.util.ObjUtils.isCoordinate(value)) {
            var lat = otp.util.ObjUtils.getLat(value);
            var lng = otp.util.ObjUtils.getLon(value);
            this.form.setFrom(value, lat, lng, true, false);
        } else {
            this._makeGeocoderRequest(value,
                    this.handleGeocoderResponse.createDelegate(this, ['from'], true),
                    this.handleGeocoderFailure.createDelegate(this, ['from'], true),
                    "from"
            );
        }
    },

    /** */
    toChanged : function(comboBox, value) {
        if (otp.util.ObjUtils.isCoordinate(value)) {
            var lat = otp.util.ObjUtils.getLat(value);
            var lng = otp.util.ObjUtils.getLon(value);
            this.form.setTo(value, lat, lng, true, false);
        } else {
            this._makeGeocoderRequest(value,
                    this.handleGeocoderResponse.createDelegate(this, ['to'], true),
                    this.handleGeocoderFailure.createDelegate(this, ['to'], true),
                    "to"
            );
        }
    },

    /** */
    _makeGeocoderRequest : function(address, successFn, failureFn, comboBoxIdentifier) {
        if (!address || address.length < 1) {
            return;
        }
        var loadMask = new Ext.LoadMask(this.form.m_fromToFP.getEl(), {msg: this.form.locale.tripPlanner.geocoder.working});
        loadMask.show();

        if (comboBoxIdentifier === "from") {
            this.m_fromGeocoding = true;
        } else if (comboBoxIdentifier === "to") {
            this.m_toGeocoding = true;
        }

        var params = {};
        params[this.addressParamName] = address;
        if (this.routerId)
            params["routerId"] = this.form.routerId;

        var self = this;
        Ext.Ajax.request({
            url: this.url,
            method: "GET",
            success: successFn,
            failure: failureFn,
            callback: function() {
                loadMask.hide();
                if (comboBoxIdentifier === "from") {
                    self.m_fromGeocoding = false;
                } else if (comboBoxIdentifier === "to") {
                    self.m_toGeocoding = false;
                }
            },
            params: params
        });
    },


    handleGeocoderResponse: function(response, ajaxOptions, comboBoxIdentifier)
    {
        var self = this;
        var xml = response.responseXML;

        // step 1: error process explicit error message, then exit out of here if we encounter an error
        var errorNode = Ext.DomQuery.selectNode("error", xml);
        if (errorNode) {
            console.log("GEOCODE ERROR: " + errorNode.firstChild.nodeValue);
            this.form.setFormErrorMessage(comboBoxIdentifier);
            return;
        }

        // step 2: find the 'count' node (how may geocode hits) in the XML
        var countNode = Ext.DomQuery.selectNode("count", xml);

        // step 3: error process condition where we have an
        if (!countNode) {
            console.log("GEOCODE ERROR: can't seem to parse the retured XML")
            this.form.setFormErrorMessage(comboBoxIdentifier);
            return;
        }

        // step 4: error process condition of ZERO geocodes (indicated with null count)...give an error, then exit
        var count = parseInt(countNode.firstChild.nodeValue);
        if (isNaN(count) || count < 1)
        {
            console.log("GEOCODE ERROR: got ZERO geocodes back in the return");
            this.form.setFormErrorMessage(comboBoxIdentifier);
            return;
        }

        // step 5: process the geocoded location / process ambiguous locations 
        if (count > 1)
        {
            // show an icon on the grid to identify what form is being geocoded
            var icon = null;
            if (comboBoxIdentifier === "from") {
                 icon = 'start-icon';
            } else if (comboBoxIdentifier === "to") {
                 icon = 'end-icon';
            }

            // step 5a: ambiguous geocoder results...ask the user to pick one
            var xmlNodes = Ext.DomQuery.jsSelect("result", xml);
            var resultsSelector = new otp.planner.GeocoderResultsSelector({
                locale  : this.form.locale,
                iconCls : icon, 
                callback: function(lat, lng, description) {
                  // TODO refactor all of this ... these from/to conditionals can go away with a lil OOP
                  if (comboBoxIdentifier === "from") {
                      self.form.setFrom(description, lat, lng, true, false);
                  } else if (comboBoxIdentifier === "to") {
                      self.form.setTo(description, lat, lng, true, false);
                  }
                },
                geocoderResults: this.parseGeocoderResultXml(xmlNodes)
            });
            resultsSelector.displayDialog();
        } else {
            // step 5b: have only 1 result, so parse that result and set appropriately
            var lat = Ext.DomQuery.selectNode("lat", xml).firstChild.nodeValue;
            var lng = Ext.DomQuery.selectNode("lng", xml).firstChild.nodeValue;
            var description = Ext.DomQuery.selectNode("description", xml).firstChild.nodeValue;
            var latlng = lat + "," + lng;

            if (comboBoxIdentifier === "from") {
                self.form.m_fromForm.getComboBox().clearInvalid();
                self.form.setFrom(description, lat, lng, true, false);
            } else if (comboBoxIdentifier === "to") {
                self.form.m_toForm.getComboBox().clearInvalid();
                self.form.setTo(description, lat, lng, true, false);
            }
        }
    },

    /** */
    handleGeocoderFailure: function(response, ajaxOptions, comboBoxIdentifier) {
        console.log("geocoder failure");
        console.log(response);
        console.log("geocoder failure options");
        console.log(ajaxOptions);
        console.log("geocoding for combobox: " + comboBoxIdentifier);
    },

    /**
     * parse xml nodes returned from geocoder into an array of arrays
     * suitable to pass into the geocoder results selector
     */
    parseGeocoderResultXml : function(xmlNodes) {
        var result = [];
        Ext.each(xmlNodes, function(node) {
            var lat = Ext.DomQuery.selectNode("lat", node).firstChild.nodeValue;
            var lng = Ext.DomQuery.selectNode("lng", node).firstChild.nodeValue;
            var description = Ext.DomQuery.selectNode("description", node).firstChild.nodeValue;
            result.push([lat, lng, description]);
        });
        return result;
    },

    CLASS_NAME: "otp.planner.Geocoder"
};

otp.planner.Geocoder = new otp.Class(otp.planner.GeocoderStatic);
