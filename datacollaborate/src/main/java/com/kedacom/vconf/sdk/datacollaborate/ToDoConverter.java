package com.kedacom.vconf.sdk.datacollaborate;

import static com.kedacom.vconf.sdk.base.MsgBeans.*;
import static com.kedacom.vconf.sdk.base.MsgConst.*;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.CreateConfResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.EBoardMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EConfMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EConfType;
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
import com.kedacom.vconf.sdk.datacollaborate.bean.ETerminalType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
final class ToDoConverter {

    public static OpPaint fromTransferObj(DCPaintOp transferObj) {
        switch (transferObj.opType){
            case DRAW_LINE:
                return fromTransferObj((DCLineOp)transferObj);
            case DRAW_RECT:
                return fromTransferObj((DCRectOp)transferObj);
            case DRAW_OVAL:
                return fromTransferObj((DCOvalOp)transferObj);
            case DRAW_PATH:
                return fromTransferObj((DCPathOp)transferObj);
            case INSERT_PIC:
                return fromTransferObj((DCInertPicOp)transferObj);
            case DEL_PIC:
                return fromTransferObj((DCDelPicOp)transferObj);
            case DRAG_PIC:
                return fromTransferObj((DCDragPicOp)transferObj);
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
            case emWbEraseOperInfo:
                return fromTransferObj((DCRectEraseOp)transferObj);
            case FULLSCREEN:
                return fromTransferObj((DCFullScreenMatrixOp)transferObj);
            default:
                return null;
        }
    }


