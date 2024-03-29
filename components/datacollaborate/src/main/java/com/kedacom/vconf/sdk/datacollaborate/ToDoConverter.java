package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Matrix;
import android.graphics.PointF;

import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.math.MatrixHelper;
import com.kedacom.vconf.sdk.datacollaborate.bean.*;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
final class ToDoConverter {
    private static final String INVALID_UUID = "";


    public static OpPaint fromPaintTransferObj(Object transferObj) {
        if (null == transferObj){
            KLog.p(KLog.ERROR, "null == transferObj");
            return null;
        }

        if (transferObj instanceof DcsOperLineOperInfoNtf){ // TODO 用枚举做判断，因为一个结构体可能被多种操作使用
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
            case ZOOM_PICTURE:
                return toZoomPicTransferObj((OpZoomPic) domainObj);
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
                return Msg.DrawLine;
            case DRAW_RECT:
                return Msg.DrawRect;
            case DRAW_OVAL:
                return Msg.DrawOval;
            case DRAW_PATH:
                return Msg.DrawPath;
            case UNDO:
                return Msg.Undo;
            case REDO:
                return Msg.Redo;
            case CLEAR_SCREEN:
                return Msg.ClearScreen;
            case ERASE:
                return Msg.Erase;
            case RECT_ERASE:
                return Msg.RectErase;
            case FULLSCREEN_MATRIX:
                return Msg.Matrix;
            case INSERT_PICTURE:
                return Msg.InsertPic;
            case DELETE_PICTURE:
                return Msg.DelPic;
            case DRAG_PICTURE:
                return Msg.DragPic;
            default:
                return Msg.DrawPath;
        }
    }

    public static List<PointF> fromTransferObj(TDCSWbPoint[] tdcsWbPoints) {
        List<PointF> pointFS = new ArrayList<>();
        if (null != tdcsWbPoints) {
            for (int i = 0; i < tdcsWbPoints.length; ++i) {
                pointFS.add(new PointF(tdcsWbPoints[i].nPosx, tdcsWbPoints[i].nPosy));
            }
        }
        return pointFS;
    }
    public static TDCSWbPoint[] toTransferObj(List<PointF>  pointFS) {
        TDCSWbPoint[] tdcsWbPoints = new TDCSWbPoint[pointFS.size()];
        int i=0;
        for (PointF pointF : pointFS){
            tdcsWbPoints[i++] = new TDCSWbPoint((int)pointF.x, (int)pointF.y);
        }
        return tdcsWbPoints;
    }

    public static OpDrawLine fromTransferObj(DcsOperLineOperInfoNtf dcLineOp) {
        TDCSWbLine tdcsWbLine = dcLineOp.AssParam.tLine;
        OpDrawLine opDrawLine = new OpDrawLine(tdcsWbLine.tBeginPt.nPosx, tdcsWbLine.tBeginPt.nPosy,
                tdcsWbLine.tEndPt.nPosx, tdcsWbLine.tEndPt.nPosy);
        assignDrawDomainObj(dcLineOp.MainParam, dcLineOp.AssParam.tLine.tEntity.achEntityId,
                tdcsWbLine.dwLineWidth, tdcsWbLine.dwRgb, opDrawLine);
        return opDrawLine;
    }

