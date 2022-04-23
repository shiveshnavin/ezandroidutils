package com.semibit.ezandroidutils.services;

import android.content.Context;

import  com.semibit.ezandroidutils.models.GenricUser;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.interfaces.CacheUtil;
import  com.semibit.ezandroidutils.interfaces.NetworkRequestCallback;
import  com.semibit.ezandroidutils.models.KeyValue;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class CacheService implements CacheUtil {
    private static CacheUtil ourInstance ;
    private static Cache<String , KeyValue> cache;
    private FirebaseRemoteConfig remoteConfig;
    private JSONObject cachePersistentJs;
    private GenricUser user;

    private CacheService() {
        remoteConfig=FirebaseRemoteConfig.getInstance();
        user = EzUtils.readUserData();
    }

    public static CacheUtil getInstance() {
        if(CacheService.ourInstance==null){
            if(!FirebaseRemoteConfig.getInstance().getBoolean("caching_enabled")){
                ourInstance=new CacheUtil(){ };
                return ourInstance;
            }
            CacheService.ourInstance=new CacheService();
            CacheService.cache= CacheBuilder.newBuilder()
                    .expireAfterWrite(60, TimeUnit.MINUTES).build();
            try {
                CacheService.ourInstance.buildCache();
            } catch (Exception e) {
                EzUtils.e("Cache","Building Cache Failed");
                e.printStackTrace();
            }
        }
        return CacheService.ourInstance;
    }

    @Override
    public void dumpCache(){
        Context ctx=App.getAppContext();
        if(ctx==null)
            return;
        JSONObject jsonObject=new JSONObject();
        cache.asMap().entrySet().stream().forEach(stringKeyValueEntry -> {
            try {
                jsonObject.put(stringKeyValueEntry.getKey(),EzUtils.js.toJson(stringKeyValueEntry.getValue()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        EzUtils.setKey("net_cache",jsonObject.toString(),ctx);
        EzUtils.e("Cache","Dumping Cache Success : Size = "+cache.size());
    }

    public void buildCache() throws  Exception {
        Context ctx= App.getAppContext();
        if(ctx==null)
            return;
        if(cachePersistentJs ==null){
            String cachePersistantString = EzUtils.getKey("net_cache", ctx);
            if(cachePersistantString !=null){
                cachePersistentJs =new JSONObject(cachePersistantString);
            }
            else {
                EzUtils.e("Cache","Building Cache Failed ! JStr is null");
                return;
            }
        }
        Iterator<String> keys = cachePersistentJs.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            KeyValue kv=EzUtils.js.fromJson(cachePersistentJs.getString(key),KeyValue.class);
            cache.put(key,kv);
        }
        EzUtils.e("Cache","Building Cache Success : Size = "+cache.size());
    }
    private int hash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        return h;
    }

    @Override
    public void invalidateOne(String url){
        cache.asMap().entrySet().stream().forEach(stringKeyValueEntry -> {

            if(stringKeyValueEntry.getKey().contains(url)){
                cache.invalidate(stringKeyValueEntry.getKey());
                EzUtils.e("Cache","Forcefully invalidated "+stringKeyValueEntry.getKey());
            }

        });
    }
    @Override
    public void invalidateAll(){
        cache.invalidateAll();
    }
    @Override
    public void putIntoCache(String url, JSONObject body, String value){

        String key = url;
        try {
            if(body!=null){
                key += "---"+hash(body.toString());//key +"---"+
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(getTimeout(key)==-1){
            return;
        }
        putIntoCache(key,value);
    }
    @Override
    public void putIntoCache(String key, String value){
        if(getTimeout(key)==-1){
            return;
        }
        EzUtils.e("Cache","Saving to cache "+key);
        KeyValue keyValue=new KeyValue(""+System.currentTimeMillis(),value);
        cache.put(key,keyValue);
    }

    @Override
    public Long getTimeout(String key){
        try{
            String jsr=remoteConfig.getString("cache_config");
            JSONObject cacheConf=new JSONObject(jsr);
            Iterator<String > i=cacheConf.keys();
            while (i.hasNext()){
                String k=i.next();
                String keyFromConfig = k;
                if(user != null)
                    keyFromConfig = keyFromConfig.replace("${userId}",user.getId());
                keyFromConfig = keyFromConfig.toLowerCase();
                if(key.toLowerCase().contains(keyFromConfig)){
                        return cacheConf.getLong(k) * 1000;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return -1L;
    }

    KeyValue lastCall=new KeyValue("0","");
    @Override
    public boolean getFromCache(String url, JSONObject body, NetworkRequestCallback cb){
        String key = url;
        try {
            if(body!=null){
                key += "---"+hash(body.toString());//body +"---"+
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{

            Long call = Long.parseLong(lastCall.key);
            if(call+1000L> System.currentTimeMillis() && lastCall.val.equals(key)){
               EzUtils.e("Cache","Skipping frquent call "+key);
               return true;
            }
            lastCall=new KeyValue(""+System.currentTimeMillis(),key);
        }catch (Exception e){
            e.printStackTrace();
        }
       return getFromCache(key,cb);
    }

    @Override
    public boolean getFromCache(String key, NetworkRequestCallback cb){
        KeyValue data= cache.getIfPresent(key);
        if(data!=null){
            EzUtils.e("Cache","Retrieved from Cache "+key);
            Long allowedTimeout =getTimeout(key);
            Long entryTime=Long.parseLong(data.key);
            if( allowedTimeout==-1 || entryTime+allowedTimeout < System.currentTimeMillis()){
                cache.invalidate(key);
                return false;
            }
            try {
                cb.onSuccessString(data.val);
                cb.onSuccess(new JSONObject(data.val));
            } catch (Exception e) {
               EzUtils.e("Cache",""+e.getMessage());
            }
            return true;
        }
        EzUtils.e("Cache","Not found in Cache "+key);
        return false;
    }


    public static void invalidateCachesAndConfig(){
        CacheService.getInstance().invalidateAll();
        EzUtils.setKey("amounts",null,App.getAppContext());
        EzUtils.setKey("pamounts",null,App.getAppContext());

    }

}
