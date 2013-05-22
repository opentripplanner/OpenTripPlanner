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
	 
	initialize : function(id, owner, options, content) {
	
	    var defaultOptions = {
	        cssClass : 'otp-defaultInfoWidget',
            closeable : true,
            minimizable : true,
            openInitially : false,
	    };
	    
	    options = (typeof options != 'undefined') ? 
	        _.extend(defaultOptions, options) : defaultOptions;
	        
	    otp.widgets.Widget.prototype.initialize.call(this, id, owner, options);
	     
	    this.setContent(content);
	    this.center(); 
	},
});
