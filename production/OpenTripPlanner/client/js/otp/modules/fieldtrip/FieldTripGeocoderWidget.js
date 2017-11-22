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

otp.namespace("otp.modules.fieldtrip");

otp.modules.fieldtrip.FieldTripGeocoderWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    activeGeocoder: null,
    
    originResults: null,
    destinationResults: null,
    
    mode : null, // 'inbound' or 'outbound'
    
    initialize : function(id, module, request, mode) {
        var this_ = this;  
        
        this.module = module;
        this.request = request;
        this.mode = mode;    
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-fieldTrip-geocoder',
            title : "Field Trip Location Geocoder for Request #"+request.id,
            resizable : true,
            closeable : true,
        });
        
        //this.contentDiv  = $('<div class="otp-fieldTrip-requestWidget-content notDraggable" />').appendTo(this.mainDiv);
        
        this.content = ich['otp-fieldtrip-geocoder']({
            origin : request.startLocation,
            destination : request.endLocation,
            showGeocoders : (this.module.webapp.geocoders && this.module.webapp.geocoders.length > 1),
            geocoders: this.module.webapp.geocoders,
            
        }).appendTo(this.mainDiv);
        //this.render();
        
        this.content.find(".useLocationsButton").button().click($.proxy(this.useCurrentLocations, this));

        this.content.find(".closeButton").button().click($.proxy(function() {
            this.hide();
        }, this));

        this.content.find(".refreshOriginButton").click($.proxy(this.refreshOriginList, this));

        this.content.find(".refreshDestinationButton").click($.proxy(this.refreshDestinationList, this));
        
        this.activeGeocoder = this.module.webapp.geocoders[0];
        
        if(this.module.webapp.geocoders.length > 1) {
            var selector = this.content.find('.geocoderSelect');
            selector.change(function() {
                this_.activeGeocoder = this_.module.webapp.geocoders[this.selectedIndex];
                this_.refreshOriginList();
                this_.refreshDestinationList();
            });
        }
        
        this.center();
        
        this.refreshOriginList();
        this.refreshDestinationList();
        
    },
    
    refreshOriginList : function() {
        this.activeGeocoder.geocode(this.content.find('.originInput').val(), $.proxy(function(results) {
            this.originResults = results;
            var originSelect = this.content.find('.originSelect');
            originSelect.empty();
            var descriptions = _.pluck(results, 'description');
            for(var i = 0; i < descriptions.length; i++) {
                $('<option>' + descriptions[i] + '</option>').appendTo(originSelect);
            }
        }, this));        
    },
    
    refreshDestinationList : function() {
        this.activeGeocoder.geocode(this.content.find('.destinationInput').val(), $.proxy(function(results) {
            //console.log(results);
            this.destinationResults = results;
            var destSelect = this.content.find('.destinationSelect');
            destSelect.empty();
            var descriptions = _.pluck(results, 'description');
            for(var i = 0; i < descriptions.length; i++) {
                $('<option>' + descriptions[i] + '</option>').appendTo(destSelect);
            }
        }, this));        
    },
    
    useCurrentLocations : function() {
    
        var selectedOriginIndex = this.content.find('.originSelect').find(':selected').index();
        if(selectedOriginIndex > -1) {
            var result = this.originResults[selectedOriginIndex];
            this.module.geocodedOrigins[this.request.id] = result;
            var latlng = new L.LatLng(result.lat, result.lng);
            if(this.mode === "outbound") {
                this.module.setStartPoint(latlng, false, result.description);
            }
            else if(this.mode === "inbound") {
                this.module.setEndPoint(latlng, false, result.description);
            }
        }

        var selectedDestinationIndex = this.content.find('.destinationSelect').find(':selected').index();
        if(selectedDestinationIndex > -1) {
            var result = this.destinationResults[selectedDestinationIndex];
            this.module.geocodedDestinations[this.request.id] = result;
            var latlng = new L.LatLng(result.lat, result.lng);
            if(this.mode === "outbound") {
                this.module.setEndPoint(latlng, false, result.description);
            }
            else if(this.mode === "inbound") {
                this.module.setStartPoint(latlng, false, result.description);
            }
        }

        this.close();        
    },    
});
