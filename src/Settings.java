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

    private String   proxy_address  = null;
    private int      proxy_port     = 0;
    private String   proxy_type     = null;
    private boolean  proxy_enable   = false;
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

            JSONObject jsonProxy = setting.getJSONObject("proxy");
            proxy_enable = jsonProxy.getBoolean("enable");
            proxy_address = jsonProxy.getString("address");
            proxy_port = jsonProxy.getInt("port");
            proxy_type = jsonProxy.getString("type");

            JSONArray jsonIgnore = setting.getJSONArray("ignore");

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

    public String getProxyAddress() {return proxy_address;}
    public int getProxyPort() {return proxy_port;}
    public String getProxyType() {return proxy_type;}
    public boolean getProxyEnable() {return  proxy_enable;}
    public String getUserHome() {return  user_home;}
    public List<String> getIgnoreList() {return ignoreList;}
}
