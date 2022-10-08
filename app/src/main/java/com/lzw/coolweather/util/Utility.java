package com.lzw.coolweather.util;

import android.text.TextUtils;

import com.lzw.coolweather.db.City;
import com.lzw.coolweather.db.County;
import com.lzw.coolweather.db.Province;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utility {
    public static boolean handleProvinceResponse(String response){
        try{
            if(TextUtils.isEmpty(response)==false){
                JSONArray provinces=new JSONArray(response);
                for(int i=0;i<provinces.length();i++){
                    JSONObject provinceObject=provinces.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean handleCityResponse(String response,int provinceId){
        try{
            if(TextUtils.isEmpty(response)==false){
                JSONArray cities=new JSONArray(response);
                for(int i=0;i<cities.length();i++){
                    JSONObject cityObject=cities.getJSONObject(i);
                    City city=new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean handleCountyResponse(String response,int cityId){
        try{
            if(TextUtils.isEmpty(response)==false){
                JSONArray array=new JSONArray(response);
                for(int i=0;i<array.length();i++){
                    JSONObject targetObject=array.getJSONObject(i);
                    County county=new County();
                    county.setCountyName(targetObject.getString("name"));
                    county.setCityId(cityId);
                    county.setWeatherId(targetObject.getString("weather_id"));
                    county.save();
                }
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
