package com.kedacom.vconf.sdk.datacollaborate;

import com.kedacom.vconf.sdk.base.MsgBeans;
import static com.kedacom.vconf.sdk.base.MsgConst.*;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen; // TODO 改为static
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

    public static OpPaint fromTransferObj(MsgBeans.DCPaintOp transferObj) {
        switch (transferObj.opType){
            case DRAW_LINE:
                return fromTransferObj((MsgBeans.DCLineOp)transferObj);
            case DRAW_RECT:
                return fromTransferObj((MsgBeans.DCRectOp)transferObj);
            case DRAW_OVAL:
                return fromTransferObj((MsgBeans.DCOvalOp)transferObj);
            case DRAW_PATH:
                return fromTransferObj((MsgBeans.DCPathOp)transferObj);
            case INSERT_PIC:
                return fromTransferObj((MsgBeans.DCInertPicOp)transferObj);
            case DEL_PIC:
                return fromTransferObj((MsgBeans.DCDelPicOp)transferObj);
            case DRAG_PIC:
                return fromTransferObj((MsgBeans.DCDragPicOp)transferObj);
            case ZOOM_PIC:
            case ROTATE_PIC:
            case RIGHT_ROTATE:
            case LEFT_ROTATE:
                return null; // TODO
            case UNDO:
                OpUndo opUndo = new OpUndo();
                assignPaintDomainObj(transferObj, opUndo);
                return opUndo;
            case REDO:
                OpRedo opRedo = new OpRedo();
                assignPaintDomainObj(transferObj, opRedo);
                return opRedo;
            case CLEAR_SCREEN:
                OpClearScreen opClearScreen = new OpClearScreen();
                assignPaintDomainObj(transferObj, opClearScreen);
                return opClearScreen;
            case RECT_ERASE:
                return fromTransferObj((MsgBeans.DCRectEraseOp)transferObj);
            case FULLSCREEN:
                return fromTransferObj((MsgBeans.DCFullScreenMatrixOp)transferObj);
            default:
                return null;
        }
    }


    public static MsgBeans.DCPaintOp toTransferObj(OpPaint domainObj) {
        switch (domainObj.getType()){
            case DRAW_LINE:
                return toTransferObj((OpDrawLine)domainObj);
            case DRAW_RECT:
                return toTransferObj((OpDrawRect) domainObj);
            case DRAW_OVAL:
                return toTransferObj((OpDrawOval) domainObj);
            case DRAW_PATH:
                return toTransferObj((OpDrawPath) domainObj);
            case UNDO:
                return toTransferObj((OpUndo) domainObj);
            case REDO:
                return toTransferObj((OpRedo) domainObj);
            case FULLSCREEN_MATRIX:
                return toTransferObj((OpMatrix) domainObj);
            case RECT_ERASE:
                return toTransferObj((OpRectErase) domainObj);
            case CLEAR_SCREEN:
                return toTransferObj((OpClearScreen)domainObj);
            case INSERT_PICTURE:
                return toTransferObj((OpInsertPic) domainObj);
            case DELETE_PICTURE:
                return toTransferObj((OpDeletePic) domainObj);
            case DRAG_PICTURE:
                return toTransferObj((OpDragPic) domainObj);
            case UPDATE_PICTURE:
//                return toTransferObj((OpUpdatePic)domainObj);
            default:
                return null;
        }
    }



    public static OpDrawLine fromTransferObj(MsgBeans.DCLineOp dcLineOp) {
        OpDrawLine opDrawLine = new OpDrawLine();
        assignDrawDomainObj(dcLineOp, opDrawLine);
        opDrawLine.setStartX(dcLineOp.startX);
        opDrawLine.setStartY(dcLineOp.startY);
        opDrawLine.setStopX(dcLineOp.stopX);
        opDrawLine.setStopY(dcLineOp.stopY);
        return opDrawLine;
    }

    public static OpDrawRect fromTransferObj(MsgBeans.DCRectOp dcRectOp) {
        OpDrawRect opDrawRect = new OpDrawRect();
        assignDrawDomainObj(dcRectOp, opDrawRect);
        opDrawRect.setLeft(dcRectOp.left);
        opDrawRect.setTop(dcRectOp.top);
        opDrawRect.setRight(dcRectOp.right);
        opDrawRect.setBottom(dcRectOp.bottom);
        return opDrawRect;
    }

    public static OpDrawOval fromTransferObj(MsgBeans.DCOvalOp dcOvalOp) {
        OpDrawOval opDrawOval = new OpDrawOval();
        assignDrawDomainObj(dcOvalOp, opDrawOval);
        opDrawOval.setLeft(dcOvalOp.left);
        opDrawOval.setTop(dcOvalOp.top);
        opDrawOval.setRight(dcOvalOp.right);
        opDrawOval.setBottom(dcOvalOp.bottom);
        return opDrawOval;
    }

    public static OpDrawPath fromTransferObj(MsgBeans.DCPathOp dcPathOp) {
        OpDrawPath opDrawPath = new OpDrawPath();
        assignDrawDomainObj(dcPathOp, opDrawPath);
        opDrawPath.setPoints(dcPathOp.points);
        return opDrawPath;
    }

    public static OpInsertPic fromTransferObj(MsgBeans.DCInertPicOp dcInertPicOp) {
        OpInsertPic opInsertPic = new OpInsertPic();
        assignPaintDomainObj(dcInertPicOp, opInsertPic);
        opInsertPic.setPicId(dcInertPicOp.picId);
        opInsertPic.setPicName(dcInertPicOp.picName);
        opInsertPic.setPicWidth(dcInertPicOp.width);
        opInsertPic.setPicHeight(dcInertPicOp.height);
        opInsertPic.setInsertPosX(dcInertPicOp.insertPosX);
        opInsertPic.setInsertPosY(dcInertPicOp.insertPosY);
        opInsertPic.setMatrixValue(matrixValueStr2Float(dcInertPicOp.matrixValue));
        return opInsertPic;
    }

    public static OpDeletePic fromTransferObj(MsgBeans.DCDelPicOp dcDelPicOp) {
        OpDeletePic opDeletePic = new OpDeletePic();
        assignPaintDomainObj(dcDelPicOp, opDeletePic);
        opDeletePic.setPicIds(dcDelPicOp.picIds);
        return opDeletePic;
    }

    public static OpDragPic fromTransferObj(MsgBeans.DCDragPicOp dcDragPicOp) {
        OpDragPic opDragPic = new OpDragPic();
        assignPaintDomainObj(dcDragPicOp, opDragPic);
        Map<String, float[]> picMatrices = new HashMap<>();
        for (MsgBeans.DCPicMatrix picMatrix : dcDragPicOp.picMatrices){
            picMatrices.put(picMatrix.picId, matrixValueStr2Float(picMatrix.matrixValue));
        }
        opDragPic.setPicMatrices(picMatrices);
        return opDragPic;
    }

