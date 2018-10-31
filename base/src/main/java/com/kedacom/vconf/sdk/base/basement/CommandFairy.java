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

        if (!magicBook.isSet(setId)){
            Log.e(TAG, "Unknown processSet "+setId);
            return;
        }

        if (para.getClass() != magicBook.getSetParaClazz(setId)){
            return;
        }

        stick.set(setId, jsonProcessor.toJson(para));
    }

    @Override
    public Object processGet(String getId){

        if (null == stick){
            Log.e(TAG, "no command stick ");
            return null;
        }

        if (!magicBook.isGet(getId)){ // XXX 用异常机制代替返回值机制
            Log.e(TAG, "Unknown processGet "+getId);
            return null; //TODO  throw Exception
        }

        StringBuffer buffer = new StringBuffer();
        stick.get(getId, buffer);

        return jsonProcessor.fromJson(buffer.toString(), magicBook.getGetResultClazz(getId));
    }

    @Override
    public Object processGet(String getId, Object para){

        if (null == stick){
            Log.e(TAG, "no command stick ");
            return null;
        }

        if (!magicBook.isGet(getId)){
            Log.e(TAG, "Unknown processGet "+getId);
            return null;
        }

        if (para.getClass() != magicBook.getGetParaClazz(getId)){
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        stick.get(getId, jsonProcessor.toJson(para), buffer);

        return jsonProcessor.fromJson(buffer.toString(), magicBook.getGetResultClazz(getId));
    }

    @Override
    public void setCommandStick(IStick.ICommandStick commandStick) {
        stick = commandStick;
    }

}
