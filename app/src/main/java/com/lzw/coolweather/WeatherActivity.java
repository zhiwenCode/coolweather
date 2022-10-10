package com.lzw.coolweather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lzw.coolweather.gson.Forecast;
import com.lzw.coolweather.gson.Weather;
import com.lzw.coolweather.util.HttpUtil;
import com.lzw.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private static final String TAG = "WeatherActivity";
    public static final String WEATHER = "weather";
    public static final String BING_PIC = "bing_pic";

    ScrollView weatherLayout;
    TextView titleCity;
    TextView titleUpdateTime;
    TextView degreeText;
    TextView weatherInfoText;
    LinearLayout forecastLayout;
    TextView aqiText;
    TextView pm25Text;
    TextView comfortText;
    TextView carWashText;
    TextView sportText;
    ImageView backgroundImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT>=21){
            View view=getWindow().getDecorView();
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        weatherLayout=findViewById(R.id.weather_layout);
        titleCity=findViewById(R.id.title_city);
        titleUpdateTime=findViewById(R.id.title_update_time);
        degreeText=findViewById(R.id.degree_text);
        weatherInfoText=findViewById(R.id.weather_info_text);
        forecastLayout=findViewById(R.id.forecast_layout);
        aqiText=findViewById(R.id.aqi_text);
        pm25Text=findViewById(R.id.pm25_text);
        comfortText=findViewById(R.id.comfort_text);
        carWashText=findViewById(R.id.car_wash_text);
        sportText=findViewById(R.id.sport_text);
        backgroundImage=findViewById(R.id.bing_pic_img);

        SharedPreferences pref= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherStr=pref.getString(WEATHER,"无缓存");//从本地读取
        if("无缓存".equals(weatherStr)){
            //从上一个页面获取天气id
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        else{
            Weather weather= Utility.handleWeatherResponse(weatherStr);
            showWeatherInfo(weather);
        }
        //随机图形
        Glide.with(this).load("https://bing.ioliu.cn/v1").into(backgroundImage);
    }

    private void showWeatherInfo(Weather weather) {
        titleCity.setText(weather.basic.cityName);
        titleUpdateTime.setText(weather.basic.update.updateTime.split(" ")[1]);//"loc": "2022-10-09 07:14"
        degreeText.setText(weather.now.temperature+"°C");
        weatherInfoText.setText(weather.now.more.info);

        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }

        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        comfortText.setText("舒适度："+weather.suggestion.comfort.info);
        carWashText.setText("洗车指数："+weather.suggestion.carWash.info);
        sportText.setText("运动建议："+weather.suggestion.sport.info);

        weatherLayout.setVisibility(View.VISIBLE);
    }

    private void requestWeather(String weatherId) {
        String url="http://guolin.tech/api/weather?cityid="+weatherId+"&key=b0de1893d279430cb54be504b125b284";
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Toast.makeText(WeatherActivity.this,"获取天气信息失败（1）",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String weatherStr=response.body().string();
                Weather weather= Utility.handleWeatherResponse(weatherStr);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString(WEATHER,weatherStr);
                            editor.apply();//保存到本地
                            showWeatherInfo(weather);
                        }
                        else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败（2）",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}