//    public static OpUpdatePic fromTransferObj(MsgBeans.DCLineOp lineOp) {
//
//    }

    public static OpMatrix fromTransferObj(MsgBeans.DCFullScreenMatrixOp dcFullScreenMatrixOp) {
        OpMatrix opMatrix = new OpMatrix();
        assignPaintDomainObj(dcFullScreenMatrixOp, opMatrix);
        opMatrix.setMatrixValue(matrixValueStr2Float(dcFullScreenMatrixOp.matrixValue));
        return opMatrix;
    }

    public static OpRectErase fromTransferObj(MsgBeans.DCRectEraseOp dcRectEraseOp) {
        OpRectErase opRectErase = new OpRectErase();
        assignPaintDomainObj(dcRectEraseOp, opRectErase);
        opRectErase.setLeft(dcRectEraseOp.left);
        opRectErase.setTop(dcRectEraseOp.top);
        opRectErase.setRight(dcRectEraseOp.right);
        opRectErase.setBottom(dcRectEraseOp.bottom);
        return opRectErase;
    }




    public static MsgBeans.DCLineOp toTransferObj(OpDrawLine domainObj) {
        MsgBeans.DCLineOp transferObj = new MsgBeans.DCLineOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_LINE;
        transferObj.startX = domainObj.getStartX();
        transferObj.startY = domainObj.getStartY();
        transferObj.stopX = domainObj.getStopX();
        transferObj.stopY = domainObj.getStopY();
        return transferObj;
    }

    public static MsgBeans.DCRectOp toTransferObj(OpDrawRect domainObj) {
        MsgBeans.DCRectOp transferObj = new MsgBeans.DCRectOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_RECT;
        transferObj.left    = domainObj.getLeft();
        transferObj.top     = domainObj.getTop();
        transferObj.right   = domainObj.getRight();
        transferObj.bottom  = domainObj.getBottom();
        return transferObj;
    }

    public static MsgBeans.DCOvalOp toTransferObj(OpDrawOval domainObj) {
        MsgBeans.DCOvalOp transferObj = new MsgBeans.DCOvalOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_OVAL;
        transferObj.left    = domainObj.getLeft();
        transferObj.top     = domainObj.getTop();
        transferObj.right   = domainObj.getRight();
        transferObj.bottom  = domainObj.getBottom();
        return transferObj;
    }

    public static MsgBeans.DCPathOp toTransferObj(OpDrawPath domainObj) {
        MsgBeans.DCPathOp transferObj = new MsgBeans.DCPathOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_PATH;
        transferObj.points = domainObj.getPoints();
        return transferObj;
    }

    public static MsgBeans.DCInertPicOp toTransferObj(OpInsertPic domainObj) {
        MsgBeans.DCInertPicOp transferObj = new MsgBeans.DCInertPicOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.INSERT_PIC;
        transferObj.picId = domainObj.getPicId();
        transferObj.picName = domainObj.getPicName();
        transferObj.width = domainObj.getPicWidth();
        transferObj.height = domainObj.getPicHeight();
        transferObj.insertPosX = domainObj.getInsertPosX();
        transferObj.insertPosY = domainObj.getInsertPosY();
        transferObj.matrixValue = matrixValueFloat2Str(domainObj.getMatrixValue());
        return transferObj;
    }

    public static MsgBeans.DCDelPicOp toTransferObj(OpDeletePic domainObj) {
        MsgBeans.DCDelPicOp transferObj = new MsgBeans.DCDelPicOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DEL_PIC;
        transferObj.picIds = domainObj.getPicIds();
        return transferObj;
    }

    public static MsgBeans.DCDragPicOp toTransferObj(OpDragPic domainObj) {
        MsgBeans.DCDragPicOp transferObj = new MsgBeans.DCDragPicOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAG_PIC;
        ArrayList list = new ArrayList<MsgBeans.DCPicMatrix>();
        for (String picId : domainObj.getPicMatrices().keySet()){
            list.add(new MsgBeans.DCPicMatrix(picId, matrixValueFloat2Str(domainObj.getPicMatrices().get(picId))));
        }
        transferObj.picMatrices = (MsgBeans.DCPicMatrix[]) list.toArray();
        return transferObj;
    }

    public static MsgBeans.DCFullScreenMatrixOp toTransferObj(OpMatrix domainObj) {
        MsgBeans.DCFullScreenMatrixOp transferObj = new MsgBeans.DCFullScreenMatrixOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.FULLSCREEN;
        transferObj.matrixValue = matrixValueFloat2Str(domainObj.getMatrixValue());
        return transferObj;
    }

    public static MsgBeans.DCRectEraseOp toTransferObj(OpRectErase domainObj) {
        MsgBeans.DCRectEraseOp transferObj = new MsgBeans.DCRectEraseOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.RECT_ERASE;
        transferObj.left = domainObj.getLeft();
        transferObj.top = domainObj.getTop();
        transferObj.right = domainObj.getRight();
        transferObj.bottom = domainObj.getBottom();
        return transferObj;
    }

    public static MsgBeans.DCPaintOp toTransferObj(OpClearScreen domainObj) {
        MsgBeans.DCPaintOp transferObj = new MsgBeans.DCPaintOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.CLEAR_SCREEN;
        return transferObj;
    }

    public static MsgBeans.DCPaintOp toTransferObj(OpUndo domainObj) {
        MsgBeans.DCPaintOp transferObj = new MsgBeans.DCPaintOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.UNDO;
        return transferObj;
    }

    public static MsgBeans.DCPaintOp toTransferObj(OpRedo domainObj) {
        MsgBeans.DCPaintOp transferObj = new MsgBeans.DCPaintOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.REDO;
        return transferObj;
    }



    public static void assignPaintDomainObj(MsgBeans.DCPaintOp transferObj, OpPaint domainObj){
        domainObj.setConfE164(transferObj.confE164);
        domainObj.setBoardId(transferObj.boardId);
        domainObj.setPageId(transferObj.pageId);
    }

    public static void assignDrawDomainObj(MsgBeans.DCDrawOp transferObj, OpDraw domainObj){
        assignPaintDomainObj(transferObj, domainObj);
        domainObj.setStrokeWidth(transferObj.strokeWidth);
        domainObj.setColor(transferObj.color);
    }

    public static void assignPaintTransferObj(OpPaint domainObj, MsgBeans.DCPaintOp transferObj){
        transferObj.id = "todo";// TODO;
        transferObj.confE164 = domainObj.getConfE164();
        transferObj.boardId = domainObj.getBoardId();
        transferObj.pageId = domainObj.getPageId();
    }

    public static void assignDrawTransferObj(OpDraw domainObj, MsgBeans.DCDrawOp transferObj){
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.strokeWidth = domainObj.getStrokeWidth();
        transferObj.color = domainObj.getColor();
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
