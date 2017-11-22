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

otp.namespace("otp.modules.alerts");

//var GtfsRt = GtfsRt || {};

(function(G, endpoint, $) {

  G.Alert = Backbone.Model.extend({
    urlRoot: '/api/alert/',
    
    defaults: {
      id: null,
      timeRanges: [],
      informedEntities: [],
      cause: null,
      effect: null,
      url: null,
      descriptionText: null
    }
  });

  G.Alerts = Backbone.Collection.extend({
    type: 'Alerts',
    model: G.Alert,
    url: '/api/alert/'
  });

  G.TimeRange = Backbone.Model.extend({
    urlRoot: '/api/tr/',

    defaults: {
      id: null,
      startTime: null,
      endTime: null,
    }
  });
  
  G.TimeRanges = Backbone.Collection.extend({
    type: 'TimeRanges',
    model: G.TimeRange,
    url: '/api/tr/'
  });

  G.InformedEntity = Backbone.Model.extend({
    urlRoot: '/api/ie/',

    defaults: {
      id: null,
      agencyId: null,
      routeId: null,
      stopId: null
    }
  });
  
  G.InformedEntities = Backbone.Collection.extend({
    type: 'InformedEntities',
    model: G.InformedEntity,
    url: '/api/ie/'
  });

 
})(otp.modules.alerts, '', jQuery);

