package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.PainterInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

public final class DefaultPaintFactory implements IPaintFactory {

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


    /**
     * 修正屏幕密度值。
     * 为了使不同设备上展示的效果尽量一致，我们会根据屏幕密度对图元的坐标进行相应的缩放处理。
     * 但是有些设备上根据此法变换后出来的效果不符合预期，所以提供该接口供用户手动修正屏幕密度值以达到较理想的展示效果。
     * NOTE: 请在所有画板创建前调用，一次即可。
     * @param density 重设的屏幕密度值。图元绘制时坐标会放大density/2倍。
     * */
    public void correctDensity(float density){
        DefaultPaintBoard.correctDensity(density);
    }

}
