package com.kedacom.vconf.sdk.utils.view;

public final class ResolutionHelper {

    /**
     * 维持比例适应目标窗口。处理后的矩形(sw/sh)至少一边贴合目标矩形(dw/dh)且在目标矩形内或重合，关系如下所示：
     *  |------ dw ------|
     *  ------------------  -
     *  |            |   |  |
     *  |            |   |dh,sh
     *  |            |   |  |
     *  |-----------------  -
     *  |--- sw -----|
     */
    public static final int SCALE_ASPECT_FIT = 1;

    /**
     * 维持比例填充目标窗口。处理后的矩形(sw/sh)至少一边贴合目标矩形(dw/dh)且在目标矩形外或重合，关系如下所示：
     *  |------ sw ------|
     *  ------------------  -
     *  |            |   |  |
     *  |            |   |dh,sh
     *  |            |   |  |
     *  |-----------------  -
     *  |--- dw -----|
     * */
    public static final int SCALE_ASPECT_FILL = 2;

    /**
     * 根据目标宽高及缩放策略对源宽高进行缩放处理，返回处理结果。
     * @param strategy {@link #SCALE_ASPECT_FIT},{@link #SCALE_ASPECT_FILL}
     * @return int[] result. result[0]为处理后的宽，result[1]为处理后的高。
     * */
    public static int[] adjust(int srcWidth, int srcHeight, int dstWidth, int dstHeight, int strategy){
        if (srcWidth == dstWidth && srcHeight == dstHeight){
            return new int[]{srcWidth, srcHeight};
        }
        int[] result;
        if (SCALE_ASPECT_FIT == strategy || SCALE_ASPECT_FILL == strategy){
            boolean fatter = isFatter(srcWidth, srcHeight, dstWidth, dstHeight);
            if ((fatter && SCALE_ASPECT_FIT == strategy)
                    || (!fatter && SCALE_ASPECT_FILL == strategy)){
                result = alignWidth(srcWidth, srcHeight, dstWidth);
            }else {
                result = alignHeight(srcWidth, srcHeight, dstHeight);
            }
        }else {
            result = new int[]{srcWidth, srcHeight};
        }

        return result;
    }

    /**
     * 源窗口是否比目标窗口更“胖”，即宽高比更大。
     * */
    private static boolean isFatter(int srcWidth, int srcHeight, int dstWidth, int dstHeight){
        return srcWidth*dstHeight > dstWidth*srcHeight;
    }

    /**
     * （按比例缩放以）对齐宽
     * @return int[] result. result[0]为处理后的宽，result[1]为处理后的高。
     * */
    private static int[] alignWidth(int srcWidth, int srcHeight, int dstWidth){
        return new int[]{dstWidth, (int) (srcHeight*((float)dstWidth/srcWidth))};
    }

    /**
     * （按比例缩放以）对齐高
     * @return int[] result. result[0]为处理后的宽，result[1]为处理后的高。
     * */
    private static int[] alignHeight(int srcWidth, int srcHeight, int dstHeight){
        return new int[]{(int) (srcWidth*((float)dstHeight/srcHeight)), dstHeight};
    }

}
