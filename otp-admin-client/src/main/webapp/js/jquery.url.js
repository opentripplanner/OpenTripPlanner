/*
jQuery Url Plugin
	* Version 1.0
	* 2009-03-22 19:30:05
	* URL: http://ajaxcssblog.com/jquery/url-read-get-variables/
	* Description: jQuery Url Plugin gives the ability to read GET parameters from the actual URL
	* Author: Matthias Jäggli
	* Copyright: Copyright (c) 2009 Matthias Jäggli 
	* Licence: dual, MIT/GPLv2
*/
(function ($) {
	$.url = {};
	$.extend($.url, {
		_params: {},
		init: function(){
			var paramsRaw = "";
			try{
				paramsRaw = 
					(document.location.href.split("?", 2)[1] || "").split("#")[0].split("&") || [];
				for(var i = 0; i< paramsRaw.length; i++){
					var single = paramsRaw[i].split("=");
					if(single[0])
						this._params[single[0]] = unescape(single[1]);
				}
			}
			catch(e){
				alert(e);
			}
		},
		param: function(name){
			return this._params[name] || "";
		},
		paramAll: function(){
			return this._params;
		}
	});
	$.url.init();})(jQuery);
