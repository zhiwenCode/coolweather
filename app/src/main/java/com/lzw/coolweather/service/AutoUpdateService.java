package com.lzw.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.lzw.coolweather.WeatherActivity;
import com.lzw.coolweather.gson.Weather;
import com.lzw.coolweather.util.HttpUtil;
import com.lzw.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //服务启动时调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manager= (AlarmManager) getSystemService(ALARM_SERVICE);
        //毫秒
        long triggerTime= SystemClock.elapsedRealtime()+8*60*60*1000;
        Intent intent1=new Intent(this,AutoUpdateService.class);
        //延迟的意图
        PendingIntent pendingIntent=PendingIntent.getService(this,0,intent1,0);
        manager.cancel(pendingIntent);//取消已经创建的定时任务
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerTime,pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    //无需更新背景图片
    private void updateBingPic() {
    }

    private void updateWeather() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherStr=prefs.getString(WeatherActivity.WEATHER,null);
        //有缓存，请求数据
        if(weatherStr!=null){
            Weather weather= Utility.handleWeatherResponse(weatherStr);
            String weatherId=weather.basic.weatherId;
            String url="http://guolin.tech/api/weather?cityid="+weatherId+"&key=b0de1893d279430cb54be504b125b284";
            HttpUtil.sendOkHttpRequest(url, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String weatherStr=response.body().string();
                    Weather weather= Utility.handleWeatherResponse(weatherStr);
                    if(weather!=null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString(WeatherActivity.WEATHER,weatherStr);
                        editor.apply();//保存到本地
                    }
                }
            });
        }
    }
}