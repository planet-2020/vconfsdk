package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpMatrix extends OpPaint {
    private float[] matrixValue;

    public OpMatrix(){
        type = OP_MATRIX;
    }

    public OpMatrix(float[] matrixValue){
        this.matrixValue = matrixValue;
        type = OP_MATRIX;
    }


    public OpMatrix fromTransferObj(MsgBeans.DCFullScreenMatrixOp to) {
        super.fromTransferObj(to);
        matrixValue = matrixValueStr2Float(to.matrixValue);
        return this;
    }

    public MsgBeans.DCFullScreenMatrixOp toTransferObj(MsgBeans.DCFullScreenMatrixOp to) {
        super.toTransferObj(to);
        to.matrixValue = matrixValueFloat2Str(matrixValue);
        return to;
    }



    public float[] getMatrixValue() {
        return matrixValue;
    }

    public void setMatrixValue(float[] matrixValue) {
        this.matrixValue = matrixValue;
    }
}
