package com.lzw.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.lzw.coolweather.db.City;
import com.lzw.coolweather.db.County;
import com.lzw.coolweather.db.Province;
import com.lzw.coolweather.util.HttpUtil;
import com.lzw.coolweather.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    int currentLevel;

    ProgressDialog progressDialog;
    TextView titleText;
    Button backButton;
    ListView listView;

    ArrayAdapter<String> adapter;
    List<String> dataList=new ArrayList<>();
    List<Province> provinceList;
    List<City> cityList;
    List<County> countyList;
    Province selectedProvince;
    City selectedCity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);

        titleText=view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView=view.findViewById(R.id.list_view);

        adapter=new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(i);
                    queryCities();
                }
                else if(currentLevel == LEVEL_CITY){
                    selectedCity=cityList.get(i);
                    queryCounties();
                }
                else if(currentLevel == LEVEL_COUNTY){//跳转到天气页面
                    String weatherId = countyList.get(i).getWeatherId();
                    //instanceof 判断一个对象是否是某个类的实例
                    if(getActivity() instanceof MainActivity){
                        //第一次进入程序选择位置
                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        //关闭父活动
                        getActivity().finish();
                    }
                    else if(getActivity() instanceof WeatherActivity){
                        //用户修改位置
                        WeatherActivity weatherActivity= (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        //显示刷新按钮
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }


                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }
                else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        //本地数据库查找
        provinceList= LitePal.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province p:provinceList){
                dataList.add(p.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }
        else {
            //服务器查找
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    private void queryFromServer(String address, String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseString=response.body().string();
                boolean result=false;
                //读取服务器响应数据并保存至数据库
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseString);
                }
                else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseString,selectedProvince.getProvinceCode());
                }
                else if("county".equals(type)){
                    result=Utility.handleCountyResponse(responseString,selectedCity.getCityCode());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            //读取数据库数据
                            if("province".equals(type)){
                                queryProvinces();
                            }
                            else if("city".equals(type)){
                                queryCities();
                            }
                            else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //本地数据库查找
        countyList= LitePal.where("cityid=?",String.valueOf(selectedCity.getCityCode())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for(County c:countyList){
                dataList.add(c.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }
        else {
            //服务器查找
            String address="http://guolin.tech/api/china/"+selectedProvince.getProvinceCode()+"/"+selectedCity.getCityCode();
            queryFromServer(address,"county");
        }
    }

    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        //本地数据库查找
        cityList= LitePal.where("provinceid=?",String.valueOf(selectedProvince.getProvinceCode())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City c:cityList){
                dataList.add(c.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }
        else {
            //服务器查找
            String address="http://guolin.tech/api/china/"+selectedProvince.getProvinceCode();
            queryFromServer(address,"city");
        }
    }
}
