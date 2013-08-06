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


function Admin(oba) {
  this.oba = oba;
  this.otp = new Otp(undefined, undefined); //for queries, which don't need username/password
};

/* This is called when we get results back from an oba get stop api call */
Admin.prototype.gotObaStopResults = function(data) {
  /* fill in the results */

  var stop_data = data.data;

  $('#stopResults').html($.tmpl("stop", stop_data));
  $('#stopResults').show();

  $('#routes tr:not(.header)').remove();
  $('#routesHeader').show();

  $.each(stop_data.routes,
    function(index, route) {
      var agencyAndId = parseAgencyAndId(route.id);
      route.id = agencyAndId.id;
      route.agency = agencyAndId.agency;
      $('#routes').append($.tmpl("route", route));
    });
  $('#routes').show();

  $('#noteForm').show();

  //parse real stop id out of agency-and-id format
  var agencyAndId = parseAgencyAndId(stop_data.id);
  $('#noteFormStopId').val(agencyAndId.id);
  $('#noteFormAgencyId').val(agencyAndId.agency);

  //get a list of patches for this stop
  var otp = new Otp(undefined, undefined); //for queries, which don't need username/password
  otp.getPatchesForStop(agencyAndId.agency, agencyAndId.id, this, this.gotPatchesForStop);

};

