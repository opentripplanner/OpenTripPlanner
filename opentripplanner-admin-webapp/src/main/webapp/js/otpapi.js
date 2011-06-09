

Otp.prototype.sendPatch = function(patch, context, success) {
  var url = this.baseUrl + "patch";
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
    + '<note>' + note + '</note>'
    + '</StopNotePatch>'
    + '</PatchSet>';

  this.sendPatch(patch, context, success);
};


function Otp(username, password) {
    this.username = username;
    this.password = password;
    this.baseUrl = 'http://localhost:7080/opentripplanner-api-webapp/ws/patch/';

}