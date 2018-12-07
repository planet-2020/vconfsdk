package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.PointF;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperCircleOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperEraseOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperFullScreenNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperInsertPicNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperLineOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperPencilOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperPitchPicDelNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperPitchPicDragNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperRectangleOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperRedoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperUndoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsConfMode;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsConfType;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsOper;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsType;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsWbMode;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConfResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperContent;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbCircle;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbCircleOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbEntity;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbEraseOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbGraphsInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbInsertPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbLine;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbLineOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPencil;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPencilOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPoint;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbRectangle;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbRectangleOperInfo;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("WeakerAccess")
final class ToDoConverter {

    public static OpPaint fromPaintTransferObj(Object transferObj) {
        if (transferObj instanceof DcsOperLineOperInfoNtf){
            return fromTransferObj((DcsOperLineOperInfoNtf)transferObj);
        }else if (transferObj instanceof DcsOperRectangleOperInfoNtf){
            return fromTransferObj((DcsOperRectangleOperInfoNtf)transferObj);
        }else if (transferObj instanceof DcsOperCircleOperInfoNtf){
            return fromTransferObj((DcsOperCircleOperInfoNtf)transferObj);
        }else if (transferObj instanceof DcsOperPencilOperInfoNtf){
            return fromTransferObj((DcsOperPencilOperInfoNtf)transferObj);
        }else if (transferObj instanceof DcsOperInsertPicNtf){
            return fromTransferObj((DcsOperInsertPicNtf)transferObj);
        }else if (transferObj instanceof DcsOperPitchPicDragNtf){
            return fromTransferObj((DcsOperPitchPicDragNtf)transferObj);
        }else if (transferObj instanceof DcsOperPitchPicDelNtf){
            return fromTransferObj((DcsOperPitchPicDelNtf)transferObj);
        }else if (transferObj instanceof DcsOperFullScreenNtf){
            return fromTransferObj((DcsOperFullScreenNtf)transferObj);
        }else if (transferObj instanceof DcsOperEraseOperInfoNtf){
            return fromTransferObj((DcsOperEraseOperInfoNtf)transferObj);
        }else if (transferObj instanceof DcsOperUndoNtf){
            return fromTransferObj((DcsOperUndoNtf)transferObj);
        }else if (transferObj instanceof DcsOperRedoNtf){
            return fromTransferObj((DcsOperRedoNtf)transferObj);
        }else if (transferObj instanceof TDCSOperContent){
            if (((TDCSOperContent)transferObj).emOper.equals(EmDcsOper.emWbClearScreen)){
                OpClearScreen opClearScreen = new OpClearScreen();
                assignPaintDomainObj((TDCSOperContent)transferObj, opClearScreen);
                return opClearScreen;
            }else {
                KLog.p(KLog.ERROR, "unknown paint op %s", transferObj);
                return null;
            }
        }
        else{
            KLog.p(KLog.ERROR, "unknown paint op %s", transferObj);
            return null;
        }
    }

//
//
//    public static DCPaintOp toTransferObj(OpPaint domainObj) {
//        switch (domainObj.getType()){
//            case DRAW_LINE:
//                return toTransferObj((OpDrawLine)domainObj);
//            case DRAW_RECT:
//                return toTransferObj((OpDrawRect) domainObj);
//            case DRAW_OVAL:
//                return toTransferObj((OpDrawOval) domainObj);
//            case DRAW_PATH:
//                return toTransferObj((OpDrawPath) domainObj);
//            case UNDO:
//                return toTransferObj((OpUndo) domainObj);
//            case REDO:
//                return toTransferObj((OpRedo) domainObj);
//            case FULLSCREEN_MATRIX:
//                return toTransferObj((OpMatrix) domainObj);
//            case RECT_ERASE:
//                return toTransferObj((OpRectErase) domainObj);
//            case CLEAR_SCREEN:
//                return toTransferObj((OpClearScreen)domainObj);
//            case INSERT_PICTURE:
//                return toTransferObj((OpInsertPic) domainObj);
//            case DELETE_PICTURE:
//                return toTransferObj((OpDeletePic) domainObj);
//            case DRAG_PICTURE:
//                return toTransferObj((OpDragPic) domainObj);
//            case UPDATE_PICTURE:
////                return toTransferObj((OpUpdatePic)domainObj);
//            default:
//                return null;
//        }
//    }
//
//
//

