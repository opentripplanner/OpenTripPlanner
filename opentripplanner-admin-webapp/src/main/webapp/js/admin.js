 /* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

Admin.prototype.success = function(data) {
  /* fill in the results */

  var stop_data = data.data;

  $('#stopResults').replaceWith($.tmpl("stop", stop_data));
  $('#routeResults').empty();

  $.each(stop_data.routes,
    function(index, route) {
      $('#routes').append($.tmpl("route", route));
    });

  $('#stopNoteForm').show();
  $('#stopNoteFormAgencyId').val('TriMet'); //should be stop_data.agency.id, but this is not actually the agency id found in the GTFS

  //parse real stop id out of agency-and-id format
  var parts = stop_data.id.split("_", 2);
  stop_id = parts[1];
  $('#stopNoteFormStopId').val(stop_id);



};

Admin.prototype.createdNote = function(data) {
  $('message').empty();
  $('message').append("Created note!");
};

Admin.prototype.initStopSearchForm = function() {

  $.template( "stop", 'Stop Name: <span id="stop-name">${id}</span><br/>' +
	      'Lat: <span id="stop-lat">${lat}</span><br/>' +
	      'Lon: <span id="stop-lon">${lon}</span><br/>' +
	      'Code: <span id="stop-code">${code}</span>');

  $.template( "route", '<div>' +
	      'Route id: <span id="route-id">${id}</span><br/>' +
	      'Route description: <span id="route-description">${description}</span><br/>' +
	      'Route type: <span id="route-type">${type}</span>' +
	      '</div>');


  var admin = this;
  var searchForm = $('#stopSearchForm');
  searchForm.submit(function(event) {
    event.preventDefault();
    var id = $('#idInput').val();
    var agency = $('#agencyInput').val();
    admin.oba.getStopById(agency, id, "Admin.prototype.success");
    return false;
  });

  var noteForm = $('#stopNoteForm');
  noteForm.hide();
  noteForm.submit(function(event) {
    event.preventDefault();
    var username = $('#username').val();
    var password = $('#password').val();

    var otp = new Otp(username, password);
    otp.createStopNotePatch($('#stopNoteFormAgencyId').val(),
			    $('#stopNoteFormStopId').val(),
			    $('#note').val(),
			    dateTimeToMillis($('#startTime').val()),
			    dateTimeToMillis($('#endTime').val()),
			    timeToSeconds($('#startTimeOfDay').val()),
			    timeToSeconds($('#endTimeOfDay').val()),
			    admin, admin.createdNote());
    return false;
  });

  $('.datetimepicker').datetimepicker();
  $('.timepicker').timepicker({});
};

function dateTimeToMillis(datetime) {
  return Date.parse(datetime);
}

function timeToSeconds(time) {
  var parts = time.split(":");
  var hours = parts[0];
  var minutes = parts[1];
  return hours * 3600 + minutes * 60;
}

function Admin(oba) {
  this.oba = oba;
};