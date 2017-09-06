/*
 *   Copyright 2017 Behrooz Kamary Aliabadi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import javafx.beans.property.*;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SyslogItem
{

    // Priorities.
    public static final int LOG_EMERG	= 0; // system is unusable
    public static final int LOG_ALERT	= 1; // action must be taken immediately
    public static final int LOG_CRIT	= 2; // critical conditions
    public static final int LOG_ERR	    = 3; // error conditions
    public static final int LOG_WARNING	= 4; // warning conditions
    public static final int LOG_NOTICE	= 5; // normal but significant condition
    public static final int LOG_INFO	= 6; // informational
    public static final int LOG_DEBUG	= 7; // debug-level messages
    public static final int LOG_LEVMASK = 0x0007; // mask to extract priority
    public static final int LOG_FACMASK = 0x03f8; // mask to extract priority

    // Facilities.
    public static final int LOG_KERN	= 0; // kernel messages
    public static final int LOG_USER	= 1; // random user-level messages
    public static final int LOG_MAIL	= 2; // mail system
    public static final int LOG_DAEMON	= 3; // system daemons
    public static final int LOG_AUTH	= 4; // security/authorization
    public static final int LOG_SYSLOG	= 5; // internal syslogd use
    public static final int LOG_LPR	    = 6; // line printer subsystem
    public static final int LOG_NEWS	= 7; // network news subsystem
    public static final int LOG_UUCP	= 8; // UUCP subsystem
    public static final int LOG_CRON	= 15; // clock daemon

    private final SimpleStringProperty time;
    private final SimpleStringProperty facility;
    private final SimpleStringProperty level;
    private final SimpleStringProperty message;

    private final int intLevel;

    SyslogItem(String time, String facility, String level, String message)
    {
        this.time = new SimpleStringProperty(time);
        this.facility = new SimpleStringProperty(facility);
        this.level = new SimpleStringProperty(level);
        this.message = new SimpleStringProperty(message);
        this.intLevel = 0;
    }

    SyslogItem(String line)
    {

        int startBracketPos = line.indexOf('<');
        int endBracketPos   = line.indexOf('>');
        String priority = line.substring(startBracketPos + 1, endBracketPos);

        int flag = Integer.parseInt(priority);
        int intFacility = (flag & LOG_FACMASK) >> 3;
        int intLevel = flag & LOG_LEVMASK;

        this.intLevel = intLevel;

        this.facility = new SimpleStringProperty(getFacilityString(intFacility));
        this.level = new SimpleStringProperty(getLevelString(intLevel));
        this.message = new SimpleStringProperty(line.substring(endBracketPos + 1));

        this.time = new SimpleStringProperty(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date()));
    }

    public String getTime()
    {
        return time.get();
    }

    public void setTime(String time)
    {
        this.time.set(time);
    }

    public String getFacility()
    {
        return facility.get();
    }

    public void setFacility(String facility)
    {
        this.facility.set(facility);
    }

    public String getLevel()
    {
        return level.get();
    }

    public void setLevel(String level)
    {
        this.level.set(level);
    }

    public String getMessage()
    {
        return message.get();
    }

    public void setMessage(String msg)
    {
        message.set(msg);
    }

    public int getLevelInt()
    {
        return intLevel;
    }

    public static String getLevelString(int index)
    {
        switch (index)
        {
            case LOG_ERR:
                return "ERROR";

            case LOG_EMERG:
                return "EMERG";

            case LOG_WARNING:
                return "WARNING";

            case LOG_INFO:
                return "INFO";

            case LOG_NOTICE:
                return "NOTICE";

            case LOG_DEBUG:
                return "DEBUG";

            case LOG_CRIT:
                return "CRIT";

            case LOG_ALERT:
                return "ALERT";

            default:
                return "UNKNOWN";
        }
    }

    public static String getFacilityString(int index)
    {
        switch (index)
        {
            case LOG_KERN:
                return "KERN";

            case LOG_DAEMON:
                return "DAEMON";

            case LOG_USER:
                return "USER";

            case LOG_SYSLOG:
                return "SYSLOG";

            case LOG_MAIL:
                return "MAIL";

            case LOG_NEWS:
                return "NEWS";

            default:
                return "UNKNOWN";
        }
    }
}
