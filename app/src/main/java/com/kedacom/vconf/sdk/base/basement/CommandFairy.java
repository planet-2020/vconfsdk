package com.kedacom.vconf.sdk.base.basement;

import android.util.Log;

final class CommandFairy implements ICommandProcessor{

    private static final String TAG = CommandFairy.class.getSimpleName();

    private static CommandFairy instance;

    private JsonProcessor jsonProcessor;

    private MagicBook magicBook;

    private MagicStick magicStick;

    private CommandFairy(){
        jsonProcessor = JsonProcessor.instance();
        magicBook = MagicBook.instance();
        magicStick = MagicStick.instance();
    }

    synchronized static CommandFairy instance() {
        if (null == instance) {
            instance = new CommandFairy();
        }

        return instance;
    }

    @Override
    public void set(String setId, Object para){

        if (!magicBook.isSet(setId)){
            Log.e(TAG, "Unknown set "+setId);
            return;
        }

        if (para.getClass() != magicBook.getSetParaClazz(setId)){
            return;
        }

        magicStick.set(setId, jsonProcessor.toJson(para));
    }

    @Override
    public Object get(String getId){

        if (!magicBook.isGet(getId)){ // XXX 用异常机制代替返回值机制
            Log.e(TAG, "Unknown get "+getId);
            return null; //TODO  throw Exception
        }

        StringBuffer buffer = new StringBuffer();
        magicStick.get(getId, buffer);

        return jsonProcessor.fromJson(buffer.toString(), magicBook.getGetResultClazz(getId));
    }

    @Override
    public Object get(String getId, Object para){
        if (!magicBook.isGet(getId)){
            Log.e(TAG, "Unknown get "+getId);
            return null;
        }

        if (para.getClass() != magicBook.getGetParaClazz(getId)){
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        magicStick.get(getId, jsonProcessor.toJson(para), buffer);

        return jsonProcessor.fromJson(buffer.toString(), magicBook.getGetResultClazz(getId));
    }


}
