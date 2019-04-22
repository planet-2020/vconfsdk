package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.PainterInfo;

import androidx.annotation.NonNull;

public class DefaultPaintFactory implements IPaintFactory {

    Context context;

    /**
     * @param context 工厂上下文。NOTE: 如果该上下文为生命周期拥有者（LifecycleOwner实例），
     *                则工厂创建出来的产品生命周期将自动与其绑定，即当该上下文被销毁时产品亦被销毁。
     * */
    public DefaultPaintFactory(Context context){
        this.context = context;
    }


    @Override
    public IPainter createPainter(@NonNull PainterInfo painterInfo) {
        return new DefaultPainter(context, painterInfo);
    }

    @Override
    public IPaintBoard createPaintBoard(@NonNull BoardInfo boardInfo) {
        return new DefaultPaintBoard(context, boardInfo);
    }

}
