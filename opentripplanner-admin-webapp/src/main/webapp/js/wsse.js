//this function Copyright 2009, https://developer.mozilla.org/User:Inimino
//and licensed under the "MIT License""
//http://www.opensource.org/licenses/mit-license.php
function W3DTF(d) {
 function pad(n){
   return n<10 ? '0'+n : n;
 }
 return d.getUTCFullYear()+'-'
    + pad(d.getUTCMonth()+1)+'-'
    + pad(d.getUTCDate())+'T'
    + pad(d.getUTCHours())+':'
    + pad(d.getUTCMinutes())+':'
    + pad(d.getUTCSeconds())+'Z';
}

function generateWSSEHeader(username, password) {
  var nonce = "";
  var tab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  for (var i = 0; i < 20; ++i) {
    nonce += tab.charAt(Math.floor(Math.random() * 64));
  }
  var created = W3DTF(new Date());
  var password_digest = b64_sha1(nonce + created + password);
  var header = 'UsernameToken Username="' + username + '", PasswordDigest="' + password_digest + '", Nonce="' + nonce + '", Created="' + created + '"';
  console.log(header);
  return header;
}