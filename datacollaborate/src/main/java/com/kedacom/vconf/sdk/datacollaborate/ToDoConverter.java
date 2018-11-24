package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.base.KLog;
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
import java.util.HashMap;
import java.util.Map;

final class ToDoConverter {

    public static <T extends OpPaint> T fromTransferObj(MsgBeans.DCPaintOp transferObj, Class<T> t) {

        T domainObj = null;
        try {
            domainObj = t.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        domainObj.setConfE164(transferObj.confE164);
        domainObj.setBoardId(transferObj.boardId);
        domainObj.setPageId(transferObj.pageId);
//        domainObj.setSn(transferObj.sn);

        if (OpDraw.class.isAssignableFrom(t)) {

            OpDraw opDraw = (OpDraw) domainObj;
            MsgBeans.DCDrawOp dcDrawOp = (MsgBeans.DCDrawOp) transferObj;
            opDraw.setStrokeWidth(dcDrawOp.strokeWidth);
            opDraw.setColor(dcDrawOp.color);

            if (OpDrawLine.class == t) {
                OpDrawLine opDrawLine = (OpDrawLine) domainObj;
                MsgBeans.DCLineOp dcLineOp = (MsgBeans.DCLineOp) transferObj;
                opDrawLine.setStartX(dcLineOp.startX);
                opDrawLine.setStartY(dcLineOp.startY);
                opDrawLine.setStopX(dcLineOp.stopX);
                opDrawLine.setStopY(dcLineOp.stopY);
            } else if (OpDrawOval.class == t) {
                OpDrawOval opDrawOval = (OpDrawOval) domainObj;
                MsgBeans.DCOvalOp dcOvalOp = (MsgBeans.DCOvalOp) transferObj;
                opDrawOval.setLeft(dcOvalOp.left);
                opDrawOval.setTop(dcOvalOp.top);
                opDrawOval.setRight(dcOvalOp.right);
                opDrawOval.setBottom(dcOvalOp.bottom);
            } else if (OpDrawRect.class == t) {
                OpDrawRect opDrawRect = (OpDrawRect) domainObj;
                MsgBeans.DCRectOp dcRectOp = (MsgBeans.DCRectOp) transferObj;
                opDrawRect.setLeft(dcRectOp.left);
                opDrawRect.setTop(dcRectOp.top);
                opDrawRect.setRight(dcRectOp.right);
                opDrawRect.setBottom(dcRectOp.bottom);
            } else if (OpDrawPath.class == t) {
                OpDrawPath opDrawPath = (OpDrawPath) domainObj;
                MsgBeans.DCPathOp dcPathOp = (MsgBeans.DCPathOp) transferObj;
                opDrawPath.setPoints(dcPathOp.points);
            }

        }else{

            if (OpInsertPic.class == t) {
                OpInsertPic opInsertPic = (OpInsertPic) domainObj;
                MsgBeans.DCInertPicOp dcInertPicOp = (MsgBeans.DCInertPicOp) transferObj;
                opInsertPic.setPicId(dcInertPicOp.picId);
                opInsertPic.setPicName(dcInertPicOp.picName);
                opInsertPic.setPicWidth(dcInertPicOp.width);
                opInsertPic.setPicHeight(dcInertPicOp.height);
                opInsertPic.setInsertPosX(dcInertPicOp.insertPosX);
                opInsertPic.setInsertPosY(dcInertPicOp.insertPosY);
                opInsertPic.setMatrixValue(matrixValueStr2Float(dcInertPicOp.matrixValue));
            } else if (OpDeletePic.class == t) {
                OpDeletePic opDeletePic = (OpDeletePic) domainObj;
                MsgBeans.DCDelPicOp dcDelPicOp = (MsgBeans.DCDelPicOp) transferObj;
                opDeletePic.setPicIds(dcDelPicOp.picIds);
            } else if (OpDragPic.class == t) {
                OpDragPic opDragPic = (OpDragPic) domainObj;
                MsgBeans.DCDragPicOp dcDragPicOp = (MsgBeans.DCDragPicOp) transferObj;
                Map<String, float[]> picMatrices = new HashMap<>();
                for (MsgBeans.DCPicMatrix picMatrix : dcDragPicOp.picMatrices){
                    picMatrices.put(picMatrix.picId, matrixValueStr2Float(picMatrix.matrixValue));
                }
                opDragPic.setPicMatrices(picMatrices);
            } else if (OpUpdatePic.class == t) {
                // TODO
            } else if (OpMatrix.class == t) {
                OpMatrix opMatrix = (OpMatrix) domainObj;
                MsgBeans.DCFullScreenMatrixOp dcFullScreenMatrixOp = (MsgBeans.DCFullScreenMatrixOp) transferObj;
                opMatrix.setMatrixValue(matrixValueStr2Float(dcFullScreenMatrixOp.matrixValue));
            } else if (OpRectErase.class == t) {
                OpRectErase opRectErase = (OpRectErase) domainObj;
                MsgBeans.DCRectEraseOp dcRectEraseOp = (MsgBeans.DCRectEraseOp) transferObj;
                opRectErase.setLeft(dcRectEraseOp.left);
                opRectErase.setTop(dcRectEraseOp.top);
                opRectErase.setRight(dcRectEraseOp.right);
                opRectErase.setBottom(dcRectEraseOp.bottom);

            } else if (OpUndo.class == t) {
                // Nothing to do
            } else if (OpRedo.class == t) {
                // Nothing to do
            } else if (OpClearScreen.class == t) {
                // Nothing to do
            }

        }

        return domainObj;

    }

    public static <T extends MsgBeans.DCPaintOp> T toTransferObj(OpPaint domainObj, Class<T> t) {
        T transferObj = null;
        try {
            transferObj = t.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        transferObj.id = "todo";// TODO;
        transferObj.confE164 = domainObj.getConfE164();
        transferObj.boardId = domainObj.getBoardId();
        transferObj.pageId = domainObj.getPageId();

        //由平台填写
//        transferObj.sn =
//        transferObj.bCached =
//        transferObj.authorE164 =

        if (MsgBeans.DCDrawOp.class.isAssignableFrom(t)) {
            MsgBeans.DCDrawOp dcDrawOp = (MsgBeans.DCDrawOp) transferObj;
            OpDraw opDraw = (OpDraw) domainObj;
            dcDrawOp.strokeWidth = opDraw.getStrokeWidth();
            dcDrawOp.color = opDraw.getColor();

            if (MsgBeans.DCLineOp.class == t) {
                MsgBeans.DCLineOp dcLineOp = (MsgBeans.DCLineOp) transferObj;
                OpDrawLine opDrawLine = (OpDrawLine) domainObj;
                dcLineOp.startX = opDrawLine.getStartX();
                dcLineOp.startY = opDrawLine.getStartY();
                dcLineOp.stopX = opDrawLine.getStopX();
                dcLineOp.stopY = opDrawLine.getStopY();
            } else if (MsgBeans.DCOvalOp.class == t) {
                MsgBeans.DCOvalOp dcOvalOp = (MsgBeans.DCOvalOp) transferObj;
                OpDrawOval opDrawOval = (OpDrawOval) domainObj;
                dcOvalOp.left = opDrawOval.getLeft();
                dcOvalOp.top = opDrawOval.getTop();
                dcOvalOp.right = opDrawOval.getRight();
                dcOvalOp.bottom = opDrawOval.getBottom();
            } else if (MsgBeans.DCRectOp.class == t) {
                MsgBeans.DCRectOp dcRectOp = (MsgBeans.DCRectOp) transferObj;
                OpDrawRect opDrawRect = (OpDrawRect) domainObj;
                dcRectOp.left = opDrawRect.getLeft();
                dcRectOp.top = opDrawRect.getTop();
                dcRectOp.right = opDrawRect.getRight();
                dcRectOp.bottom = opDrawRect.getBottom();
            } else if (MsgBeans.DCPathOp.class == t) {
                MsgBeans.DCPathOp dcRectOp = (MsgBeans.DCPathOp) transferObj;
                OpDrawPath opDrawPath = (OpDrawPath) domainObj;
                dcRectOp.points = opDrawPath.getPoints();
            }
        }else{
            if (MsgBeans.DCInertPicOp.class == t) {
                MsgBeans.DCInertPicOp dcInertPicOp = (MsgBeans.DCInertPicOp) transferObj;
                OpInsertPic opInsertPic = (OpInsertPic) domainObj;
                dcInertPicOp.picId = opInsertPic.getPicId();
                dcInertPicOp.picName = opInsertPic.getPicName();
                dcInertPicOp.width = opInsertPic.getPicWidth();
                dcInertPicOp.height = opInsertPic.getPicHeight();
                dcInertPicOp.insertPosX = opInsertPic.getInsertPosX();
                dcInertPicOp.insertPosY = opInsertPic.getInsertPosY();
                dcInertPicOp.matrixValue = matrixValueFloat2Str(opInsertPic.getMatrixValue());
            } else if (MsgBeans.DCDelPicOp.class == t) {
                MsgBeans.DCDelPicOp dcDelPicOp = (MsgBeans.DCDelPicOp) transferObj;
                OpDeletePic opDeletePic = (OpDeletePic) domainObj;
                dcDelPicOp.picIds = opDeletePic.getPicIds();
            } else if (MsgBeans.DCDragPicOp.class == t) {
                MsgBeans.DCDragPicOp dcDragPicOp = (MsgBeans.DCDragPicOp) transferObj;
                OpDragPic opDragPic = (OpDragPic) domainObj;
                ArrayList list = new ArrayList<MsgBeans.DCPicMatrix>();
                for (String picId : opDragPic.getPicMatrices().keySet()){
                    list.add(new MsgBeans.DCPicMatrix(picId, matrixValueFloat2Str(opDragPic.getPicMatrices().get(picId))));
                }
                dcDragPicOp.picMatrices = (MsgBeans.DCPicMatrix[]) list.toArray();
            } else if (MsgBeans.DCFullScreenMatrixOp.class == t) {
                MsgBeans.DCFullScreenMatrixOp dcFullScreenMatrixOp = (MsgBeans.DCFullScreenMatrixOp) transferObj;
                OpMatrix opMatrix = (OpMatrix) domainObj;
                dcFullScreenMatrixOp.matrixValue = matrixValueFloat2Str(opMatrix.getMatrixValue());
            } else if (MsgBeans.DCRectEraseOp.class == t) {
                MsgBeans.DCRectEraseOp dcRectEraseOp = (MsgBeans.DCRectEraseOp) transferObj;
                OpRectErase opRectErase = (OpRectErase) domainObj;
                dcRectEraseOp.left = opRectErase.getLeft();
                dcRectEraseOp.top = opRectErase.getTop();
                dcRectEraseOp.right = opRectErase.getRight();
                dcRectEraseOp.bottom = opRectErase.getBottom();
            } else if (MsgBeans.DCPaintOp.class == t) {
                // Nothing to do for clearscreen,undo,redo.
            }
        }

        return transferObj;
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
