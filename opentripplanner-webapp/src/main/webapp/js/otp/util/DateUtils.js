/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.util");

/**
 * Utility routines for operating on unknown objects
 */
otp.util.DateUtils = {

    DATE_TIME_FORMAT_STRING : "D, j M H:i",
    TIME_FORMAT_STRING : "H:i",

    /** creates a nicely formatted date @ time string */
    getPrettyDate : function(pre, post, date)
    {
        var retVal = "";
        try
        {
            if(date == null)
                date = new Date();
            if(pre == null)
                pre = "";
            if(post == null)
                post = "";
                
            retVal = pre + date.toDateString() + ' @ ' + date.toLocaleTimeString() + post;
        }
        catch(e)
        {
        }

        return retVal;
    },

    /** minutes / seconds */
    getMinutesAndSeconds : function(m, s, mStr, mmStr, sStr)
    {
        var retVal = "";

        if(mStr  == ":"){ mmStr = ":"; sStr="";}
        if(mStr  == "."){ mmStr = "."; sStr="";}
        if(mStr  == null) mStr  = " " + this.locale.time.minute_abbrev + ", ";
        if(mmStr == null) mmStr = " " + this.locale.time.minutes_abbrev;
        if(sStr  == null) sStr  = " " + this.locale.time.seconds_abbrev + " ";

        if(m && m > 0)
            retVal += m + (m == 1 ? mStr : mmStr);

        retVal +=  (s < 10 ? "0" + s : s) + sStr;

        return retVal;
    },

    /** */ 
    getMonthAsInt : function(str, pad)
    {
        var retVal = str;
        var months = this.locale.time.months;

        for(var i = 0; i < months.length; i++)
        {
            if(str == months[i])
            {
                i++;  // advance to real month number
                retVal = "";

                if(pad && i < 10)
                    retVal = "0";

                retVal += i;
                break;
            }
        }

        return retVal;
    },

    /** */ 
    getElapsedTime : function(oldDate, min, sec)
    {
        if(min == null) min = 0;
        if(sec == null) sec = 0;

        var retVal = {};
        try
        {
            var tsecs  = Math.round(((new Date().getTime() - oldDate.getTime()) / 1000) + sec);
            retVal.min = Math.floor(tsecs / 60) + min;
            retVal.sec = tsecs % 60;
        }
        catch(e)
        {
            retVal = {"min":this.locale.time.minute_abbrev, "sec":this.locale.time.second_abbrev};
        }

        return retVal;
    },

    
    /** default to now + 365 days */ 
    addDays : function(days, date)
    {
        if(date == null)
            date = new Date();
        if(days == null)
            days = 365;

        return new Date(date.getTime()+(1000*60*60*24*days));
    },
    
    /** Make a Date object from an ISO 8601 date string (ignoring time zone) */
    isoDateStringToDate : function(str)
    {
        if (!str)
            return null;
        if (str.lastIndexOf("Z") != -1) {
        	str = str.substring(0, str.length - 1);
        }
        var tokens = str.split(/[\-\+T:]/);
        var date = new Date(tokens[0], tokens[1] - 1, tokens[2], tokens[3],
                tokens[4], tokens[5], 0);
        return date;
    },

    /** */
    prettyDateTime : function(date)
    {
        if (typeof date == "string") {
            date = this.isoDateStringToDate(date);
        }
        return date.format(this.DATE_TIME_FORMAT_STRING);
    },

    /** */
    prettyTime : function(date)
    {
        if (typeof date == "string") {
            date = this.isoDateStringToDate(date);
        }
        return date.format(this.TIME_FORMAT_STRING);
    },

    /** arbitrary time string parser / correction */
    parseTime : function(t)
    {
        var time = t.trim();
        time = time.match(/(\d+)(?::(\d\d))?\s*(p?)/);
    
        var h = time[1];
        var m = parseInt(time[2]) || null;
        var am = time[3];
    
        if(h && h.length > 2)
        {
            if(m == null)
                m = h.substring(h.length-2);
            h = h.substring(0, h.length-2);
        }
        h = parseInt(h) || 12;

        if(m == null || m > 59 || m < 0)
            m = "00"

        if(am && am == 'p')
            h += 12;
    
        return  h + ":" + m;
    },

    /** */
    parseTimeTest : function(t)
    {
        var times = ['1:00 pm','1:00 p.m.','100 p','1:00p.m.','1:00p','1 pm','1 p.m.','1 p','1pm','1p.m.','1p','1:pm','13:00','13','944am', '1354','12335','1232p'];
    
        for ( var i = 0; i < times.length; i++ )
        {
          console.log(parseTime(times[i]));
        }
    },

    CLASS_NAME : "otp.util.DateUtils"
};