Admin.prototype.gotRouteData = function(route_data) {

  $('#variants tr:not(.header)').remove();
  $('#variantsHeader').show();

  $.each(route_data.variants,
    function(index, variant) {
      $('#variants').append($.tmpl("variant", variant));
    });
  $('#variants').show();

  $('#noteForm').show();
  $('#noteFormAgencyId').val(route_data.id.@agency);
  $('#noteFormRouteId').val(route_data.id.@id);

  //populate direction select
  options = '<option value="">All</option>';
  $.each(route_data.directions.directions, function(index, direction) {
      options += '<option value="'+ direction +'">' + direction + '</option>';
  });
  $('#direction').html(options);

  //get a list of patches for this route
  this.otp.getPatchesForRoute(route_data.id.@agency, route_data.id.@id, this, this.gotPatchesForRoute);

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

function showNoPatches() {
    var patchtable = $('#patches');
    $('#patches tr:not(.header)').remove();
    patchtable.append('<tr><td colspan="6"><b>no patches</b></td></tr>');
    patchtable.show();
}

//fucking badgerfish is worthless shit
//this function takes a single object or a list, and if a single object
//wraps it in a list
function makeList(objectOrList) {
    if (!(objectOrList instanceof Array)) {
        return [objectOrList];
    }
    return objectOrList;
}

Admin.prototype.gotPatchesForStop = function (data) {
  if (data == null) {
      showNoPatches();
      return;
  }
  var patchtable = $('#patches');
  $('#patches tr:not(.header)').remove();
  var stopNotePatches = makeList(data.patches.StopNotePatch);
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

Admin.prototype.gotPatchesForRoute = function (data) {
  if (data == null) {
      showNoPatches();
      return;
  }
  var patchtable = $('#patches');
  $('#patches tr:not(.header)').remove();
  var patches = makeList(data.patches.RouteNotePatch);
  $.each(patches, function(index, patch) {
    patchtable.append("<tr>"
          + "<td>" + patch.route.@id + "</td>"
          + "<td>" + (patch.direction || 'all') + "</td>"
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

Admin.prototype.createdStopNote = function(data) {
  //refresh patch list
  this.otp.getPatchesForStop($('#noteFormAgencyId').val(),
			$('#noteFormStopId').val(),
			this, this.gotPatchesForStop);

  //show message
  $('#message').html("Created note!");
  $('#message').show();
  $('#noteForm')[0].reset();
};


Admin.prototype.createdRouteNote = function(data) {
  //refresh patch list
  this.otp.getPatchesForRoute($('#noteFormAgencyId').val(),
			$('#noteFormRouteId').val(),
			this, this.gotPatchesForRoute);

  //show message
  $('#message').html("Created note!");
  $('#message').show();
  $('#noteForm')[0].reset();
};


Admin.prototype.init = function() {

  if ($.url.param("agency")) {
      $('#agencyInput').val($.url.param("agency"));
  } else {
    if (Config.defaultAgency != null) {
      $('#agencyInput').val(Config.defaultAgency);
    }
  }
  $('#idInput').val($.url.param("id"));

  $('.hidden').hide();
  $('.datetimepicker').datetimepicker();
  $('.timepicker').timepicker({});
  this.initValidator();

};

Admin.prototype.initStopSearchForm = function() {
  this.init();
  $.template( "stop", 'Stop Name: <span id="stop-name">${id}</span><br/>' +
	      'Lat: <span id="stop-lat">${lat}</span><br/>' +
	      'Lon: <span id="stop-lon">${lon}</span><br/>' +
	      'Code: <span id="stop-code">${code}</span>');

  $.template( "route", '<tr>' +
	      '<td><a href="routes.html?agency=${agency}&id=${id}">${id}</td>' +
	      '<td>${description}</td>' +
	      '<td>${type}</td>' +
	      '</tr>');


  var admin = this;
  var searchForm = $('#stopSearchForm');
  searchForm.submit(function(event) {
    event.preventDefault();
    var id = $('#idInput').val();
    var agency = $('#agencyInput').val();
    admin.oba.getStopById(agency, id, "Admin.prototype.gotObaStopResults");
    return false;
  });

  var noteForm = $('#noteForm');
  noteForm.submit(function(event) {
    event.preventDefault();
    if (!$("#noteForm").validate().form()) {
      return;
    }
    var username = $('#username').val();
    var password = $('#password').val();

    var otp = new Otp(username, password);
    otp.createStopNotePatch($('#noteFormAgencyId').val(),
			    $('#noteFormStopId').val(),
			    $('#notes').val(),
			    dateTimeToMillis($('#startTime').val()),
			    dateTimeToMillis($('#endTime').val()),
			    timeToSeconds($('#startTimeOfDay').val()),
			    timeToSeconds($('#endTimeOfDay').val()),
			    admin, admin.createdStopNote);
    return false;
  });

};

Admin.prototype.initRouteSearchForm = function() {
  this.init();
  $.template( "variant", '<tr>' +
	      '<td>${name}</td>' +
	      '<td>${direction}</td>' +
	      '</tr>');

  var admin = this;
  var searchForm = $('#routeSearchForm');
  searchForm.submit(function(event) {
    event.preventDefault();
    var id = $('#idInput').val();
    var agency = $('#agencyInput').val();
    admin.otp.getRouteData(agency, id, admin, admin.gotRouteData);
    return false;
  });

  var noteForm = $('#noteForm');
  noteForm.submit(function(event) {
    event.preventDefault();
    if (!$("#noteForm").validate().form()) {
      return;
    }
    var username = $('#username').val();
    var password = $('#password').val();

    var otp = new Otp(username, password);
    otp.createRouteNotePatch($('#noteFormAgencyId').val(),
			    $('#noteFormRouteId').val(),
			    $('#direction').val(),
			    $('#notes').val(),
			    dateTimeToMillis($('#startTime').val()),
			    dateTimeToMillis($('#endTime').val()),
			    timeToSeconds($('#startTimeOfDay').val()),
			    timeToSeconds($('#endTimeOfDay').val()),
			    admin, admin.createdRouteNote);
    return false
  });

};


jQuery.validator.addMethod("date", function(value, element) {
  return this.optional(element) || !isNaN(Date.parse(value));
}, "Must be a valid date-time (2011-07-03 15:37)");

jQuery.validator.addMethod("time", function(value, element) {
  return this.optional(element) || /^[0-9]{1,2}:[0-9]{2}$/i.test(value);
}, "Must be a valid time (15:37)");


Admin.prototype.initValidator = function() {
  $(".stopNoteForm").validate({
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
  $(".routeNoteForm").validate({
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

function parseAgencyAndId(agencyAndId) {
  var parts = agencyAndId.split("_", 2);
  return {
    'agency' : parts[0],
    'id' : parts[1]
  };
}