    public static TDCSWbLineOperInfo toTransferObj(OpDrawLine domainObj) {
        TDCSWbLine tdcsWbLine = new TDCSWbLine(new TDCSWbEntity(domainObj.getUuid()),
                new TDCSWbPoint((int)domainObj.getStartX(), (int)domainObj.getStartY()),
                new TDCSWbPoint((int)domainObj.getStopX(), (int)domainObj.getStopY()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbLineOperInfo(domainObj.getBoardId(), tdcsWbLine);
    }

    public static OpDrawRect fromTransferObj(DcsOperRectangleOperInfoNtf dcRectOp) {
        TDCSWbRectangle rectangle = dcRectOp.AssParam.tRectangle;
        OpDrawRect opDrawRect = new OpDrawRect(rectangle.tBeginPt.nPosx, rectangle.tBeginPt.nPosy,
                rectangle.tEndPt.nPosx, rectangle.tEndPt.nPosy);
        assignDrawDomainObj(dcRectOp.MainParam, dcRectOp.AssParam.tRectangle.tEntity.achEntityId,
                rectangle.dwLineWidth, rectangle.dwRgb, opDrawRect);
        return opDrawRect;
    }

    public static TDCSWbRectangleOperInfo toTransferObj(OpDrawRect domainObj) {
        TDCSWbRectangle tdcsWbRectangle = new TDCSWbRectangle(new TDCSWbEntity(domainObj.getUuid()),
                new TDCSWbPoint((int)domainObj.getLeft(), (int)domainObj.getTop()),
                new TDCSWbPoint((int)domainObj.getRight(), (int)domainObj.getBottom()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbRectangleOperInfo(domainObj.getBoardId(), tdcsWbRectangle);
    }

    public static OpDrawOval fromTransferObj(DcsOperCircleOperInfoNtf dcOvalOp) {
        TDCSWbCircle circle = dcOvalOp.AssParam.tCircle;
        OpDrawOval opDrawOval = new OpDrawOval(circle.tBeginPt.nPosx, circle.tBeginPt.nPosy,
                circle.tEndPt.nPosx, circle.tEndPt.nPosy);
        assignDrawDomainObj(dcOvalOp.MainParam, dcOvalOp.AssParam.tCircle.tEntity.achEntityId,
                circle.dwLineWidth, circle.dwRgb, opDrawOval);
        return opDrawOval;
    }

    public static TDCSWbCircleOperInfo toTransferObj(OpDrawOval domainObj) {
        TDCSWbCircle tdcsWbCircle = new TDCSWbCircle(new TDCSWbEntity(domainObj.getUuid()),
                new TDCSWbPoint((int)domainObj.getLeft(), (int)domainObj.getTop()),
                new TDCSWbPoint((int)domainObj.getRight(), (int)domainObj.getBottom()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbCircleOperInfo(domainObj.getBoardId(), tdcsWbCircle);
    }

    public static OpDrawPath fromTransferObj(DcsOperPencilOperInfoNtf dcPathOp) {
        TDCSWbPencil pencil = dcPathOp.AssParam.tPencil;
        OpDrawPath opDrawPath = new OpDrawPath(fromTransferObj(pencil.atPList));
        assignDrawDomainObj(dcPathOp.MainParam, dcPathOp.AssParam.tPencil.tEntity.achEntityId,
                pencil.dwLineWidth, pencil.dwRgb, opDrawPath);
        opDrawPath.setFinished(dcPathOp.AssParam.bFinished);
        return opDrawPath;
    }

    public static TDCSWbPencilOperInfo toTransferObj(OpDrawPath domainObj) {
        TDCSWbPencil tdcsWbPencil = new TDCSWbPencil(new TDCSWbEntity(domainObj.getUuid()),
                toTransferObj(domainObj.getPoints()),
                domainObj.getStrokeWidth(), domainObj.getColor());
        return new TDCSWbPencilOperInfo(domainObj.getBoardId(), 0, tdcsWbPencil, domainObj.isFinished());
    }


    public static OpInsertPic fromTransferObj(DcsOperInsertPicNtf dcInertPicOp) {
        TDCSWbInsertPicOperInfo ip = dcInertPicOp.AssParam;
        float[] matrixVal = MatrixHelper.valStr2Float(ip.aachMatrixValue);
        Matrix matrix = new Matrix();
        matrix.setValues(matrixVal);
        OpInsertPic opInsertPic = new OpInsertPic(ip.achImgId, ip.achPicName,
                new PointF(ip.tPoint.nPosx, ip.tPoint.nPosy), ip.dwImgWidth, ip.dwImgHeight, matrix);
        assignPaintDomainObj(dcInertPicOp.MainParam, INVALID_UUID, opInsertPic);
        return opInsertPic;
    }

    public static TDCSWbInsertPicOperInfo toTransferObj(OpInsertPic domainObj) {
        float[] matrixVal = new float[9];
        domainObj.getTransMatrix().getValues(matrixVal);
        return new TDCSWbInsertPicOperInfo(domainObj.getBoardId(), domainObj.getPageId(), domainObj.getPicId(),
                domainObj.getPicWidth(), domainObj.getPicHeight(),
                new TDCSWbPoint((int)domainObj.getInsertPos().x, (int)domainObj.getInsertPos().y),
                domainObj.getPicName(), MatrixHelper.valFloat2Str(matrixVal));
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
        Map<String, Matrix> picMatrices = new HashMap<>();
        for (TDCSWbGraphsInfo picMatrix : dcDragPicOp.AssParam.atGraphsInfo){
            float[] matrixVal = MatrixHelper.valStr2Float(picMatrix.aachMatrixValue);
            Matrix matrix = new Matrix();
            matrix.setValues(matrixVal);
            picMatrices.put(picMatrix.achGraphsId, matrix);
        }
        OpDragPic opDragPic = new OpDragPic(picMatrices);
        assignPaintDomainObj(dcDragPicOp.MainParam, INVALID_UUID, opDragPic);
        return opDragPic;
    }

    public static TDCSWbPitchPicOperInfo toTransferObj(OpDragPic domainObj) {
        Map<String, Matrix> matrices = domainObj.getPicMatrices();
        TDCSWbGraphsInfo[] tdcsWbGraphsInfos = new TDCSWbGraphsInfo[matrices.size()];
        int i=0;
        for (String picId : matrices.keySet()){
            float[] matrixVal = new float[9];
            matrices.get(picId).getValues(matrixVal);
            tdcsWbGraphsInfos[i] = new TDCSWbGraphsInfo(picId, MatrixHelper.valFloat2Str(matrixVal));
            ++i;
        }
        return new TDCSWbPitchPicOperInfo(domainObj.getBoardId(), domainObj.getPageId(), tdcsWbGraphsInfos);
    }

    public static OpZoomPic fromZoomPicTransferObj(DcsOperPitchPicDragNtf dcDragPicOp) {
        Map<String, Matrix> picMatrices = new HashMap<>();
        for (TDCSWbGraphsInfo picMatrix : dcDragPicOp.AssParam.atGraphsInfo){
            float[] matrixVal = MatrixHelper.valStr2Float(picMatrix.aachMatrixValue);
            Matrix matrix = new Matrix();
            matrix.setValues(matrixVal);
            picMatrices.put(picMatrix.achGraphsId, matrix);
        }
        OpZoomPic opZoomPic = new OpZoomPic(picMatrices);
        assignPaintDomainObj(dcDragPicOp.MainParam, INVALID_UUID, opZoomPic);
        return opZoomPic;
    }

    public static TDCSWbPitchPicOperInfo toZoomPicTransferObj(OpZoomPic domainObj) {
        Map<String, Matrix> matrices = domainObj.getPicMatrices();
        TDCSWbGraphsInfo[] tdcsWbGraphsInfos = new TDCSWbGraphsInfo[matrices.size()];
        int i=0;
        for (String picId : matrices.keySet()){
            float[] matrixVal = new float[9];
            matrices.get(picId).getValues(matrixVal);
            tdcsWbGraphsInfos[i] = new TDCSWbGraphsInfo(picId, MatrixHelper.valFloat2Str(matrixVal));
            ++i;
        }
        return new TDCSWbPitchPicOperInfo(domainObj.getBoardId(), domainObj.getPageId(), tdcsWbGraphsInfos);
    }


    public static OpMatrix fromTransferObj(DcsOperFullScreenNtf dcFullScreenMatrixOp) {
        float[] matrixVal = MatrixHelper.valStr2Float(dcFullScreenMatrixOp.AssParam.aachMatrixValue);
        OpMatrix opMatrix = new OpMatrix(matrixVal);
        assignPaintDomainObj(dcFullScreenMatrixOp.MainParam, INVALID_UUID, opMatrix);
        return opMatrix;
    }

    public static TDCSWbDisPlayInfo toTransferObj(OpMatrix domainObj) {
        float[] matrixVal = domainObj.getMatrixValue();
        return new TDCSWbDisPlayInfo(domainObj.getBoardId(), domainObj.getPageId(), MatrixHelper.valFloat2Str(matrixVal));
    }


    public static OpRectErase fromTransferObj(DcsOperEraseOperInfoNtf dcRectEraseOp) {
        TDCSWbEraseOperInfo eraseOperInfo = dcRectEraseOp.AssParam;
        OpRectErase opRectErase = new OpRectErase(eraseOperInfo.tBeginPt.nPosx, eraseOperInfo.tBeginPt.nPosy,
                eraseOperInfo.tEndPt.nPosx, eraseOperInfo.tEndPt.nPosy);
        assignPaintDomainObj(dcRectEraseOp.MainParam, INVALID_UUID, opRectErase);
        return opRectErase;
    }

    public static TDCSWbEraseOperInfo toTransferObj(OpRectErase domainObj) {
        return new TDCSWbEraseOperInfo(domainObj.getBoardId(),
                new TDCSWbPoint((int)domainObj.getLeft(), (int)domainObj.getTop()),
                new TDCSWbPoint((int)domainObj.getRight(), (int)domainObj.getBottom()));
    }

    public static OpErase fromTransferObj(DcsOperReginEraseNtf dcErase) {
        TDCSWbReginEraseOperInfo opInfo = dcErase.AssParam;
        OpErase opErase = new OpErase(opInfo.dwEraseWidth, opInfo.dwEraseHeight, fromTransferObj(opInfo.atPoint));
        assignPaintDomainObj(dcErase.MainParam, INVALID_UUID, opErase);
        return opErase;
    }

    public static TDCSWbReginEraseOperInfo toTransferObj(OpErase domainObj) {
        return new TDCSWbReginEraseOperInfo(domainObj.getBoardId(), domainObj.getPageId(),
                domainObj.getWidth(), domainObj.getHeight(), toTransferObj(domainObj.getPoints()));
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


    public static void assignPaintDomainObj(TDCSOperContent transferObj, String uuid, OpPaint domainObj){
        domainObj.setUuid(uuid);
        domainObj.setAuthorE164(transferObj.achFromE164);
        domainObj.setConfE164(transferObj.achConfE164);
        domainObj.setBoardId(transferObj.achTabId);
        domainObj.setPageId(transferObj.dwWbPageId);
        domainObj.setSn(transferObj.dwMsgSequence);
    }

    public static void assignDrawDomainObj(TDCSOperContent transferObj, String uuid, int strokeWidth, long color, OpDraw domainObj){
        assignPaintDomainObj(transferObj, uuid, domainObj);
        domainObj.setStrokeWidth(strokeWidth);
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

    public static BoardInfo fromTransferObj(TDCSBoardInfo dcBoard, String confE164) {
        return new BoardInfo(dcBoard.achTabId, dcBoard.achWbName, confE164, dcBoard.achWbCreatorE164, dcBoard.dwWbCreateTime,
                fromTransferObj(dcBoard.emWbMode), dcBoard.dwWbPageNum, dcBoard.dwPageId, dcBoard.dwWbAnonyId, dcBoard.achElementUrl);
    }

    public static DcConfInfo fromTransferObj(TDCSCreateConfResult to) {
        return new DcConfInfo(to.achConfE164, to.achConfName, fromTransferObj(to.emConfMode), fromTransferObj(to.emConfType), to.bCreator);
    }

    public static TDCSCreateConfResult toTDCSCreateConfResult(DcConfInfo confInfo) {
        return new TDCSCreateConfResult(confInfo.getConfE164(), confInfo.getConfName(),true, 0,
                toTransferObj(confInfo.getConfMode()), toTransferObj(confInfo.getConfType()), true);
    }

    public static DcConfInfo fromTransferObj(TDCSConfInfo to) {
        return new DcConfInfo(to.achConfE164, to.achConfName, fromTransferObj(to.emConfMode), fromTransferObj(to.emConfType), false);
    }

    public static TDCSConfInfo toTransferObj(DcConfInfo confInfo){
        return new TDCSConfInfo(confInfo.getConfE164(), confInfo.getConfName(), toTransferObj(confInfo.getConfMode()), toTransferObj(confInfo.getConfType()));
    }

    public static DcConfInfo fromTransferObj(DcsSetConfInfoRsp to) {
        return new DcConfInfo(to.achConfE164, to.achConfName, fromTransferObj(to.emConfMode), fromTransferObj(to.emConfType), false);
    }

    public static DCMember fromTransferObj(TDCSConfUserInfo userInfo){
        return new DCMember(userInfo.achE164, userInfo.achName, fromTransferObj(userInfo.emMttype), userInfo.bIsOper, userInfo.bIsConfAdmin, userInfo.bOnline);
    }

    public static TDCSConfUserInfo toTransferObj(DCMember member){
        return new TDCSConfUserInfo(member.getE164(), member.getName(), toTransferObj(member.getType()),
                member.isbOnline(), member.isbOperator(), member.isbChairman());
    }

    public static List<DCMember> fromDcUserList(List<TDCSConfUserInfo> userInfos){
        List<DCMember> members = new ArrayList<>();
        if (null != userInfos) {
            for (TDCSConfUserInfo userInfo : userInfos) {
                members.add(fromTransferObj(userInfo));
            }
        }
        return members;
    }

    public static List<TDCSConfUserInfo> toDcUserList(List<DCMember> members){
        List<TDCSConfUserInfo> userInfos = new ArrayList<>();
        if (null != members) {
            for (DCMember member : members) {
                userInfos.add(toTransferObj(member));
            }
        }
        return userInfos;
    }

}
