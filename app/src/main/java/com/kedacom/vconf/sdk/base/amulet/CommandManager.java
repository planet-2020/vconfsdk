package com.kedacom.vconf.sdk.base.amulet;


import android.util.Log;

/**
 *
 * Created by Sissi on 2/22/2017.
 */
final class CommandManager implements ICommandProcessor{

    private static final String TAG = CommandManager.class.getSimpleName();

    private static CommandManager instance;

    private JsonProcessor jsonProcessor;

    private SpellBook spellBook;

    private MagicStick magicStick;

    private CommandManager(){
        jsonProcessor = JsonProcessor.instance();
        spellBook = SpellBook.instance();
        magicStick = MagicStick.instance();
    }

    synchronized static CommandManager instance() {
        if (null == instance) {
            instance = new CommandManager();
        }

        return instance;
    }

    @Override
    public void set(String setId, Object para){

        if (!spellBook.isSet(setId)){
            Log.e(TAG, "Unknown set "+setId);
            return;
        }

        if (para.getClass() != spellBook.getSetParaClazz(setId)){
            return;
        }

        magicStick.set(setId, jsonProcessor.toJson(para));
    }

    @Override
    public Object get(String getId){

        if (!spellBook.isGet(getId)){ // XXX 用异常机制代替返回值机制
            Log.e(TAG, "Unknown get "+getId);
            return null; //TODO  throw Exception
        }

        StringBuffer buffer = new StringBuffer();
        magicStick.get(getId, buffer);

        return jsonProcessor.fromJson(buffer.toString(), spellBook.getGetResultClazz(getId));
    }

    @Override
    public Object get(String getId, Object para){
        if (!spellBook.isGet(getId)){
            Log.e(TAG, "Unknown get "+getId);
            return null;
        }

        if (para.getClass() != spellBook.getGetParaClazz(getId)){
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        magicStick.get(getId, jsonProcessor.toJson(para), buffer);

        return jsonProcessor.fromJson(buffer.toString(), spellBook.getGetResultClazz(getId));
    }


}