    public static ConcurrentLinkedQueue<PointF> fromTransferObj(TDCSWbPoint[] tdcsWbPoints) {
        ConcurrentLinkedQueue<PointF> pointFS = new ConcurrentLinkedQueue<>();
        for (int i=0; i<tdcsWbPoints.length; ++i){
            pointFS.offer( new PointF(tdcsWbPoints[i].nPosx, tdcsWbPoints[i].nPosy));
        }
        return pointFS;
    }
    public static TDCSWbPoint[] toTransferObj(ConcurrentLinkedQueue<PointF>  pointFS) {
        TDCSWbPoint[] tdcsWbPoints = new TDCSWbPoint[pointFS.size()];
        int i=0;
        for (PointF pointF : pointFS){
            tdcsWbPoints[i++] = new TDCSWbPoint((int)pointF.x, (int)pointF.y);
        }
        return tdcsWbPoints;
    }

    public static OpDrawLine fromTransferObj(DcsOperLineOperInfoNtf dcLineOp) {
        TDCSWbLine tdcsWbLine = dcLineOp.AssParam.tLine;
        OpDrawLine opDrawLine = new OpDrawLine(tdcsWbLine.tBeginPt.nPosx, tdcsWbLine.tBeginPt.nPosy, tdcsWbLine.tEndPt.nPosx, tdcsWbLine.tEndPt.nPosy);
        assignDrawDomainObj(dcLineOp.MainParam, tdcsWbLine.dwLineWidth, (int) tdcsWbLine.dwRgb, opDrawLine);
        return opDrawLine;
    }