    public static DCPaintOp toTransferObj(OpPaint domainObj) {
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



    public static OpDrawLine fromTransferObj(DCLineOp dcLineOp) {
        OpDrawLine opDrawLine = new OpDrawLine();
        assignDrawDomainObj(dcLineOp, opDrawLine);
        opDrawLine.setStartX(dcLineOp.startX);
        opDrawLine.setStartY(dcLineOp.startY);
        opDrawLine.setStopX(dcLineOp.stopX);
        opDrawLine.setStopY(dcLineOp.stopY);
        return opDrawLine;
    }

    public static OpDrawRect fromTransferObj(DCRectOp dcRectOp) {
        OpDrawRect opDrawRect = new OpDrawRect();
        assignDrawDomainObj(dcRectOp, opDrawRect);
        opDrawRect.setLeft(dcRectOp.left);
        opDrawRect.setTop(dcRectOp.top);
        opDrawRect.setRight(dcRectOp.right);
        opDrawRect.setBottom(dcRectOp.bottom);
        return opDrawRect;
    }

    public static OpDrawOval fromTransferObj(DCOvalOp dcOvalOp) {
        OpDrawOval opDrawOval = new OpDrawOval();
        assignDrawDomainObj(dcOvalOp, opDrawOval);
        opDrawOval.setLeft(dcOvalOp.left);
        opDrawOval.setTop(dcOvalOp.top);
        opDrawOval.setRight(dcOvalOp.right);
        opDrawOval.setBottom(dcOvalOp.bottom);
        return opDrawOval;
    }

    public static OpDrawPath fromTransferObj(DCPathOp dcPathOp) {
        OpDrawPath opDrawPath = new OpDrawPath();
        assignDrawDomainObj(dcPathOp, opDrawPath);
        opDrawPath.setPoints(dcPathOp.points);
        return opDrawPath;
    }

    public static OpInsertPic fromTransferObj(DCInertPicOp dcInertPicOp) {
        OpInsertPic opInsertPic = new OpInsertPic();
        assignPaintDomainObj(dcInertPicOp, opInsertPic);
        opInsertPic.setPicId(dcInertPicOp.picId);
        opInsertPic.setPicName(dcInertPicOp.picName); // TODO 确认此字段是否即为fullpath
        opInsertPic.setPicWidth(dcInertPicOp.width);
        opInsertPic.setPicHeight(dcInertPicOp.height);
        opInsertPic.setInsertPosX(dcInertPicOp.insertPosX);
        opInsertPic.setInsertPosY(dcInertPicOp.insertPosY);
        opInsertPic.setMatrixValue(matrixValueStr2Float(dcInertPicOp.matrixValue));
        return opInsertPic;
    }

    public static OpDeletePic fromTransferObj(DCDelPicOp dcDelPicOp) {
        OpDeletePic opDeletePic = new OpDeletePic();
        assignPaintDomainObj(dcDelPicOp, opDeletePic);
        opDeletePic.setPicIds(dcDelPicOp.picIds);
        return opDeletePic;
    }

    public static OpDragPic fromTransferObj(DCDragPicOp dcDragPicOp) {
        OpDragPic opDragPic = new OpDragPic();
        assignPaintDomainObj(dcDragPicOp, opDragPic);
        Map<String, float[]> picMatrices = new HashMap<>();
        for (DCPicMatrix picMatrix : dcDragPicOp.picMatrices){
            picMatrices.put(picMatrix.picId, matrixValueStr2Float(picMatrix.matrixValue));
        }
        opDragPic.setPicMatrices(picMatrices);
        return opDragPic;
    }

//    public static OpUpdatePic fromTransferObj(DCLineOp lineOp) {
//
//    }

    public static OpMatrix fromTransferObj(DCFullScreenMatrixOp dcFullScreenMatrixOp) {
        OpMatrix opMatrix = new OpMatrix();
        assignPaintDomainObj(dcFullScreenMatrixOp, opMatrix);
        opMatrix.setMatrixValue(matrixValueStr2Float(dcFullScreenMatrixOp.matrixValue));
        return opMatrix;
    }

    public static OpRectErase fromTransferObj(DCRectEraseOp dcRectEraseOp) {
        OpRectErase opRectErase = new OpRectErase();
        assignPaintDomainObj(dcRectEraseOp, opRectErase);
        opRectErase.setLeft(dcRectEraseOp.left);
        opRectErase.setTop(dcRectEraseOp.top);
        opRectErase.setRight(dcRectEraseOp.right);
        opRectErase.setBottom(dcRectEraseOp.bottom);
        return opRectErase;
    }




    public static DCLineOp toTransferObj(OpDrawLine domainObj) {
        DCLineOp transferObj = new DCLineOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_LINE;
        transferObj.startX = domainObj.getStartX();
        transferObj.startY = domainObj.getStartY();
        transferObj.stopX = domainObj.getStopX();
        transferObj.stopY = domainObj.getStopY();
        return transferObj;
    }

    public static DCRectOp toTransferObj(OpDrawRect domainObj) {
        DCRectOp transferObj = new DCRectOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_RECT;
        transferObj.left    = domainObj.getLeft();
        transferObj.top     = domainObj.getTop();
        transferObj.right   = domainObj.getRight();
        transferObj.bottom  = domainObj.getBottom();
        return transferObj;
    }

    public static DCOvalOp toTransferObj(OpDrawOval domainObj) {
        DCOvalOp transferObj = new DCOvalOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_OVAL;
        transferObj.left    = domainObj.getLeft();
        transferObj.top     = domainObj.getTop();
        transferObj.right   = domainObj.getRight();
        transferObj.bottom  = domainObj.getBottom();
        return transferObj;
    }

    public static DCPathOp toTransferObj(OpDrawPath domainObj) {
        DCPathOp transferObj = new DCPathOp();
        assignDrawTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAW_PATH;
        transferObj.points = domainObj.getPoints();
        return transferObj;
    }

    public static DCInertPicOp toTransferObj(OpInsertPic domainObj) {
        DCInertPicOp transferObj = new DCInertPicOp();
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

    public static DCDelPicOp toTransferObj(OpDeletePic domainObj) {
        DCDelPicOp transferObj = new DCDelPicOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DEL_PIC;
        transferObj.picIds = domainObj.getPicIds();
        return transferObj;
    }

    public static DCDragPicOp toTransferObj(OpDragPic domainObj) {
        DCDragPicOp transferObj = new DCDragPicOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.DRAG_PIC;
        ArrayList<DCPicMatrix> list = new ArrayList<>();
        for (String picId : domainObj.getPicMatrices().keySet()){
            list.add(new DCPicMatrix(picId, matrixValueFloat2Str(domainObj.getPicMatrices().get(picId))));
        }
        transferObj.picMatrices = (DCPicMatrix[]) list.toArray();
        return transferObj;
    }

    public static DCFullScreenMatrixOp toTransferObj(OpMatrix domainObj) {
        DCFullScreenMatrixOp transferObj = new DCFullScreenMatrixOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.FULLSCREEN;
        transferObj.matrixValue = matrixValueFloat2Str(domainObj.getMatrixValue());
        return transferObj;
    }

    public static DCRectEraseOp toTransferObj(OpRectErase domainObj) {
        DCRectEraseOp transferObj = new DCRectEraseOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.RECT_ERASE;
        transferObj.left = domainObj.getLeft();
        transferObj.top = domainObj.getTop();
        transferObj.right = domainObj.getRight();
        transferObj.bottom = domainObj.getBottom();
        return transferObj;
    }

    public static DCPaintOp toTransferObj(OpClearScreen domainObj) {
        DCPaintOp transferObj = new DCPaintOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.CLEAR_SCREEN;
        return transferObj;
    }

    public static DCPaintOp toTransferObj(OpUndo domainObj) {
        DCPaintOp transferObj = new DCPaintOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.UNDO;
        return transferObj;
    }

    public static DCPaintOp toTransferObj(OpRedo domainObj) {
        DCPaintOp transferObj = new DCPaintOp();
        assignPaintTransferObj(domainObj, transferObj);
        transferObj.opType = EDcOpType.REDO;
        return transferObj;
    }



    public static void assignPaintDomainObj(DCPaintOp transferObj, OpPaint domainObj){
        domainObj.setConfE164(transferObj.confE164);
        domainObj.setBoardId(transferObj.boardId);
        domainObj.setPageId(transferObj.pageId);
        domainObj.setSn(transferObj.sn);
    }

    public static void assignDrawDomainObj(DCDrawOp transferObj, OpDraw domainObj){
        assignPaintDomainObj(transferObj, domainObj);
        domainObj.setStrokeWidth(transferObj.strokeWidth);
        domainObj.setColor(transferObj.color);
    }

    public static void assignPaintTransferObj(OpPaint domainObj, DCPaintOp transferObj){
        transferObj.id = "todo";// TODO;
        transferObj.confE164 = domainObj.getConfE164();
        transferObj.boardId = domainObj.getBoardId();
        transferObj.pageId = domainObj.getPageId();
    }

    public static void assignDrawTransferObj(OpDraw domainObj, DCDrawOp transferObj){
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


    public static EmDcsType toTransferObj(ETerminalType type){
        switch (type){
            case TrueLinkWindows:
                return EmDcsType.emTypeTrueLink;
            case TrueLinkIosPhone:
                return EmDcsType.emTypeTrueTouchPhoneIOS;
            case TrueLinkIosPad:
                return EmDcsType.emTypeTrueTouchPadIOS;
            case TrueLinkAndroidPhone:
                return EmDcsType.emTypeTrueTouchPhoneAndroid;
            case TrueLinkAndroidPad:
                return EmDcsType.emTypeTrueTouchPadAndroid;
            case TrueSens:
                return EmDcsType.emTypeTrueSens;
            case Imix:
                return EmDcsType.emTypeIMIX;
            case ThirdParty:
                return EmDcsType.emTypeThirdPartyTer;
            case Unknown:
            default:
                return EmDcsType.emTypeUnknown;
        }
    }

    public static EConfType fromTransferObj(EmDcsConfType dcsConfType) {
        switch (dcsConfType){
            case emConfTypeP2P:
                return EConfType.P2P;
            case emConfTypeMCC:
                return EConfType.MCC;
            default:
                return EConfType.P2P;
        }
    }

    public static EConfMode fromTransferObj(EmDcsConfMode dcsConfModed) {
        switch (dcsConfModed){
            case emConfModeAuto:
                return EConfMode.Auto;
            case emConfModeManage:
                return EConfMode.Manage;
            case emConfModeStop:
                return EConfMode.Stop;
            default:
                return EConfMode.Auto;
        }
    }

    public static CreateConfResult fromTransferObj(DCCreateConfResult dcCreateConfResult) {
        CreateConfResult createConfResult = new CreateConfResult();
        createConfResult.setConfE164(dcCreateConfResult.confE164);
        createConfResult.setConfName(dcCreateConfResult.confName);
        createConfResult.setConfType(fromTransferObj(dcCreateConfResult.confType));
        createConfResult.setConfMode(fromTransferObj(dcCreateConfResult.confMode));
        return createConfResult;
    }


    public static EBoardMode fromTransferObj(EmDcsWbMode dcsWbMode) {
        switch (dcsWbMode){
            case emWbModeWB:
                return EBoardMode.Normal;
            case emWBModeDOC:
                return EBoardMode.Doc;
            default:
                return EBoardMode.Normal;
        }
    }

    public static BoardInfo fromTransferObj(DCBoard dcBoard) {
        BoardInfo boardInfo = new BoardInfo();
        boardInfo.setId(dcBoard.id);
        boardInfo.setConfE164(dcBoard.confE164);
        boardInfo.setCreatorE164(dcBoard.creatorE164);
        boardInfo.setCreateTime(dcBoard.createTime);
        boardInfo.setMode(fromTransferObj(dcBoard.mode));
        boardInfo.setPageNum(dcBoard.pageNum);
        boardInfo.setPageId(dcBoard.pageId);
        boardInfo.setAnonyId(dcBoard.anonyId);
        return boardInfo;
    }

}
