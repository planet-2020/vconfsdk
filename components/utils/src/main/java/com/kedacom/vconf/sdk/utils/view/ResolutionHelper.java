package com.kedacom.vconf.sdk.utils.view;

public final class ResolutionHelper {

    public static final int SCALE_TO_FIT = 0;
    public static final int SCALE_ASPECT_FIT = 1;
    public static final int SCALE_ASPECT_FILL = 2;

    /**
     * 根据目标宽高及缩放策略对源宽高进行缩放处理，返回处理结果。
     * @param strategy {@link #SCALE_TO_FIT},{@link #SCALE_ASPECT_FIT},{@link #SCALE_ASPECT_FILL}
     * @return float[] result. result[0]为处理后的宽，result[1]为处理后的高, result[2]为宽的缩放比，result[3]为高的缩放比。
     * */
    public static float[] adjust(int srcWidth, int srcHeight, int dstWidth, int dstHeight, int strategy){
        float width=srcWidth, height=srcHeight, scaleW=1, scaleH=1;
        if (SCALE_ASPECT_FIT == strategy){
            if (width*dstHeight > dstWidth*height){ // 源分辨率对比目标分辨率显得比较宽胖
                // 维持比例缩放使得宽刚好贴合目标宽
                scaleW = scaleH = dstWidth/width;
                width = dstWidth;
                height *= scaleH;
            }else { // 源分辨率对比目标分辨率显得比较高瘦
                // 维持比例缩放使得高刚好贴合目标高
                scaleW = scaleH = dstHeight/height;
                height = dstHeight;
                width *= scaleW;
            }
        }else if (SCALE_ASPECT_FILL == strategy){
            if (width*dstHeight > dstWidth*height){ // 源分辨率对比目标分辨率显得比较宽胖
                // 维持比例缩放使得高刚好贴合目标高
                scaleW = scaleH = dstHeight/height;
                height = dstHeight;
                width *= scaleW;
            }else { // 源分辨率对比目标分辨率显得比较高瘦
                // 维持比例缩放使得宽刚好贴合目标宽
                scaleW = scaleH = dstWidth/width;
                width = dstWidth;
                height *= scaleH;
            }
        }else if (SCALE_TO_FIT == strategy){
            scaleW = dstWidth/width;
            scaleH = dstHeight/height;
            width = dstWidth;
            height = dstHeight;
        }

        return new float[]{width, height, scaleW, scaleH};
    }


    /**
     * 将源宽高进行最小缩放处理以至符合目标宽高比
     * @param shrink true,进行缩小处理以满足目标宽高比，false,进行放大处理以满足目标宽高比。
     * @param limitedHeight 高度上限。转换出的高度不能超过该限制但可以小于
     * @return float[] result. result[0]为处理后的宽，result[1]为处理后的高, result[2]为宽的缩放比，result[3]为高的缩放比。
     * */
    public static float[] adjust(int srcWidth, int srcHeight, float dstRatio, int limitedHeight, boolean shrink){
        float width=srcWidth, height=srcHeight, scaleW=1, scaleH=1;
        if ((width > height*dstRatio && shrink) // 如果源比较宽且是缩小策略
                || (width < height*dstRatio && !shrink) // 或者源比较高且是放大策略
        ){
            // 则对宽进行缩放处理以匹配目标宽高比
            width = (int) (height*dstRatio);
        }else {
            // 否则对高进行缩放处理以匹配目标宽高比
            height = (int) (width/dstRatio);
        }
        if (height>limitedHeight){
            // 超出限制则等比缩小
            float scaleFactor = limitedHeight/height;
            width *=scaleFactor;
            height *=scaleFactor;
        }
        scaleW = width/srcWidth;
        scaleH = height/srcHeight;
        return new float[]{width, height, scaleW, scaleH};
    }

}
