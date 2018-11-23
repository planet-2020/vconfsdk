package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRectErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRedo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUndo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;

import java.util.ArrayList;
import java.util.List;

public final class ToDoConverter {

    public static <D extends OpPaint, T extends MsgBeans.DCPaintOp>
    D fromTransferObj(MsgBeans.DCPaintOp transferObj, Class<D> d, Class<T> t) {

        D domainObj = null;
        try {
            domainObj = d.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        domainObj.setConfE164(transferObj.confE164);
        domainObj.setBoardId(transferObj.boardId);
        domainObj.setPageId(transferObj.pageId);
        domainObj.setSn(transferObj.sn);

        if (OpDraw.class == d) {

            OpDraw opDraw = (OpDraw) domainObj;
            MsgBeans.DCDrawOp dcDrawOp = (MsgBeans.DCDrawOp) transferObj;
            opDraw.setStrokeWidth(dcDrawOp.strokeWidth);
            opDraw.setColor(dcDrawOp.color);

            if (OpDrawLine.class == d) {
                OpDrawLine opDrawLine = (OpDrawLine) domainObj;
                MsgBeans.DCLineOp dcLineOp = (MsgBeans.DCLineOp) transferObj;
                opDrawLine.setStartX(dcLineOp.startX);
                opDrawLine.setStartY(dcLineOp.startY);
                opDrawLine.setStopX(dcLineOp.stopX);
                opDrawLine.setStopY(dcLineOp.stopY);
            } else if (OpDrawOval.class == d) {
                OpDrawOval opDrawOval = (OpDrawOval) domainObj;
                MsgBeans.DCOvalOp dcOvalOp = (MsgBeans.DCOvalOp) transferObj;
                opDrawOval.setLeft(dcOvalOp.left);
                opDrawOval.setTop(dcOvalOp.top);
                opDrawOval.setRight(dcOvalOp.right);
                opDrawOval.setBottom(dcOvalOp.bottom);
            } else if (OpDrawRect.class == d) {
                OpDrawRect opDrawRect = (OpDrawRect) domainObj;
                MsgBeans.DCRectOp dcRectOp = (MsgBeans.DCRectOp) transferObj;
                opDrawRect.setLeft(dcRectOp.left);
                opDrawRect.setTop(dcRectOp.top);
                opDrawRect.setRight(dcRectOp.right);
                opDrawRect.setBottom(dcRectOp.bottom);
            } else if (OpDrawPath.class == d) {
                OpDrawPath opDrawPath = (OpDrawPath) domainObj;
                MsgBeans.DCPathOp dcPathOp = (MsgBeans.DCPathOp) transferObj;
                opDrawPath.setPoints(dcPathOp.points);
            }

        }else{

            if (OpInsertPic.class == d) {
                OpInsertPic opInsertPic = (OpInsertPic) domainObj;
                MsgBeans.DCInertPicOp dcInertPicOp = (MsgBeans.DCInertPicOp) transferObj;
                opInsertPic.setPicId(dcInertPicOp.picId);
                opInsertPic.setPicWidth(dcInertPicOp.width);
                opInsertPic.setPicHeight(dcInertPicOp.height);
                opInsertPic.setInsertPosX(dcInertPicOp.dstPos.x);
                opInsertPic.setInsertPosY(dcInertPicOp.dstPos.y);
                opInsertPic.setMatrixValue(matrixValueStr2Float(dcInertPicOp.matrixValue));
            } else if (OpDeletePic.class == d) {
                OpDeletePic opDeletePic = (OpDeletePic) domainObj;
                MsgBeans.DCDelPicOp dcDelPicOp = (MsgBeans.DCDelPicOp) transferObj;
                opDeletePic.setPicIds(dcDelPicOp.picIds);
            } else if (OpDragPic.class == d) {
                OpDragPic opDragPic = (OpDragPic) domainObj;
                MsgBeans.DCDragPicOp dcDragPicOp = (MsgBeans.DCDragPicOp) transferObj;
                List list = new ArrayList<MsgBeans.DCPicMatrix>();
                for (String picId : picsMatrix.keySet()){  // TODO
                    list.add(new MsgBeans.DCPicMatrix(picId, matrixValueFloat2Str(picsMatrix.get(picId))));
                }
                to.picMatrices = (MsgBeans.DCPicMatrix[]) list.toArray();
                opDragPic.setPicsMatrix();
            } else if (OpUpdatePic.class == d) {

            } else if (OpMatrix.class == d) {

            } else if (OpRectErase.class == d) {

            } else if (OpUndo.class == d) {

            } else if (OpRedo.class == d) {

            } else if (OpClearScreen.class == d) {

            }

        }

    }

    public static <T extends MsgBeans.DCPaintOp> T toTransferObj(OpPaint domainObj, Class<T> t) {
        if (domainObj instanceof OpDraw) {
            if (domainObj instanceof OpDrawLine) {

            } else if (domainObj instanceof OpDrawOval) {

            } else if (domainObj instanceof OpDrawRect) {

            } else if (domainObj instanceof OpDrawPath) {

            }
        }else{
            if (domainObj instanceof OpInsertPic) {

            } else if (domainObj instanceof OpDeletePic) {

            } else if (domainObj instanceof OpUpdatePic) {

            } else if (domainObj instanceof OpDragPic) {

            } else if (domainObj instanceof OpClearScreen) {

            } else if (domainObj instanceof OpMatrix) {

            } else if (domainObj instanceof OpRectErase) {

            } else if (domainObj instanceof OpUndo) {

            } else if (domainObj instanceof OpRedo) {

            }
        }
    }




    private static float[] matrixValueStr2Float(String[] strMatrixValue){
        float[] matrixValue = new float[9];
        for (int i=0; i<9; ++i){
            matrixValue[i] = Float.valueOf(strMatrixValue[i]);
        }
        return matrixValue;
    }

    private static String[] matrixValueFloat2Str(float[] matrixValue){
        String[] strMatrixValue = new String[9];
        for (int i=0; i<9; ++i){
            strMatrixValue[i] = ""+matrixValue[i];
        }
        return strMatrixValue;
    }

}
