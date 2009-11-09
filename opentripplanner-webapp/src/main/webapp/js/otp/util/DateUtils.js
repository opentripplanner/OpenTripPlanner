otp.namespace("otp.util");

/**
 * Utility routines for operating on unknown objects
 */
otp.util.DateUtils = {

    /** minutes / seconds */
    getMinutesAndSeconds : function(m, s, mStr, mmStr, sStr)
    {
        var retVal = "";

        if(mStr  == ":"){ mmStr = ":"; sStr="";}
        if(mStr  == "."){ mmStr = "."; sStr="";}
        if(mStr  == null) mStr  = " min, ";
        if(mmStr == null) mmStr = " mins, ";
        if(sStr  == null) sStr  = " secs ";

        if(m && m > 0)
            retVal += m + (m == 1 ? mStr : mmStr);

        retVal +=  (s < 10 ? "0" + s : s) + sStr;

        return retVal;
    },

    /** */ 
    getMonthAsInt : function(str, pad)
    {
        var retVal = str;
        var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

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
            retVal = {"min":min, "sec":sec};
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

    CLASS_NAME : "otp.util.DateUtils"
};
