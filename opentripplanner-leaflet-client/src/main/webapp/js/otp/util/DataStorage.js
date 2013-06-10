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

otp.namespace("otp.util");

/**
 * Utility routines remote storing routes
 */

otp.util.DataStorage = {
	
    store : function(data, module) {
    
		var this_ = this;
	
		$.ajax(otp.config.dataStorageUrl , {dataType: 'jsonp', data: {data: JSON.stringify(data)},
		
			success: function(data) { 
				module.newTrip(data.id);
			}
		
		});
	
    },
    
    retrieve : function(id, module) {
        
		var this_ = this;
	
		$.ajax(otp.config.dataStorageUrl , {dataType: 'jsonp', data: {id: id},
		
			success: function(data) {
				if(data)
					module.restorePlan(data);
			}
		});
    },

    CLASS_NAME: "otp.util.DataStorage"
};

