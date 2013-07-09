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
	    otp.widgets.Widget.prototype.initialize.apply(this, arguments);
	     
	    //$(this.div).draggable();
	    
	    this._div = this.div;
	    this.hide();
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
			this.start_button.html("<strong>Recommended Pick Up:</strong><br /> " + start.name + "<br /><strong>Bikes:</strong> " + start.bikesAvailable);
			this.end_button.html("<strong>Recommended Drop Off:</strong><br /> " + end.name + "<br /><strong>Spaces:</strong> " + end.spacesAvailable);

		} else {
			this.start_button = $("<div id='pickup_btn'><strong>Recommended Pick Up:</strong><br /> " + start.name + "<br /><strong>Bikes:</strong> " + start.bikesAvailable + "</div>");
			this.end_button = $("<div id='dropoff_btn'><strong>Recommended Drop Off:</strong><br /> " + end.name + "<br /><strong>Spaces:</strong> " + end.spacesAvailable + "</div>");
			
			($(this.div).append($("<div class='button left'></div>").append(this.start_button))).append($("<div class='button right'></div>").append(this.end_button));  
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
