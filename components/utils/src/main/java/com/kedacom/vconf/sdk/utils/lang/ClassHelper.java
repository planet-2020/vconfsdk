package com.kedacom.vconf.sdk.utils.lang;

import androidx.annotation.NonNull;

public class ClassHelper {
    public static String getReadableName(@NonNull Class<?> cls){
        String name = cls.getName();
        if (name.contains("$")){
            Class<?>[] interfaces = cls.getInterfaces();
            if (interfaces.length>0){
                name += " (implements "+interfaces[0].getName()+")";
            }else{
                name += " (extends "+cls.getSuperclass()+")";
            }
        }
        return name;
    }

    public static String getParents(@NonNull Class<?> cls){
        StringBuilder parents = new StringBuilder();
        Class<?>[] interfaces = cls.getInterfaces();
        Class<?> superclass = cls.getSuperclass();
        if (superclass != Object.class){
            parents.append(" extends ").append(superclass).append("\n");
        }
        for (Class<?> itf : interfaces){
            parents.append(" implements ").append(itf).append("\n");
        }

        return parents.toString();

    }
}
