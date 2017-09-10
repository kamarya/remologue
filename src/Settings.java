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

public class Settings
{
    private static Settings instance = null;

    private String   ip_address     = null;
    private int      port           = 0;
    private String   protocol       = null;
    private String   user_home      = null;
    private List<String> ignoreList = new ArrayList<String>();

    protected Settings()
    {
        user_home = System.getProperty("user.home");

        String local = getSettings();

        if (local == null) return;

        try
        {
            JSONTokener jsonTokener = new JSONTokener(local);
            JSONObject setting = new JSONObject(jsonTokener);

            JSONObject jsonBind = setting.getJSONObject("bind");

            if (jsonBind != null)
            {
                ip_address = jsonBind.getString("ip");
                port = jsonBind.getInt("port");
                protocol = jsonBind.getString("protocol");
            }
            else
            {
                ip_address = "0.0.0.0"; // listen on all interfaces
                port = 5514;
                protocol = "udp";
            }

            JSONArray jsonIgnore = setting.getJSONArray("ignore-list");

            if (jsonIgnore != null)
            {
                for (int i = 0; i < jsonIgnore.length(); i++)
                { 
                    ignoreList.add(jsonIgnore.getString(i));
                    System.out.println("ignore : " + jsonIgnore.getString(i));
                }
            }
        }
        catch (JSONException ex)
        {
            System.out.println("JSON parser exception.");
        }

        System.out.println("User Home : " + user_home);
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
        }

        return ret;
    }



    public static Settings getInstance()
    {
        if (instance == null) instance = new Settings();
        return instance;
    }

    public String getIPAddress() {return ip_address;}
    public int getPort() {return port;}
    public String getProtocol() {return protocol;}
    public String getUserHome() {return  user_home;}
    public List<String> getIgnoreList() {return ignoreList;}
}
