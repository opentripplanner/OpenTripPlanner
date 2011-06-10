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


/* This is called when we get results back from an oba get stop api call */
Admin.prototype.gotObaStopResults = function(data) {
  /* fill in the results */

  var stop_data = data.data;

  $('#stopResults').html($.tmpl("stop", stop_data));
  $('#routes').empty();
  $('#routesHeader').show();
  $.each(stop_data.routes,
    function(index, route) {
      $('#routes').append($.tmpl("route", route));
    });

  $('#stopNoteForm').show();
  $('#stopNoteFormAgencyId').val('TriMet'); //should be stop_data.agency.id, but this is not actually the agency id found in the GTFS

  //parse real stop id out of agency-and-id format
  var parts = stop_data.id.split("_", 2);
  var stop_id = parts[1];
  $('#stopNoteFormStopId').val(stop_id);

  //get a list of patches for this stop
  var otp = new Otp(undefined, undefined); //for queries, which don't need username/password
  otp.getPatchesForStop('TriMet', stop_id, this, this.gotPatchesForStop);

};

Admin.prototype.gotPatchesForStop = function (data) {
  var patchtable = $('#patches');
  $('#patches tr:not(.header)').remove();
  var stopNotePatches = data.patches.StopNotePatch;
  $.each(stopNotePatches, function(index, patch) {
    patchtable.append("<tr>"
		      + "<td>" + patch.stop.@id + "</td>"
		      + "<td>" + patch.notes + "</td>"
		      + "<td>" + toDate(patch.startTime).f("yyyy-NNN-d HH:mm") + "</td>"
		      + "<td>" + toDate(patch.endTime).f("yyyy-NNN-d HH:mm") + "</td>"
		      + "<td>" + toTimeString(patch.startTimeOfDay) + "</td>"
		      + "<td>" + toTimeString(patch.endTimeOfDay) + "</td>"
		      + "</tr>"
		     );
  });

  patchtable.show();
};

Admin.prototype.createdNote = function(data) {
  //refresh patch list
  this.otp.getPatchesForStop($('#stopNoteFormAgencyId').val(),
			$('#stopNoteFormStopId').val(),
			this, this.gotPatchesForStop);

  //show message
  $('#message').html("Created note!");
  $('#message').show();
  $('#stopNoteForm')[0].reset();
};

Admin.prototype.initStopSearchForm = function() {
  $('.hidden').hide();

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
    admin.oba.getStopById(agency, id, "Admin.prototype.gotObaStopResults");
    return false;
  });

  var noteForm = $('#stopNoteForm');
  noteForm.submit(function(event) {
    event.preventDefault();
    if (!$("#stopNoteForm").validate().form()) {
      return;
    }
    var username = $('#username').val();
    var password = $('#password').val();

    var otp = new Otp(username, password);
    otp.createStopNotePatch($('#stopNoteFormAgencyId').val(),
			    $('#stopNoteFormStopId').val(),
			    $('#notes').val(),
			    dateTimeToMillis($('#startTime').val()),
			    dateTimeToMillis($('#endTime').val()),
			    timeToSeconds($('#startTimeOfDay').val()),
			    timeToSeconds($('#endTimeOfDay').val()),
			    admin, admin.createdNote);
    return false
  });
  $('.datetimepicker').datetimepicker();
  $('.timepicker').timepicker({});
  this.initValidator();

};

jQuery.validator.addMethod("date", function(value, element) {
  return this.optional(element) || !isNaN(Date.parse(value));
}, "Must be a valid date-time (2011-07-03 15:37)");

jQuery.validator.addMethod("time", function(value, element) {
  return this.optional(element) || /^[0-9]{1,2}:[0-9]{2}$/i.test(value);
}, "Must be a valid time (15:37)");


Admin.prototype.initValidator = function() {
  $("#stopNoteForm").validate({
    rules: {
      username: "required",
      password: "required",
      startTime: {
	required: true,
	date: true
      },
      endTime: {
	required: true,
	date: true
      },
      startTimeOfDay: {
	required: true,
	time: true
      },
      endTimeOfDay: {
	required: true,
	time: true
      },
      notes: {
	required: true,
	minlength: 2
      }
    }
  });
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

function toDate(millisecondsStr) {
  return new Date(parseInt(millisecondsStr));
}

function padWithZeros(number, length) {

var str = '' + number;
while (str.length < length) {
  str = '0' + str;
}

  return str;
}

function toTimeString(secondsStr) {
  var seconds = parseInt(secondsStr);
  var hours = Math.floor(seconds / 3600);
  seconds -= hours * 3600;
  var minutes = Math.floor(hours / 3600);
  return padWithZeros(hours, 2) + ":" + padWithZeros(minutes, 2);
}

function Admin(oba) {
  this.oba = oba;
  this.otp = new Otp(undefined, undefined); //for queries, which don't need username/password
};