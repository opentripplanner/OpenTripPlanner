
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
			    $('#startTime').val(),
			    $('#endTime').val(),
			    $('#startTimeOfDay').val(),
			    $('#endTimeOfDay').val(),
			    admin, admin.createdNote());
    return false;
  });

};

function Admin(oba) {
  this.oba = oba;
};