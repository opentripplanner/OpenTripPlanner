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

otp.namespace("otp.widgets");

otp.widgets.BikeStationsWidget = 
	otp.Class(otp.widgets.Widget, {
	
	_div: null,
	
	start_button: null,
	
	end_button: null,
		 
	initialize : function(id, module) {
	    otp.configure(this, id);
	    otp.widgets.Widget.prototype.initialize.call(this, id, module, {
	        cssClass : 'otp-bikeshare-stationsWidget',
	        showHeader : false,
	        draggable : false,
	        transparent : true,
	        openInitially : false,
	    });
	     
	    //this.hide();
	},
	
	setContentAndShow: function(startStation, endStation, module) {
	    var start = startStation.toJSON(),
	        end = endStation.toJSON();

		// Fit station names to widget:
		start.name = start.name.length > 50 ? start.name.substring(0,50) + "..." : start.name;
		end.name = end.name.length > 50 ? end.name.substring(0,50) + "..." : end.name;
		
		// Swap existing button name or create new button:
		if (this.start_button !== null) {
			this.start_button.empty();
			this.end_button.empty();
                        //TRANSLATORS: Recommended Pick Up: bike sharing
                        //station name
			this.start_button.html("<strong>" + _tr("Recommended Pick Up:") + "</strong><br /> " + _tr('Bicycle rental') + " " + start.name + "<br />" +
                                                //TRANSLATORS: number of bikes
                                               //availible in a bike sharing
                                               //station
                                               ngettext("<strong>%d</strong> bike available", "<strong>%d</strong> bikes available", start.bikesAvailable));
                        //TRANSLATORS: Recommended Drop Off: bike sharing
                        //station name
			this.end_button.html("<strong>" + _tr("Recommended Drop Off:") + "</strong><br /> " + _tr('Bicycle rental') + " " + end.name + "<br />" +
                                               //TRANSLATORS: number of free
                                               //places to put bikes
                                               //in a bike sharing
                                               //station
                                               ngettext("<strong>%d</strong> dock available", "<strong>%d</strong> docks available", end.spacesAvailable));

		} else {
			this.start_button = $("<div id='pickup_btn'><strong>" + _tr("Recommended Pick Up:") + "</strong><br /> " + _tr('Bicycle rental') + " " + start.name + "<br />" +
                                              ngettext("<strong>%d</strong> bike available", "<strong>%d</strong> bikes available", start.bikesAvailable) + "</div>");
			this.end_button = $("<div id='dropoff_btn'><strong>" + _tr("Recommended Drop Off:") + "</strong><br /> " + _tr('Bicycle rental') + " " + end.name + "<br />" +
                                            ngettext("<strong>%d</strong> dock available", "<strong>%d</strong> docks available", end.spacesAvailable) + "</div>");
			
			this.$().append($("<div class='otp-bikeshare-stationsWidget-left'></div>").append(this.start_button))
        			.append($("<div class='otp-bikeshare-stationsWidget-right'></div>").append(this.end_button));  
		    this.show();
		}
		
        var start_marker = module.getStationMarker(startStation);
        var end_marker = module.getStationMarker(endStation);

        this.start_button.click(function(e) {
        	e.preventDefault();
        	start_marker.openPopup();
        });

        this.end_button.click(function(e) {
        	e.preventDefault();
        	end_marker.openPopup();
        });
        
        
	},

	CLASS_NAME : "otp.widgets.BikeStationsWidget"
	 
});
