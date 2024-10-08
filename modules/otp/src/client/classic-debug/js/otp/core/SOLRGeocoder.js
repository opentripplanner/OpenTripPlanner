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


otp.namespace("otp.core");

otp.core.SOLRGeocoder = otp.Class({
    
    url : null,
    addressParam : null,
    
    initialize : function(url, addressParam) {
        this.url = url;
        this.addressParam = addressParam;
    },
    
    geocode : function(address, setResultsCallback) {
        console.log('solr geocode');
        var params = {
            start : 0,
            limit : 10,
            wt : 'json', 
            qt : 'dismax',
            rows: 10
        }; 
        params[this.addressParam] = address;
        
        $.ajax(this.url, {
            data : params,
            
            success: function(data) {
                if(!data.response) data = jQuery.parseJSON(data);

                console.log(data);
                var results = [];
                var resultData = data.response.docs;
                for(var i=0; i<resultData.length; i++) {
                    if(!otp.util.Text.isNumber(resultData[i].lat) || !otp.util.Text.isNumber(resultData[i].lon)) continue;
                    var resultObj = {
                        description : resultData[i].name + (resultData[i].city ? ', ' + resultData[i].city : ''),
                        lat : resultData[i].lat,
                        lng : resultData[i].lon
                    };
                    results.push(resultObj);                    
                }

                setResultsCallback.call(this, results);
            }
        });        
    } 
    
});
