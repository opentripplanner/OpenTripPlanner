/* This is how we make queries to one bus away */

Oba.prototype.getStopById = function(agency, id, callback) {

  var url = this.baseUrl + 'stop/' + agency + "_" + id + ".json?key=TEST&callback=" + callback;
  var encoded = encodeURIComponent(url);
  $.ajax({
  url: url,
    type: 'GET',
    dataType: 'jsonp',
    success: Otp.prototype.success,
    failure: this.failure
  });

};


Oba.prototype.success = function(data) {
  console.log("hopefully this will all go via jsonp");
  console.log(arguments);
};


Oba.prototype.failure = function(data) {
  console.log("failure args are: ");
  console.log(arguments);
};

function Oba() {
  this.baseUrl = "http://soak-api.onebusaway.org/api/where/";
}
