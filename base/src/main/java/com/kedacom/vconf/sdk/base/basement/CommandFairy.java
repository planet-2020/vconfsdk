package com.kedacom.vconf.sdk.base.basement;

import android.util.Log;

final class CommandFairy implements IFairy.ICommandFairy{

    private static final String TAG = CommandFairy.class.getSimpleName();

    private static CommandFairy instance;

    private JsonProcessor jsonProcessor;

    private MagicBook magicBook;

    private IStick.ICommandStick stick;

    private CommandFairy(){
        jsonProcessor = JsonProcessor.instance();
        magicBook = MagicBook.instance();
    }

    synchronized static CommandFairy instance() {
        if (null == instance) {
            instance = new CommandFairy();
        }

        return instance;
    }

    @Override
    public void processSet(String setId, Object para){

        if (null == stick){
            Log.e(TAG, "no command stick ");
            return;
        }

        String setName = magicBook.getMsgName(setId);

        if (!magicBook.isSet(setName)){
            Log.e(TAG, "Unknown processSet "+setName);
            return;
        }

        if (para.getClass() != magicBook.getSetParaClazz(setName)){
            return;
        }

        stick.set(setName, jsonProcessor.toJson(para));
    }

    @Override
    public Object processGet(String getId){

        if (null == stick){
            Log.e(TAG, "no command stick ");
            return null;
        }

        String getName = magicBook.getMsgName(getId);

        if (!magicBook.isGet(getName)){ // XXX 用异常机制代替返回值机制
            Log.e(TAG, "Unknown processGet "+getName);
            return null; //TODO  throw Exception
        }

        StringBuffer buffer = new StringBuffer();
        stick.get(getName, buffer);

        return jsonProcessor.fromJson(buffer.toString(), magicBook.getGetResultClazz(getName));
    }

    @Override
    public Object processGet(String getId, Object para){

        if (null == stick){
            Log.e(TAG, "no command stick ");
            return null;
        }

        String getName = magicBook.getMsgName(getId);

        if (!magicBook.isGet(getName)){
            Log.e(TAG, "Unknown processGet "+getName);
            return null;
        }

        if (para.getClass() != magicBook.getGetParaClazz(getName)){
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        stick.get(getName, jsonProcessor.toJson(para), buffer);

        return jsonProcessor.fromJson(buffer.toString(), magicBook.getGetResultClazz(getName));
    }

    @Override
    public void setCommandStick(IStick.ICommandStick commandStick) {
        stick = commandStick;
    }

}
