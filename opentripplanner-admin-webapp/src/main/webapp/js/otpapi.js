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


Otp.prototype.sendPatch = function(patch, context, success) {
  var url = this.baseUrl + "patch/patch";
  $.ajax({
    url: url,
    headers: {
      'X-WSSE' : generateWSSEHeader(this.username, this.password),
      'Authorization' : 'WSSE profile="UsernameToken"',
      'Content-type' : 'application/xml'
    },
    contentType: "text/xml",
    data: patch,
    type: 'POST',
    dataType: 'json',
    context: context,
    success: success,
    failure: this.failure
  });

};


Otp.prototype.failure = function(data) {
  console.log("failure args are: ");
  console.log(arguments);
};


Otp.prototype.createId = function() {
  var tab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  var id = '';
  for (var i = 0; i < 11; ++i) {
    id += tab.charAt(Math.floor(Math.random() * 62));
  }
  return id;
};

Otp.prototype.getPatchesForStop = function(agency, stop_id, context, success) {
  var url = this.baseUrl + "patch/stopPatches?agency=" + agency + "&id=" + stop_id;
  $.ajax({
    url: url,
    type: 'GET',
    dataType: 'json',
    context: context,
    success: success,
    failure: this.failure
  });

};

Otp.prototype.getPatchesForRoute = function(agency, route_id, context, success) {
  var url = this.baseUrl + "patch/routePatches?agency=" + agency + "&id=" + route_id;
  $.ajax({
    url: url,
    type: 'GET',
    dataType: 'json',
    context: context,
    success: success,
    failure: this.failure
  });

};


Otp.prototype.getRouteData = function(agency, route_id, context, success) {
  var url = this.baseUrl + "transit/routeData?agency=" + agency + "&id=" + route_id;
  $.ajax({
    url: url,
    type: 'GET',
    dataType: 'json',
    context: context,
    success: success,
    failure: this.failure
  });

};


Otp.prototype.createStopNotePatch = function(agency, stop_id, note, startTime, endTime, startTimeOfDay, endTimeOfDay, context, success) {
  var id = this.createId();
  var patch = '<PatchSet>'
    + '<StopNotePatch>'
    + '<id>' + id + '</id>'
    + '<stop agency= "' + agency + '" id = "' + stop_id + '" />'
    + '<startTime>' + startTime + '</startTime>'
    + '<endTime>' + endTime + '</endTime>'
    + '<startTimeOfDay>' + startTimeOfDay + '</startTimeOfDay>'
    + '<endTimeOfDay>' + endTimeOfDay + '</endTimeOfDay>'
    + '<notes>' + note + '</notes>'
    + '</StopNotePatch>'
    + '</PatchSet>';

  this.sendPatch(patch, context, success);
};


function Otp(username, password) {
    this.username = username;
    this.password = password;
    this.baseUrl = Config.otpAPIBaseUrl + '/ws/';

}