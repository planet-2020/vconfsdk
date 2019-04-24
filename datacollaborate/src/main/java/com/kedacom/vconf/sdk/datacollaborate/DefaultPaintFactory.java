package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.PainterInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPaintFactory implements IPaintFactory {

    private Context context;
    private LifecycleOwner lifecycleOwner;

    /**
     * @param lifecycleOwner 绑定的生命周期拥有者，若不为null则生产的产品的生命周期将绑定到该lifecycleOwner——当其销毁时产品亦随之销毁。
     * */
    public DefaultPaintFactory(@NonNull Context context, @Nullable LifecycleOwner lifecycleOwner){
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
    }


    @Override
    public IPainter createPainter(@NonNull PainterInfo painterInfo) {
        return new DefaultPainter(context, painterInfo, lifecycleOwner);
    }

    @Override
    public IPaintBoard createPaintBoard(@NonNull BoardInfo boardInfo) {
        return new DefaultPaintBoard(context, boardInfo);
    }

}