    public static TDCSWbLineOperInfo toTransferObj(OpDrawLine domainObj) {
        TDCSWbLine tdcsWbLine = new TDCSWbLine(new TDCSWbEntity(UUID.randomUUID().toString()),
                new TDCSWbPoint((int)domainObj.getStartX(), (int)domainObj.getStartY()),
                new TDCSWbPoint((int)domainObj.getStopX(), (int)domainObj.getStopY()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbLineOperInfo(domainObj.getBoardId(), tdcsWbLine);
    }

    public static OpDrawRect fromTransferObj(DcsOperRectangleOperInfoNtf dcRectOp) {
        TDCSWbRectangle rectangle = dcRectOp.AssParam.tRectangle;
        OpDrawRect opDrawRect = new OpDrawRect(rectangle.tBeginPt.nPosx, rectangle.tBeginPt.nPosy, rectangle.tEndPt.nPosx, rectangle.tEndPt.nPosy);
        assignDrawDomainObj(dcRectOp.MainParam, rectangle.dwLineWidth, (int) rectangle.dwRgb, opDrawRect);
        return opDrawRect;
    }

    public static TDCSWbRectangleOperInfo toTransferObj(OpDrawRect domainObj) {
        TDCSWbRectangle tdcsWbRectangle = new TDCSWbRectangle(new TDCSWbEntity(UUID.randomUUID().toString()),
                new TDCSWbPoint((int)domainObj.getLeft(), (int)domainObj.getTop()),
                new TDCSWbPoint((int)domainObj.getRight(), (int)domainObj.getBottom()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbRectangleOperInfo(domainObj.getBoardId(), tdcsWbRectangle);
    }

    public static OpDrawOval fromTransferObj(DcsOperCircleOperInfoNtf dcOvalOp) {
        TDCSWbCircle circle = dcOvalOp.AssParam.tCircle;
        OpDrawOval opDrawOval = new OpDrawOval(circle.tBeginPt.nPosx, circle.tBeginPt.nPosy, circle.tEndPt.nPosx, circle.tEndPt.nPosy);
        assignDrawDomainObj(dcOvalOp.MainParam, circle.dwLineWidth, (int) circle.dwRgb, opDrawOval);
        return opDrawOval;
    }

    public static TDCSWbCircleOperInfo toTransferObj(OpDrawOval domainObj) {
        TDCSWbCircle tdcsWbCircle = new TDCSWbCircle(new TDCSWbEntity(UUID.randomUUID().toString()),
                new TDCSWbPoint((int)domainObj.getLeft(), (int)domainObj.getTop()),
                new TDCSWbPoint((int)domainObj.getRight(), (int)domainObj.getBottom()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbCircleOperInfo(domainObj.getBoardId(), tdcsWbCircle);
    }

    public static OpDrawPath fromTransferObj(DcsOperPencilOperInfoNtf dcPathOp) {
        TDCSWbPencil pencil = dcPathOp.AssParam.tPencil;
        OpDrawPath opDrawPath = new OpDrawPath(fromTransferObj(pencil.atPList));
        assignDrawDomainObj(dcPathOp.MainParam, pencil.dwLineWidth, (int) pencil.dwRgb, opDrawPath);
        return opDrawPath;
    }

    public static TDCSWbPencilOperInfo toTransferObj(OpDrawPath domainObj) {
        TDCSWbPencil tdcsWbPencil = new TDCSWbPencil(new TDCSWbEntity(UUID.randomUUID().toString()),
                toTransferObj(domainObj.getPoints()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbPencilOperInfo(domainObj.getBoardId(), tdcsWbPencil);
    }

    public static OpInsertPic fromTransferObj(DcsOperInsertPicNtf dcInertPicOp) {
        TDCSWbInsertPicOperInfo ip = dcInertPicOp.AssParam;
        OpInsertPic opInsertPic = new OpInsertPic(ip.achImgId, ip.achPicName, ip.dwImgWidth, ip.dwImgHeight,
                ip.tPoint.nPosx, ip.tPoint.nPosy, matrixValueStr2Float(ip.aachMatrixValue));
        assignPaintDomainObj(dcInertPicOp.MainParam, opInsertPic);
        return opInsertPic;
    }

    public static OpDeletePic fromTransferObj(DcsOperPitchPicDelNtf dcDelPicOp) {
        OpDeletePic opDeletePic = new OpDeletePic(dcDelPicOp.AssParam.achGraphsId);
        assignPaintDomainObj(dcDelPicOp.MainParam, opDeletePic);
        return opDeletePic;
    }

    public static OpDragPic fromTransferObj(DcsOperPitchPicDragNtf dcDragPicOp) {
        Map<String, float[]> picMatrices = new HashMap<>();
        for (TDCSWbGraphsInfo picMatrix : dcDragPicOp.AssParam.atGraphsInfo){
            picMatrices.put(picMatrix.achGraphsId, matrixValueStr2Float(picMatrix.aachMatrixValue));
        }
        OpDragPic opDragPic = new OpDragPic(picMatrices);
        assignPaintDomainObj(dcDragPicOp.MainParam, opDragPic);
        return opDragPic;
    }


    public static OpMatrix fromTransferObj(DcsOperFullScreenNtf dcFullScreenMatrixOp) {
        OpMatrix opMatrix = new OpMatrix(matrixValueStr2Float(dcFullScreenMatrixOp.AssParam.aachMatrixValue));
        assignPaintDomainObj(dcFullScreenMatrixOp.MainParam, opMatrix);
        return opMatrix;
    }

    public static OpRectErase fromTransferObj(DcsOperEraseOperInfoNtf dcRectEraseOp) {
        TDCSWbEraseOperInfo eraseOperInfo = dcRectEraseOp.AssParam;
        OpRectErase opRectErase = new OpRectErase(eraseOperInfo.tBeginPt.nPosx, eraseOperInfo.tBeginPt.nPosy, eraseOperInfo.tEndPt.nPosx, eraseOperInfo.tEndPt.nPosy);
        assignPaintDomainObj(dcRectEraseOp.MainParam, opRectErase);
        return opRectErase;
    }


    public static OpUndo fromTransferObj(DcsOperUndoNtf dcsOperUndoNtf) {
        OpUndo opUndo = new OpUndo();
        assignPaintDomainObj(dcsOperUndoNtf.MainParam, opUndo);
        return opUndo;
    }

    public static OpRedo fromTransferObj(DcsOperRedoNtf dcsOperRedoNtf) {
        OpRedo opRedo = new OpRedo();
        assignPaintDomainObj(dcsOperRedoNtf.MainParam, opRedo);
        return opRedo;
    }


//
//
//
//

//
//    public static DCRectOp toTransferObj(OpDrawRect domainObj) {
//        DCRectOp transferObj = new DCRectOp();
//        assignDrawTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.DRAW_RECT;
//        transferObj.left    = domainObj.getLeft();
//        transferObj.top     = domainObj.getTop();
//        transferObj.right   = domainObj.getRight();
//        transferObj.bottom  = domainObj.getBottom();
//        return transferObj;
//    }
//
//    public static DCOvalOp toTransferObj(OpDrawOval domainObj) {
//        DCOvalOp transferObj = new DCOvalOp();
//        assignDrawTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.DRAW_OVAL;
//        transferObj.left    = domainObj.getLeft();
//        transferObj.top     = domainObj.getTop();
//        transferObj.right   = domainObj.getRight();
//        transferObj.bottom  = domainObj.getBottom();
//        return transferObj;
//    }
//
//    public static DCPathOp toTransferObj(OpDrawPath domainObj) {
//        DCPathOp transferObj = new DCPathOp();
//        assignDrawTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.DRAW_PATH;
//        transferObj.points = domainObj.getPoints();
//        return transferObj;
//    }
//
//    public static DCInertPicOp toTransferObj(OpInsertPic domainObj) {
//        DCInertPicOp transferObj = new DCInertPicOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.INSERT_PIC;
//        transferObj.picId = domainObj.getPicId();
//        transferObj.picName = domainObj.getPicName();
//        transferObj.width = domainObj.getPicWidth();
//        transferObj.height = domainObj.getPicHeight();
//        transferObj.insertPosX = domainObj.getInsertPosX();
//        transferObj.insertPosY = domainObj.getInsertPosY();
//        transferObj.matrixValue = matrixValueFloat2Str(domainObj.getMatrixValue());
//        return transferObj;
//    }
//
//    public static DCDelPicOp toTransferObj(OpDeletePic domainObj) {
//        DCDelPicOp transferObj = new DCDelPicOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.DEL_PIC;
//        transferObj.picIds = domainObj.getPicIds();
//        return transferObj;
//    }
//
//    public static DCDragPicOp toTransferObj(OpDragPic domainObj) {
//        DCDragPicOp transferObj = new DCDragPicOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.DRAG_PIC;
//        ArrayList<DCPicMatrix> list = new ArrayList<>();
//        for (String picId : domainObj.getPicMatrices().keySet()){
//            list.add(new DCPicMatrix(picId, matrixValueFloat2Str(domainObj.getPicMatrices().get(picId))));
//        }
//        transferObj.picMatrices = (DCPicMatrix[]) list.toArray();
//        return transferObj;
//    }
//
//    public static DCFullScreenMatrixOp toTransferObj(OpMatrix domainObj) {
//        DCFullScreenMatrixOp transferObj = new DCFullScreenMatrixOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.FULLSCREEN;
//        transferObj.matrixValue = matrixValueFloat2Str(domainObj.getMatrixValue());
//        return transferObj;
//    }
//
//    public static DCRectEraseOp toTransferObj(OpRectErase domainObj) {
//        DCRectEraseOp transferObj = new DCRectEraseOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.RECT_ERASE;
//        transferObj.left = domainObj.getLeft();
//        transferObj.top = domainObj.getTop();
//        transferObj.right = domainObj.getRight();
//        transferObj.bottom = domainObj.getBottom();
//        return transferObj;
//    }
//
//    public static DCPaintOp toTransferObj(OpClearScreen domainObj) {
//        DCPaintOp transferObj = new DCPaintOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.CLEAR_SCREEN;
//        return transferObj;
//    }
//
//    public static DCPaintOp toTransferObj(OpUndo domainObj) {
//        DCPaintOp transferObj = new DCPaintOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.UNDO;
//        return transferObj;
//    }
//
//    public static DCPaintOp toTransferObj(OpRedo domainObj) {
//        DCPaintOp transferObj = new DCPaintOp();
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.opType = EDcOpType.REDO;
//        return transferObj;
//    }
//
//
//
    public static void assignPaintDomainObj(TDCSOperContent transferObj, OpPaint domainObj){
        domainObj.setConfE164(transferObj.achConfE164);
        domainObj.setBoardId(transferObj.achTabId);
        domainObj.setPageId(transferObj.dwWbPageId);
        domainObj.setSn(transferObj.dwMsgSequence);
    }

    public static void assignDrawDomainObj(TDCSOperContent transferObj, int strokeWidth, int color, OpDraw domainObj){
        assignPaintDomainObj(transferObj, domainObj);
        domainObj.setStrokeWidth(strokeWidth);
        domainObj.setColor(color);
    }
//
//    public static void assignPaintTransferObj(OpPaint domainObj, DCPaintOp transferObj){
//        transferObj.id = "todo";// TODO;
//        transferObj.confE164 = domainObj.getConfE164();
//        transferObj.boardId = domainObj.getBoardId();
//        transferObj.pageId = domainObj.getPageId();
//    }
//
//    public static void assignDrawTransferObj(OpDraw domainObj, DCDrawOp transferObj){
//        assignPaintTransferObj(domainObj, transferObj);
//        transferObj.strokeWidth = domainObj.getStrokeWidth();
//        transferObj.color = domainObj.getColor();
//    }
//
//
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
//
//    public static CreateConfResult fromPaintTransferObj(DCCreateConfResult dcCreateConfResult) {
//        CreateConfResult createConfResult = new CreateConfResult();
//        createConfResult.setConfE164(dcCreateConfResult.confE164);
//        createConfResult.setConfName(dcCreateConfResult.confName);
//        createConfResult.setConfType(fromPaintTransferObj(dcCreateConfResult.confType));
//        createConfResult.setConfMode(fromPaintTransferObj(dcCreateConfResult.confMode));
//        return createConfResult;
//    }
//
//
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

    public static BoardInfo fromTransferObj(TDCSBoardInfo dcBoard) {
        return new BoardInfo(dcBoard.achTabId, dcBoard.achWbName, dcBoard.achWbCreatorE164, dcBoard.dwWbCreateTime,
                fromTransferObj(dcBoard.emWbMode), dcBoard.dwWbPageNum, dcBoard.dwPageId, dcBoard.dwWbAnonyId);
    }

    public static CreateConfResult fromTransferObj(TDCSCreateConfResult to) {
        return new CreateConfResult(to.achConfE164, to.achConfName, fromTransferObj(to.emConfMode), fromTransferObj(to.emConfType));
    }

}
