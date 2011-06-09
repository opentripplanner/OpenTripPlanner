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
