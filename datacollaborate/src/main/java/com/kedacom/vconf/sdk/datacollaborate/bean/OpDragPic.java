package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpDragPic extends OpPaint {

    private Map<String, float[]> picsMatrix;

    public OpDragPic(){
        type = OP_DRAG_PICTURE;
    }

    public OpDragPic(Map<String, float[]> picsMatrix) {
        this.picsMatrix = picsMatrix;
        type = OP_DRAG_PICTURE;
    }


    public OpDragPic fromTransferObj(MsgBeans.DCDragPicOp to) {
        super.fromTransferObj(to);
        MsgBeans.DCPicMatrix[] matrices = to.picMatrices;
        for (MsgBeans.DCPicMatrix matrix : matrices){
            picsMatrix.put(matrix.picId, matrixValueStr2Float(matrix.matrixValue));
        }
        return this;
    }

    public MsgBeans.DCDragPicOp toTransferObj(MsgBeans.DCDragPicOp to) {
        super.toTransferObj(to);
        List list = new ArrayList<MsgBeans.DCPicMatrix>();
        for (String picId : picsMatrix.keySet()){
            list.add(new MsgBeans.DCPicMatrix(picId, matrixValueFloat2Str(picsMatrix.get(picId))));
        }
        to.picMatrices = (MsgBeans.DCPicMatrix[]) list.toArray();

        return to;
    }

    public Map<String, float[]> getPicsMatrix() {
        return picsMatrix;
    }

    public void setPicsMatrix(Map<String, float[]> picsMatrix) {
        this.picsMatrix = picsMatrix;
    }

}
