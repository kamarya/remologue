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

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONException;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Settings
{
    private static Settings instance = null;

    private String   ifc            = null;
    private int      port           = 0;
    private String   protocol       = null;
    private String   user_home      = null;
    private List<String> ignoreList = new ArrayList<String>();

    private ErrorStatus err = ErrorStatus.NONE;

    protected Settings()
    {
        reload();
    }

    private String getSettings()
    {
        String ret = null;
        try
        {
            File fsettings = new File(user_home + "/.remologue");

            if (fsettings == null || !fsettings.exists() || fsettings.isDirectory())
            {
                System.out.println("could not find the local settings.");
                fsettings = new File(getClass().getClassLoader().getResource("json/settings.json").getFile());
            }

            FileInputStream fisettings = new FileInputStream(fsettings);
            byte[] data = new byte[(int) fsettings.length()];
            fisettings.read(data);
            fisettings.close();
            ret = new String(data,"utf-8");
        }
        catch (Exception ex)
        {
            System.out.println("could not read neither the local nor the default settings file.");
            err = ErrorStatus.SETTINGS_CRITICAL;
        }

        return ret;
    }

    public void reload()
    {
        err = ErrorStatus.NONE;

        user_home = System.getProperty("user.home");

        String local = getSettings();

        if (local == null) return;

        ignoreList.clear();

        try
        {
            JSONTokener jsonTokener = new JSONTokener(local);
            JSONObject setting = new JSONObject(jsonTokener);

            JSONObject jsonBind = setting.getJSONObject("bind");

            if (jsonBind != null)
            {
                ifc = jsonBind.getString("interface");
                port = jsonBind.getInt("port");
                protocol = jsonBind.getString("protocol").toLowerCase();
            }
            else
            {
                ifc = "0.0.0.0"; // listen on all interfaces
                port = 5514;
                protocol = "udp";
                err = ErrorStatus.SETTINGS_DEFAULT;
            }

            JSONObject jsonMessage = setting.getJSONObject("message");
            if (jsonMessage != null)
            {
                JSONArray jsonIgnore = jsonMessage.getJSONArray("wipeoff");
                if (jsonIgnore != null)
                {
                    String rule;
                    for (int i = 0; i < jsonIgnore.length(); i++)
                    {
                        rule = jsonIgnore.getString(i);

                        if (Settings.validateRegEx(rule))
                        {
                            ignoreList.add(rule);
                        }
                        else
                        {
                            System.out.println("invalid regex : " + rule);
                            err = ErrorStatus.SETTINGS_IGNORED;
                        }
                    }
                }
            }
        }
        catch (JSONException ex)
        {
            System.out.println("JSON parser exception.");
            err = ErrorStatus.SETTINGS_CRITICAL;
        }
    }

    public static Settings getInstance()
    {
        if (instance == null) instance = new Settings();
        return instance;
    }

    public static boolean validateRegEx(String pat)
    {
        try
        {
            Pattern.compile(pat);
        }
        catch (PatternSyntaxException exception)
        {
            return false;
        }

        return true;
    }

    public ErrorStatus getStatus()
    {
        return err;
    }

    public String       getInterface()  {return ifc;}
    public int          getPort()       {return port;}
    public String       getProtocol()   {return protocol;}
    public String       getUserHome()   {return  user_home;}
    public List<String> getIgnoreList() {return ignoreList;}
}
