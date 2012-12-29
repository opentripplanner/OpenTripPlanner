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

otp.widgets.InfoWidget = 
	otp.Class(otp.widgets.Widget, {
	
	_div: null,
	 
	initialize : function(id, config) {
	    otp.configure(this, id);
	    otp.widgets.Widget.prototype.initialize.apply(this, arguments);
	     
	    this.addCloseButton();	
	    //$(this.div).draggable();
	    
	    this._div = this.div;
	},
	
	addCloseButton: function() {
		var _this = this;
		var close_div = $("<div class='close'><div>").html("&times;")
			.click(function(e) {
				e.preventDefault();
				_this.hide();
			});				
		this.div = $(this.div).append(close_div);
	},
	 
	setContent : function(content) {
	    $(this._div).append("<div class='content'>" + content + "</div>");
	},

	show: function() {
		$(this._div).show();
	},
	 
	hide: function() {
		$(this._div).hide();
	},

	CLASS_NAME : "otp.widgets.InfoWidget"
	 
});
