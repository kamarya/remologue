package model;
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
    public static final int LOG_INTERN  = 8; // internal
    public static final int LOG_LEVMASK = 0x0007; // mask to extract level
    public static final int LOG_FACMASK = 0x03F8; // mask to extract priority

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
    public static final int LOG_CRON0	= 9; // clock daemon
    public static final int LOG_CRON1	= 15; // clock daemon
    public static final int LOG_LOCAL0	= 16; // local
    public static final int LOG_LOCAL1	= 17; // local
    public static final int LOG_LOCAL2	= 18; // local
    public static final int LOG_LOCAL3	= 19; // local
    public static final int LOG_LOCAL4	= 20; // local
    public static final int LOG_LOCAL5	= 21; // local
    public static final int LOG_LOCAL6	= 22; // local
    public static final int LOG_LOCAL7	= 23; // local
    public static final int LOG_REMO    = 99; // Internal

    private final SimpleStringProperty  time;
    private SimpleStringProperty        server;
    private final SimpleStringProperty  facility;
    private final SimpleStringProperty  level;
    private final SimpleStringProperty  message;

    private final int                   intLevel;
    private final String                raw;

    public SyslogItem(String time, String server, String facility, String level, String message)
    {
        this.raw        = new String();
        this.time       = new SimpleStringProperty(time);
        this.server     = new SimpleStringProperty(server);
        this.facility   = new SimpleStringProperty(facility);
        this.level      = new SimpleStringProperty(level);
        this.message    = new SimpleStringProperty(message);
        this.intLevel   = 0;
    }

    public SyslogItem(String time, String server, int facility, int level, String message)
    {
        this.time       = new SimpleStringProperty(time);
        this.server     = new SimpleStringProperty(server);
        this.facility   = new SimpleStringProperty(getFacilityString(facility));
        this.level      = new SimpleStringProperty(getLevelString(level));
        this.message    = new SimpleStringProperty(message);
        this.intLevel   = level;

        int flag = level & LOG_LEVMASK + (facility << 3) & LOG_FACMASK;
        this.raw = "<" + Integer.toString(flag) + ">" + message;
    }

    public SyslogItem(String server, String line)
    {
        this(line);
        this.server     = new SimpleStringProperty(server);
    }

    public SyslogItem(String line)
    {
        this.raw = line;
        int startBracketPos = line.indexOf('<');
        int endBracketPos   = line.indexOf('>');
        String priority     = line.substring(startBracketPos + 1, endBracketPos);

        int flag        = Integer.parseInt(priority);
        int intFacility = (flag & LOG_FACMASK) >> 3;
        int intLevel    = flag & LOG_LEVMASK;

        this.intLevel   = intLevel;
        this.server     = new SimpleStringProperty();
        this.facility   = new SimpleStringProperty(getFacilityString(intFacility));
        this.level      = new SimpleStringProperty(getLevelString(intLevel));
        this.message    = new SimpleStringProperty(line.substring(endBracketPos + 1));

        this.time = new SimpleStringProperty(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date()));
    }

    public String getRAW()
    {
        return raw;
    }

    public String getTime()
    {
        return time.get();
    }

    public void setTime(String time)
    {
        this.time.set(time);
    }

    public String getServer()
    {
        return server.get();
    }

    public void setServer(String server)
    {
        this.server.set(server);
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

            case LOG_INTERN:
                return "INTERN";

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

            case LOG_AUTH:
                return "AUTH";

            case LOG_NEWS:
                return "NEWS";

            case LOG_REMO:
                return "REMOLOG";

            case LOG_LOCAL4:
                return "LOCAL4";

            case LOG_LOCAL5:
                return "LOCAL5";

            case LOG_LOCAL6:
                return "LOCAL6";

            case LOG_LOCAL7:
                return "LOCAL7";

            default:
                return "UNKNOWN";
        }
    }
}
