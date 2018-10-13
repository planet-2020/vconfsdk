package com.sissi.vconfsdk.base.amulet;


/**
 *
 * Created by Sissi on 2/22/2017.
 */
final class CommandManager implements ICommandProcessor{

//    private static final String TAG = CommandManager.class.getSimpleName();

    private static CommandManager instance;

    private JsonProcessor jsonProcessor;

    private MessageRegister messageRegister;

    private NativeInteractor nativeInteractor;

    private CommandManager(){
        jsonProcessor = JsonProcessor.instance();
        messageRegister = MessageRegister.instance();
        nativeInteractor = NativeInteractor.instance();
    }

    synchronized static CommandManager instance() {
        if (null == instance) {
            instance = new CommandManager();
        }

        return instance;
    }

    @Override
    public void set(String setId, Object para){

        if (!messageRegister.isSet(setId)){
            return;
        }

        if (para.getClass() != messageRegister.getSetParaClazz(setId)){
            return;
        }

        nativeInteractor.set(setId, jsonProcessor.toJson(para));
    }

    @Override
    public Object get(String getId){

        if (!messageRegister.isGet(getId)){ // XXX 用异常机制代替返回值机制
            return null; //TODO  throw Exception
        }

        StringBuffer buffer = new StringBuffer();
        nativeInteractor.get(getId, buffer);

        return jsonProcessor.fromJson(buffer.toString(), messageRegister.getGetResultClazz(getId));
    }

    @Override
    public Object get(String getId, Object para){
        if (!messageRegister.isGet(getId)){
            return null;
        }

        if (para.getClass() != messageRegister.getGetParaClazz(getId)){
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        nativeInteractor.get(getId, jsonProcessor.toJson(para), buffer);

        return jsonProcessor.fromJson(buffer.toString(), messageRegister.getGetResultClazz(getId));
    }


}
