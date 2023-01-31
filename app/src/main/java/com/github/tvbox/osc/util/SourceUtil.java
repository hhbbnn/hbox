package com.github.tvbox.osc.util;

import android.content.res.AssetManager;
import android.widget.Toast;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.ApiModel;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceUtil {

    private static final String userAgent = "okhttp/3.15";

    private static final String requestAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

    private static List<ApiModel> history = null;
    private static Map<String,String> historyMap = new HashMap<>();

    public static void init() {
        history = getHistoryFromDB();
        if(history.size()>0) {
            putHistory(history);
            return;
        }
    }

    public static ApiModel getCurrentApi(){
        ApiModel api = new ApiModel();
        api.setUrl(Hawk.get(HawkConfig.API_URL, ""));
        api.setName(Hawk.get(HawkConfig.API_NAME, ""));
        return api;
    }

    public static ApiModel setCurrentApi(ApiModel api){
        Hawk.put(HawkConfig.API_NAME, api.getName());
        Hawk.put(HawkConfig.API_URL, api.getUrl());
        return api;
    }

    public static ApiModel setCurrentApi(String apiUrl){
        String apiName = getApiName(apiUrl);
        Hawk.put(HawkConfig.API_NAME, apiName);
        Hawk.put(HawkConfig.API_URL, apiUrl);
        ApiModel api = new ApiModel();
        api.setUrl(apiUrl);
        api.setName(apiName);
        return api;
    }

    public static void clearCurrentApi(){
        Hawk.put(HawkConfig.API_NAME, "");
        Hawk.put(HawkConfig.API_URL, "");
    }

    public static List<ApiModel> getHistory(){
        return history;
    }

    public static List<ApiModel> getHistoryFromDB(){
        return getHistoryFromDB(new ArrayList<ApiModel>());
    }

    public static List<ApiModel> removeHistory(String apiUrl){
        int idx = indexOf(apiUrl);
        history.remove(idx);
        putHistory(history);
        return history;
    }

    public static List<ApiModel> clearHistory(){
        history.clear();
        historyMap.clear();
        return history;
    }

    public static List<String> getHistoryApiUrls(){
        List<String> list = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            ApiModel api = history.get(i);
            list.add(api.getUrl());
        }
        return list;
    }

    public static List<ApiModel> getHistoryFromDB(List<ApiModel> defaultApiList){
        return Hawk.get(HawkConfig.API_HISTORY, defaultApiList);
    }

    public static List<ApiModel> addHistory(ApiModel api){
        if(indexOf(api.getUrl())==-1){
            history.add(0,api);
            historyMap.put(api.getUrl(),api.getName());
            Hawk.put(HawkConfig.API_HISTORY,history);
        }
        return history;
    }

    public static List<ApiModel> putHistory(List<ApiModel> list){
        historyMap = new HashMap<>();
        history = list;
        Hawk.put(HawkConfig.API_HISTORY,history);
        for (ApiModel api:history) {
            historyMap.put(api.getUrl(),api.getName());
        }
        return history;
    }
    
    public static Integer indexOf(String url){
        for (int i = 0; i < history.size(); i++) {
            ApiModel api = history.get(i);
            if(StringUtils.equals(api.getUrl(),url)){
                return i;
            }
        }
        return -1;
    }

    public static String getApiName(String url){
        String name = historyMap.get(url);
        return StringUtils.isBlank(name)?url:name;
    }

    public static void httpGet(String api, Callback<String> callback) {
        OkGo.<String>get(api)
                .headers("User-Agent", userAgent)
                .headers("Accept", requestAccept)
                .cacheMode(CacheMode.NO_CACHE)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            callback.success(response.body());
                        } catch (Throwable th) {
                            callback.error("");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        callback.error("");
                    }

                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        String result = "";
                        if (response.body() == null) {
                            result = "";
                        } else {
                            result = response.body().string();
                        }
                        return result;
                    }
                });
    }
    public static void addSource(String api, Callback<String> callback){
        httpGet(api,new Callback<String>(){
            @Override
            public void success(String data) {
                JsonObject apiDoc =  new Gson().fromJson(data, (Type)JsonObject.class);// 从builder中读取了json中的数据。
                if(apiDoc.has("storeHouse")){
                    loadStoreHouse(data, new Callback<Map<String,String>>() {
                        @Override
                        public void success(Map<String,String> data) {
                            addSource(data.get("data"));
                            callback.success(data.get("msg"));
                        }

                        @Override
                        public void error(String msg) {
                            callback.error(msg);
                        }
                    });
                }else{
                    addSource(data);
                }
                callback.success("导入成功！");
            }
            @Override
            public void error(String msg) {
                callback.error("导入失败");
            }
        });
    }
    public static void addSource(String sourceJson){
        JsonObject apiDoc =  new Gson().fromJson(sourceJson, (Type)JsonObject.class);// 从builder中读取了json中的数据。
        ApiModel[] apiArray =  new Gson().fromJson(apiDoc.get("urls"), ApiModel[].class);// 从builder中读取了json中的数据。
        for (ApiModel api:apiArray) {
            if(indexOf(api.getUrl())==-1){
                history.add(api);
                historyMap.put(api.getUrl(),api.getName());
                Hawk.put(HawkConfig.API_HISTORY,history);
            }
        }
        if(getCurrentApi().getUrl().isEmpty()){
            setCurrentApi(history.get(0));
        }
    }
    public static void replaceAllSource(String api, Callback<String> callback){
        httpGet(api,new Callback<String>(){
            @Override
            public void success(String data) {
                JsonObject apiDoc =  new Gson().fromJson(data, (Type)JsonObject.class);// 从builder中读取了json中的数据。
                if(apiDoc.has("storeHouse")){
                    clearCurrentApi();
                    clearHistory();
                    loadStoreHouse(data, new Callback<Map<String,String>>() {
                        @Override
                        public void success(Map<String,String> data) {
                            addSource(data.get("data"));
                            callback.success(data.get("msg"));
                        }

                        @Override
                        public void error(String msg) {
                            callback.error(msg);
                        }
                    });
                }else{
                    replaceAllSource(data);
                }
                callback.success("导入成功！");
            }
            @Override
            public void error(String msg) {
                callback.error("导入失败");
            }
        });
    }
    public static void replaceAllSource(String sourceJson){
        JsonObject apiDoc =  new Gson().fromJson(sourceJson, (Type)JsonObject.class);// 从builder中读取了json中的数据。
        ApiModel[] apiArray =  new Gson().fromJson(apiDoc.get("urls"), ApiModel[].class);// 从builder中读取了json中的数据。
        history = Arrays.asList(apiArray);
        putHistory(history);
        setCurrentApi(history.get(0));
    }

    public static void resetSource(Callback<String> callback) {
        try {
            AssetManager assetManager = App.getInstance().getAssets(); //获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
            InputStreamReader inputStreamReader = new InputStreamReader(assetManager.open("default_api.json"),"UTF-8"); //使用IO流读取json文件内容
            BufferedReader br = new BufferedReader(inputStreamReader);//使用字符高效流
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine())!=null){
                builder.append(line);
            }
            br.close();
            inputStreamReader.close();
            if(!builder.toString().isEmpty()){
                loadStoreHouse(builder.toString(), new Callback<Map<String,String>>() {
                    @Override
                    public void success(Map<String,String> data) {
                        clearCurrentApi();
                        clearHistory();
                        addSource(data.get("data"));
                        callback.success(data.get("msg"));
                    }

                    @Override
                    public void error(String msg) {
                        callback.error(msg);
                    }
                });
            }

        } catch (IOException e) {
            callback.error("重置失败！");
        }
    }

    public static void loadStoreHouse(String data,Callback<Map<String,String>> callback){
        JsonObject apiDoc =  new Gson().fromJson(data, (Type)JsonObject.class);// 从builder中读取了json中的数据。
        JsonArray storeHouse = apiDoc.getAsJsonArray("storeHouse");
        for (JsonElement s: storeHouse) {
            JsonObject source = (JsonObject) s;
            String sourceUrl = source.get("sourceUrl").getAsString().trim();
            httpGet(sourceUrl, new Callback<String>() {
                @Override
                public void success(String data) {
                    Map<String,String> map = new HashMap<>();
                    map.put("data",data);
                    map.put("msg",source.get("sourceName").getAsString().trim()+":导入成功！");
                    callback.success(map);
                }
                @Override
                public void error(String msg) {
                    callback.error(source.get("sourceName").getAsString().trim()+":导入失败！");
                }
            });
        }
    }

    public interface Callback<T> {
        void success(T data);
        void error(String msg);
    }

}
