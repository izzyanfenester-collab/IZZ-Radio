package com.izzyan.izzradio;

import android.content.Context;
import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

final class Station {
    final String id, name, country, primary, fallback, logo, logoPage, localLogo;
    Station(String id, String country, JSONObject item) throws JSONException {
        this.id=id; this.country=country; name=item.getString("name"); primary=item.getString("primary");
        fallback=item.optString("fallback"); logo=item.optString("logo"); logoPage=item.optString("logoPage"); localLogo=item.optString("localLogo");
    }
    static List<Station> load(Context context) {
        try (java.io.InputStream in=context.getAssets().open("stations.json")) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream(); byte[] buffer=new byte[4096]; int n;
            while ((n=in.read(buffer))!=-1) bytes.write(buffer,0,n);
            JSONObject root=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
            List<Station> result=new ArrayList<>();
            for (String key : new String[]{"malaysia","singapore"}) {
                JSONArray items=root.getJSONArray(key);
                for(int i=0;i<items.length();i++) result.add(new Station(key+":"+i,key.equals("malaysia")?"Malaysia":"Singapore",items.getJSONObject(i)));
            }
            return result;
        } catch(Exception e) { throw new IllegalStateException("Cannot read bundled station catalog",e); }
    }
}
