package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Matrix;
import android.graphics.PointF;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.base.Msg;
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
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperReginEraseNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperUndoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsConfMode;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsConfType;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsOper;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsType;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsWbMode;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConfUserInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConfResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperContent;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperReq;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbCircle;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbCircleOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbDelPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbDisPlayInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbEntity;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbEraseOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbGraphsInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbInsertPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbLine;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbLineOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPencil;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPencilOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPitchPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPoint;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbRectangle;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbRectangleOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbReginEraseOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbTabPageIdInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.CreateConfResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.EBoardMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EDcMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EConfType;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
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

@SuppressWarnings("WeakerAccess")
final class ToDoConverter {
    private static DataCollaborateManager dcMan = DataCollaborateManager.getInstance(null);
    private static float density = DataCollaborateManager.getContext().getResources().getDisplayMetrics().density;
    private static final String INVALID_UUID = "";

    /**
     * 将坐标值由本设备实际使用的转为传输的
     * */
    private static float tt(float val){
        float t = val/density;
        if (-1<t && t<0){
            return -1;
        }else if (0<t && t<1){
            return 1;
        }
        return t;
    }

    /**
     * 将坐标值由传输过来的转为在本设备上实际使用的
     * */
    private static float ft(float val){
        return val*density;
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
            strMatrixValue[i] = Float.toString(matrixValue[i]);
        }
        return strMatrixValue;
    }

    private static void picMatrixValueFt(float[] matrixVal){
        // 位移适配屏幕密度
        matrixVal[Matrix.MTRANS_X] = ft(matrixVal[Matrix.MTRANS_X]);
        matrixVal[Matrix.MTRANS_Y] = ft(matrixVal[Matrix.MTRANS_Y]);

        // 图片根据屏幕密度放缩
        matrixVal[Matrix.MSCALE_X] = ft(matrixVal[Matrix.MSCALE_X]);
        matrixVal[Matrix.MSCALE_Y] = ft(matrixVal[Matrix.MSCALE_Y]);
    }

    private static void picMatrixValueTt(float[] matrixVal){
        // 位移适配屏幕密度
        matrixVal[Matrix.MTRANS_X] = tt(matrixVal[Matrix.MTRANS_X]);
        matrixVal[Matrix.MTRANS_Y] = tt(matrixVal[Matrix.MTRANS_Y]);
        // 图片根据屏幕密度放缩
        matrixVal[Matrix.MSCALE_X] = tt(matrixVal[Matrix.MSCALE_X]);
        matrixVal[Matrix.MSCALE_Y] = tt(matrixVal[Matrix.MSCALE_Y]);

    }

    public static OpPaint fromPaintTransferObj(Object transferObj) {
        if (transferObj instanceof DcsOperLineOperInfoNtf){ // TODO 用枚举做判断
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
        }else if (transferObj instanceof DcsOperReginEraseNtf){
            return fromTransferObj((DcsOperReginEraseNtf)transferObj);
        }else if (transferObj instanceof DcsOperEraseOperInfoNtf){
            return fromTransferObj((DcsOperEraseOperInfoNtf)transferObj);
        }else if (transferObj instanceof DcsOperUndoNtf){
            return fromTransferObj((DcsOperUndoNtf)transferObj);
        }else if (transferObj instanceof DcsOperRedoNtf){
            return fromTransferObj((DcsOperRedoNtf)transferObj);
        }else if (transferObj instanceof TDCSOperContent){
            if (((TDCSOperContent)transferObj).emOper.equals(EmDcsOper.emWbClearScreen)){
                OpClearScreen opClearScreen = new OpClearScreen();
                assignPaintDomainObj((TDCSOperContent)transferObj, INVALID_UUID, opClearScreen);
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



    public static Object toPaintTransferObj(OpPaint domainObj) {
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
            case ERASE:
                return toTransferObj((OpErase) domainObj);
            case RECT_ERASE:
                return toTransferObj((OpRectErase) domainObj);
            case INSERT_PICTURE:
                return toTransferObj((OpInsertPic) domainObj);
            case DELETE_PICTURE:
                return toTransferObj((OpDeletePic) domainObj);
            case DRAG_PICTURE:
                return toTransferObj((OpDragPic) domainObj);
            case CLEAR_SCREEN:
            default:
                return null;
        }
    }


    public static TDCSOperReq toCommonPaintTransferObj(OpPaint domainObj) {
        return new TDCSOperReq(domainObj.getConfE164(), domainObj.getBoardId(), domainObj.getPageId());
    }


    public static Msg opTypeToReqMsg(EOpType type){
        switch (type){
            case DRAW_LINE:
                return Msg.DCDrawLine;
            case DRAW_RECT:
                return Msg.DCDrawRect;
            case DRAW_OVAL:
                return Msg.DCDrawOval;
            case DRAW_PATH:
                return Msg.DCDrawPath;
            case UNDO:
                return Msg.DCUndo;
            case REDO:
                return Msg.DCRedo;
            case CLEAR_SCREEN:
                return Msg.DCClearScreen;
            case ERASE:
                return Msg.DCErase;
            case RECT_ERASE:
                return Msg.DCRectErase;
            case FULLSCREEN_MATRIX:
                return Msg.DCMatrix;
            case INSERT_PICTURE:
                return Msg.DCInsertPic;
            case DELETE_PICTURE:
                return Msg.DCDeletePic;
            case DRAG_PICTURE:
                return Msg.DCDragPic;
            default:
                return null;
        }
    }

    public static List<PointF> fromTransferObj(TDCSWbPoint[] tdcsWbPoints) {
        List<PointF> pointFS = new ArrayList<>();
        for (int i=0; i<tdcsWbPoints.length; ++i){
            pointFS.add( new PointF(ft(tdcsWbPoints[i].nPosx), ft(tdcsWbPoints[i].nPosy)));
        }
        return pointFS;
    }
    public static TDCSWbPoint[] toTransferObj(List<PointF>  pointFS) {
        TDCSWbPoint[] tdcsWbPoints = new TDCSWbPoint[pointFS.size()];
        int i=0;
        for (PointF pointF : pointFS){
            tdcsWbPoints[i++] = new TDCSWbPoint((int)tt(pointF.x), (int)tt(pointF.y));
        }
        return tdcsWbPoints;
    }

    public static OpDrawLine fromTransferObj(DcsOperLineOperInfoNtf dcLineOp) {
        TDCSWbLine tdcsWbLine = dcLineOp.AssParam.tLine;
        OpDrawLine opDrawLine = new OpDrawLine(ft(tdcsWbLine.tBeginPt.nPosx), ft(tdcsWbLine.tBeginPt.nPosy),
                ft(tdcsWbLine.tEndPt.nPosx), ft(tdcsWbLine.tEndPt.nPosy));
        assignDrawDomainObj(dcLineOp.MainParam, dcLineOp.AssParam.tLine.tEntity.achEntityId,
                tdcsWbLine.dwLineWidth, tdcsWbLine.dwRgb, opDrawLine);
        return opDrawLine;
    }

    public static TDCSWbLineOperInfo toTransferObj(OpDrawLine domainObj) {
        TDCSWbLine tdcsWbLine = new TDCSWbLine(new TDCSWbEntity(domainObj.getUuid()),
                new TDCSWbPoint((int)tt(domainObj.getStartX()), (int)tt(domainObj.getStartY())),
                new TDCSWbPoint((int)tt(domainObj.getStopX()), (int)tt(domainObj.getStopY())),
                (int) tt(domainObj.getStrokeWidth()), domainObj.getColor());
        return new TDCSWbLineOperInfo(domainObj.getBoardId(), tdcsWbLine);
    }

    public static OpDrawRect fromTransferObj(DcsOperRectangleOperInfoNtf dcRectOp) {
        TDCSWbRectangle rectangle = dcRectOp.AssParam.tRectangle;
        OpDrawRect opDrawRect = new OpDrawRect(ft(rectangle.tBeginPt.nPosx), ft(rectangle.tBeginPt.nPosy),
                ft(rectangle.tEndPt.nPosx), ft(rectangle.tEndPt.nPosy));
        assignDrawDomainObj(dcRectOp.MainParam, dcRectOp.AssParam.tRectangle.tEntity.achEntityId,
                rectangle.dwLineWidth, rectangle.dwRgb, opDrawRect);
        return opDrawRect;
    }

    public static TDCSWbRectangleOperInfo toTransferObj(OpDrawRect domainObj) {
        TDCSWbRectangle tdcsWbRectangle = new TDCSWbRectangle(new TDCSWbEntity(domainObj.getUuid()),
                new TDCSWbPoint((int)tt(domainObj.getLeft()), (int)tt(domainObj.getTop())),
                new TDCSWbPoint((int)tt(domainObj.getRight()), (int)tt(domainObj.getBottom())),
                (int) tt(domainObj.getStrokeWidth()), domainObj.getColor());
        return new TDCSWbRectangleOperInfo(domainObj.getBoardId(), tdcsWbRectangle);
    }

    public static OpDrawOval fromTransferObj(DcsOperCircleOperInfoNtf dcOvalOp) {
        TDCSWbCircle circle = dcOvalOp.AssParam.tCircle;
        OpDrawOval opDrawOval = new OpDrawOval(ft(circle.tBeginPt.nPosx), ft(circle.tBeginPt.nPosy),
                ft(circle.tEndPt.nPosx), ft(circle.tEndPt.nPosy));
        assignDrawDomainObj(dcOvalOp.MainParam, dcOvalOp.AssParam.tCircle.tEntity.achEntityId,
                circle.dwLineWidth, circle.dwRgb, opDrawOval);
        return opDrawOval;
    }

    public static TDCSWbCircleOperInfo toTransferObj(OpDrawOval domainObj) {
        TDCSWbCircle tdcsWbCircle = new TDCSWbCircle(new TDCSWbEntity(domainObj.getUuid()),
                new TDCSWbPoint((int)tt(domainObj.getLeft()), (int)tt(domainObj.getTop())),
                new TDCSWbPoint((int)tt(domainObj.getRight()), (int)tt(domainObj.getBottom())),
                (int) tt(domainObj.getStrokeWidth()), domainObj.getColor());
        return new TDCSWbCircleOperInfo(domainObj.getBoardId(), tdcsWbCircle);
    }

    public static OpDrawPath fromTransferObj(DcsOperPencilOperInfoNtf dcPathOp) {
        TDCSWbPencil pencil = dcPathOp.AssParam.tPencil;
        OpDrawPath opDrawPath = new OpDrawPath(fromTransferObj(pencil.atPList));
        assignDrawDomainObj(dcPathOp.MainParam, dcPathOp.AssParam.tPencil.tEntity.achEntityId,
                pencil.dwLineWidth, pencil.dwRgb, opDrawPath);
        return opDrawPath;
    }

    public static TDCSWbPencilOperInfo toTransferObj(OpDrawPath domainObj) {
        TDCSWbPencil tdcsWbPencil = new TDCSWbPencil(new TDCSWbEntity(domainObj.getUuid()),
                toTransferObj(domainObj.getPoints()),
                (int) tt(domainObj.getStrokeWidth()), domainObj.getColor());
        return new TDCSWbPencilOperInfo(domainObj.getBoardId(), tdcsWbPencil);
    }

    public static OpInsertPic fromTransferObj(DcsOperInsertPicNtf dcInertPicOp) {
        TDCSWbInsertPicOperInfo ip = dcInertPicOp.AssParam;
        float[] matrixVal = matrixValueStr2Float(ip.aachMatrixValue);
//        matrixVal[Matrix.MTRANS_X] += ip.tPoint.nPosx;  // TODO 让windows改
//        matrixVal[Matrix.MTRANS_Y] += ip.tPoint.nPosy;
        picMatrixValueFt(matrixVal);
        Matrix matrix = new Matrix();
        matrix.setValues(matrixVal);
        OpInsertPic opInsertPic = new OpInsertPic(ip.achImgId, ip.achPicName, ip.dwImgWidth, ip.dwImgHeight, matrix);
        assignPaintDomainObj(dcInertPicOp.MainParam, INVALID_UUID, opInsertPic);
        return opInsertPic;
    }

    public static TDCSWbInsertPicOperInfo toTransferObj(OpInsertPic domainObj) {
        float[] matrixVal = domainObj.getMatrixValue();
        picMatrixValueTt(matrixVal);
        return new TDCSWbInsertPicOperInfo(domainObj.getBoardId(), domainObj.getPageId(), domainObj.getPicId(),
                domainObj.getPicWidth(), domainObj.getPicHeight(),
                new TDCSWbPoint(0, 0),
                domainObj.getPicName(), matrixValueFloat2Str(matrixVal));
    }

    public static OpDeletePic fromTransferObj(DcsOperPitchPicDelNtf dcDelPicOp) {
        OpDeletePic opDeletePic = new OpDeletePic(dcDelPicOp.AssParam.achGraphsId);
        assignPaintDomainObj(dcDelPicOp.MainParam, INVALID_UUID, opDeletePic);
        return opDeletePic;
    }

    public static TDCSWbDelPicOperInfo toTransferObj(OpDeletePic domainObj) {
        return new TDCSWbDelPicOperInfo(domainObj.getBoardId(), domainObj.getPageId(), domainObj.getPicIds());
    }

    public static OpDragPic fromTransferObj(DcsOperPitchPicDragNtf dcDragPicOp) {
        Map<String, float[]> picMatrices = new HashMap<>();
        for (TDCSWbGraphsInfo picMatrix : dcDragPicOp.AssParam.atGraphsInfo){
            float[] matrixVal = matrixValueStr2Float(picMatrix.aachMatrixValue);
            picMatrixValueFt(matrixVal);
            picMatrices.put(picMatrix.achGraphsId, matrixVal);
        }
        OpDragPic opDragPic = new OpDragPic(picMatrices);
        assignPaintDomainObj(dcDragPicOp.MainParam, INVALID_UUID, opDragPic);
        return opDragPic;
    }

    public static TDCSWbPitchPicOperInfo toTransferObj(OpDragPic domainObj) {
        Map<String, float[]> matrices = domainObj.getPicMatrices();
        TDCSWbGraphsInfo[] tdcsWbGraphsInfos = new TDCSWbGraphsInfo[matrices.size()];
        int i=0;
        for (String picId : matrices.keySet()){
            float[] matrixVal = matrices.get(picId);
            picMatrixValueTt(matrixVal);
            tdcsWbGraphsInfos[i] = new TDCSWbGraphsInfo(picId, matrixValueFloat2Str(matrixVal));
            ++i;
        }
        return new TDCSWbPitchPicOperInfo(domainObj.getBoardId(), domainObj.getPageId(), tdcsWbGraphsInfos);
    }


    public static OpMatrix fromTransferObj(DcsOperFullScreenNtf dcFullScreenMatrixOp) {
        float[] matrixVal = matrixValueStr2Float(dcFullScreenMatrixOp.AssParam.aachMatrixValue);
        // 位移适配屏幕密度
        matrixVal[Matrix.MTRANS_X] = ft(matrixVal[Matrix.MTRANS_X]);
        matrixVal[Matrix.MTRANS_Y] = ft(matrixVal[Matrix.MTRANS_Y]);

        OpMatrix opMatrix = new OpMatrix(matrixVal);
        assignPaintDomainObj(dcFullScreenMatrixOp.MainParam, INVALID_UUID, opMatrix);
        return opMatrix;
    }

    public static TDCSWbDisPlayInfo toTransferObj(OpMatrix domainObj) {
        float[] matrixVal = domainObj.getMatrixValue();
        // 位移适配屏幕密度
        matrixVal[Matrix.MTRANS_X] = tt(matrixVal[Matrix.MTRANS_X]);
        matrixVal[Matrix.MTRANS_Y] = tt(matrixVal[Matrix.MTRANS_Y]);
        return new TDCSWbDisPlayInfo(domainObj.getBoardId(), domainObj.getPageId(), matrixValueFloat2Str(matrixVal));
    }


    public static OpRectErase fromTransferObj(DcsOperEraseOperInfoNtf dcRectEraseOp) {
        TDCSWbEraseOperInfo eraseOperInfo = dcRectEraseOp.AssParam;
        OpRectErase opRectErase = new OpRectErase(ft(eraseOperInfo.tBeginPt.nPosx), ft(eraseOperInfo.tBeginPt.nPosy),
                ft(eraseOperInfo.tEndPt.nPosx), ft(eraseOperInfo.tEndPt.nPosy));
        assignPaintDomainObj(dcRectEraseOp.MainParam, INVALID_UUID, opRectErase);
        return opRectErase;
    }

    public static TDCSWbEraseOperInfo toTransferObj(OpRectErase domainObj) {
        return new TDCSWbEraseOperInfo(domainObj.getBoardId(),
                new TDCSWbPoint((int)tt(domainObj.getLeft()), (int)tt(domainObj.getTop())),
                new TDCSWbPoint((int)tt(domainObj.getRight()), (int)tt(domainObj.getBottom())));
    }

    public static OpErase fromTransferObj(DcsOperReginEraseNtf dcErase) {
        TDCSWbReginEraseOperInfo opInfo = dcErase.AssParam;
        OpErase opErase = new OpErase((int)ft(opInfo.dwEraseWidth), (int)ft(opInfo.dwEraseHeight), fromTransferObj(opInfo.atPoint));
        assignPaintDomainObj(dcErase.MainParam, INVALID_UUID, opErase);
        return opErase;
    }

    public static TDCSWbReginEraseOperInfo toTransferObj(OpErase domainObj) {
        return new TDCSWbReginEraseOperInfo(domainObj.getBoardId(), domainObj.getPageId(),
                (int)tt(domainObj.getWidth()), (int)tt(domainObj.getHeight()), toTransferObj(domainObj.getPoints()));
    }


    public static OpUndo fromTransferObj(DcsOperUndoNtf dcsOperUndoNtf) {
        OpUndo opUndo = new OpUndo();
        assignPaintDomainObj(dcsOperUndoNtf.MainParam, INVALID_UUID, opUndo);
        return opUndo;
    }

    public static TDCSWbTabPageIdInfo toTransferObj(OpUndo domainObj) {
        return new TDCSWbTabPageIdInfo(domainObj.getBoardId(), domainObj.getPageId());
    }

    public static OpRedo fromTransferObj(DcsOperRedoNtf dcsOperRedoNtf) {
        OpRedo opRedo = new OpRedo();
        assignPaintDomainObj(dcsOperRedoNtf.MainParam, INVALID_UUID, opRedo);
        return opRedo;
    }

    public static TDCSWbTabPageIdInfo toTransferObj(OpRedo domainObj) {
        return new TDCSWbTabPageIdInfo(domainObj.getBoardId(), domainObj.getPageId());
    }


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

    public static void assignPaintDomainObj(TDCSOperContent transferObj, String uuid, OpPaint domainObj){
        domainObj.setUuid(uuid);
        domainObj.setConfE164(transferObj.achConfE164);
        domainObj.setBoardId(transferObj.achTabId);
        domainObj.setPageId(transferObj.dwWbPageId);
        domainObj.setSn(transferObj.dwMsgSequence);
    }

    public static void assignDrawDomainObj(TDCSOperContent transferObj, String uuid, int strokeWidth, long color, OpDraw domainObj){
        assignPaintDomainObj(transferObj, uuid, domainObj);
        domainObj.setStrokeWidth((int) ft(strokeWidth));
        domainObj.setColor(color);
    }

    public static ETerminalType fromTransferObj(EmDcsType type){
        switch (type){
            case emTypeTrueLink:
                return ETerminalType.TrueLinkWindows;
            case emTypeTrueTouchPhoneIOS:
                return ETerminalType.TrueLinkIosPhone;
            case emTypeTrueTouchPadIOS:
                return ETerminalType.TrueLinkIosPad;
            case emTypeTrueTouchPhoneAndroid:
                return ETerminalType.TrueLinkAndroidPhone;
            case emTypeTrueTouchPadAndroid:
                return ETerminalType.TrueLinkAndroidPad;
            case emTypeTrueSens:
                return ETerminalType.TrueSens;
            case emTypeIMIX:
                return ETerminalType.Imix;
            case emTypeThirdPartyTer:
                return ETerminalType.ThirdParty;
            case emTypeUnknown:
            default:
                return ETerminalType.Unknown;
        }
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

    public static EmDcsConfType toTransferObj(EConfType type){
        switch (type){
            case P2P:
                return EmDcsConfType.emConfTypeP2P;
            case MCC:
                return EmDcsConfType.emConfTypeMCC;
            default:
                return EmDcsConfType.emConfTypeP2P;
        }
    }

    public static EDcMode fromTransferObj(EmDcsConfMode dcsConfModed) {
        switch (dcsConfModed){
            case emConfModeAuto:
                return EDcMode.Auto;
            case emConfModeManage:
                return EDcMode.Manage;
            case emConfModeStop:
                return EDcMode.Stop;
            default:
                return EDcMode.Auto;
        }
    }

    public static EmDcsConfMode toTransferObj(EDcMode mode){
        switch (mode){
            case Stop:
                return EmDcsConfMode.emConfModeStop;
            case Manage:
                return EmDcsConfMode.emConfModeManage;
            case Auto:
                return EmDcsConfMode.emConfModeAuto;
            default:
                return EmDcsConfMode.emConfModeAuto;
        }
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

    public static BoardInfo fromTransferObj(TDCSBoardInfo dcBoard) {
        return new BoardInfo(dcBoard.achTabId, dcBoard.achWbName, dcMan.getCurDcConfE164(), dcBoard.achWbCreatorE164, dcBoard.dwWbCreateTime,
                fromTransferObj(dcBoard.emWbMode), dcBoard.dwWbPageNum, dcBoard.dwPageId, dcBoard.dwWbAnonyId);
    }

    public static CreateConfResult fromTransferObj(TDCSCreateConfResult to) {
        return new CreateConfResult(to.achConfE164, to.achConfName, fromTransferObj(to.emConfMode), fromTransferObj(to.emConfType));
    }

    public static DCMember fromTransferObj(TDCSConfUserInfo userInfo){
        return new DCMember(userInfo.achE164, userInfo.achName, fromTransferObj(userInfo.emMttype), true, false, true);
    }

    public static TDCSConfUserInfo toTransferObj(DCMember member){
        return new TDCSConfUserInfo(member.getE164(), member.getName(), toTransferObj(member.getType()),
                member.isbOnline(), member.isbOperator(), member.isbChairman());
    }

}
