package com.semibit.ezandroidutils.utils;


import java.util.HashMap;

public class ObjectTransporter  {

    HashMap<String ,Object> objectHashMap=new HashMap<>();
    private static ObjectTransporter instance;
    public static ObjectTransporter getInstance()
    {
        if(instance==null)
            instance=new ObjectTransporter();
        return instance;
    }
    private ObjectTransporter(){}

    public void put(String tag,Object obj)
    {
        //EzUtils.e("Mapper","put "+obj);
        objectHashMap.put(tag,obj);
    }

    public Object get(String tag)
    {
        if(objectHashMap.containsKey(tag))
        {
            //EzUtils.e("Mapper","get "+objectHashMap.get(tag));
            return objectHashMap.get(tag);
        }
        return null;
    }

    public Object remove(String tag)
    {
        if(objectHashMap.containsKey(tag))
        {
            Object obj=objectHashMap.get(tag);
            objectHashMap.remove(tag);
            //EzUtils.e("Mapper","remove "+obj);

            return obj;
        }
        return null;
    }
}
