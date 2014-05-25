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

otp.modules.calltaker.Call = Backbone.Model.extend({
    
    url : otp.config.datastoreUrl+'/calltaker/call',
    playName : 'call',
    queries : null,   
    
    initialize : function() {
        this.queries = [ ];
    },
    
    defaults: {
        startTime: null,
        endTime: null
    },

});

otp.modules.calltaker.CallList = Backbone.Collection.extend({
    model: otp.modules.calltaker.Call,
    url: otp.config.datastoreUrl+'/calltaker/call',
});


otp.modules.calltaker.Query = Backbone.Model.extend({
    
    url : otp.config.datastoreUrl+'/calltaker/callQuery',
    playName : 'query'
   
});
 
otp.modules.calltaker.QueryList = Backbone.Collection.extend({

    model: otp.modules.calltaker.Query,
    url: otp.config.datastoreUrl+'/calltaker/callQuery',
       
});

