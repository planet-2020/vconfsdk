package com.kedacom.vconf.sdk.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

/**
 * Created by Sissi on 2018/9/3.
 */

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.kedacom.vconf.sdk.annotation.Message",
        "com.kedacom.vconf.sdk.annotation.Request",
        "com.kedacom.vconf.sdk.annotation.Response",
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MessageProcessor extends AbstractProcessor {
    private boolean bDone = false;

    private String module;
    private BiMap<String, String> nameIdMap = HashBiMap.create();
    private Table<String, String, Object> reqMap = HashBasedTable.create();
    private Table<String, String, Object> rspMap = HashBasedTable.create();

    private String packageName;

    private String className;

    private Messager messager;

    private static String COL_METHOD = "method";
    private static String COL_OWNER = "owner";
    private static String COL_PARAS = "paras";
    private static String COL_USERPARAS = "userParas";
    private static String COL_TYPE = "type";
    private static String COL_RSPSEQ = "rspSeq";
    private static String COL_TIMEOUT = "timeout";
    private static String COL_ID = "id";
    private static String COL_CLZ = "clz";
    private static String COL_DELAY = "delay";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (bDone){
            return true;
        }
        bDone = true;

        messager = processingEnv.getMessager();

        Set<? extends Element> msgSet = roundEnvironment.getElementsAnnotatedWith(Message.class);
        for (Element element : msgSet) {
            if (ElementKind.ENUM != element.getKind()){
                continue;
            }
            parseMessage((TypeElement) element);
            generateFile();
        }

        return true;
    }



    private void parseMessage(TypeElement msgDefClass){
        packageName = ((PackageElement) msgDefClass.getEnclosingElement()).getQualifiedName().toString();
        className = "Message$$Generated";
        module = msgDefClass.getAnnotation(Message.class).module();
        nameIdMap.clear();
        reqMap.clear();
        rspMap.clear();
        List<? extends Element> msgElements = msgDefClass.getEnclosedElements();
        Request request;
        Response response;
        String name;
        for (Element element : msgElements){
            if (ElementKind.ENUM_CONSTANT != element.getKind()){
                continue;
            }

            if (null != (request = element.getAnnotation(Request.class))){
                name = module+"_"+element.getSimpleName().toString();
                String method = request.method();
                method = !method.isEmpty() ? method : name;
                nameIdMap.put(name, method);

                reqMap.put(name, COL_METHOD, method);

                String owner = request.owner();
                reqMap.put(name, COL_OWNER, owner);

                String[] paraClzNames = null;
                try {
                    Class[] paraClasses = request.paras();
                }catch (MirroredTypesException mte) {
                    paraClzNames = parseClassNameFromMirroredTypesException(mte);
                }
                reqMap.put(name, COL_PARAS, paraClzNames);

                try {
                    Class[] paraClasses = request.userParas();
                }catch (MirroredTypesException mte) {
                    paraClzNames = parseClassNameFromMirroredTypesException(mte);
                }
                reqMap.put(name, COL_USERPARAS, paraClzNames);

                reqMap.put(name, COL_TYPE, request.type());

                // 获取响应序列
                String[] rspSeq1 = request.rspSeq();
                for (int i=0; i<rspSeq1.length; ++i){
                    rspSeq1[i] = module+"_"+rspSeq1[i];
                }
                String[] rspSeq2 = request.rspSeq2();
                for (int i=0; i<rspSeq2.length; ++i){
                    rspSeq2[i] = module+"_"+rspSeq2[i];
                }
                String[] rspSeq3 = request.rspSeq3();
                for (int i=0; i<rspSeq3.length; ++i){
                    rspSeq3[i] = module+"_"+rspSeq3[i];
                }
                List<String[]> rspSeqList = new ArrayList<>();
                if (rspSeq1.length>0) rspSeqList.add(rspSeq1);
                if (rspSeq2.length>0) rspSeqList.add(rspSeq2);
                if (rspSeq3.length>0) rspSeqList.add(rspSeq3);
                reqMap.put(name, COL_RSPSEQ, rspSeqList.toArray(new String[][]{}));

                // 获取超时时长
                reqMap.put(name, COL_TIMEOUT, request.timeout());

//                messager.printMessage(Diagnostic.Kind.NOTE, "request: "+reqName
//                        + " reqParaFullName: "+reqParaFullName
//                        + " rspSeq: "+request.rspSeq()
//                        + " timeout: "+request.timeout());

            }
            else if (null != (response = element.getAnnotation(Response.class))){
                name = module+"_"+element.getSimpleName().toString();
                String id = response.id();
                id = !id.isEmpty() ? id : element.getSimpleName().toString();
                nameIdMap.put(name, id);

                rspMap.put(name, COL_ID, id);

                // 获取响应对应的消息体类型
                String rspClazzFullName;
                try {
                    Class clz = response.clz();
                    rspClazzFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    rspClazzFullName = parseClassNameFromMirroredTypeException(mte);
                }
//                messager.printMessage(Diagnostic.Kind.NOTE, "response: "+rspName
//                        + " rspClazzFullName: "+rspClazzFullName);

                rspMap.put(name, COL_CLZ, rspClazzFullName);

                rspMap.put(name, COL_DELAY, response.delay());

            }
        }
    }


    private String parseClassNameFromMirroredTypeException(MirroredTypeException mte){
        String className;
        TypeMirror typeMirror = mte.getTypeMirror();
        if (typeMirror instanceof PrimitiveType){
            className = typeMirror.toString();
        }else {
            try { // 为普通类类型
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                className = classTypeElement.getQualifiedName().toString();
            } catch (ClassCastException e) { // 为数组类型
                ArrayType classTypeMirror = (ArrayType) mte.getTypeMirror();
                className = classTypeMirror.getComponentType().toString() + "[]";
            }
        }

        return className;
    }

    private String[] parseClassNameFromMirroredTypesException(MirroredTypesException mte){
        List<String> paraClzNames = new ArrayList<>();
        String className;
        List<? extends TypeMirror> typeMirrors = mte.getTypeMirrors();
        for (TypeMirror mirror : typeMirrors){
            if (mirror instanceof PrimitiveType){
                className = mirror.toString();
            }else {
                try { // 为普通类类型
                    DeclaredType classTypeMirror = (DeclaredType) mirror;
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    className = classTypeElement.getQualifiedName().toString();
                } catch (ClassCastException e) { // 为数组类型
                    ArrayType classTypeMirror = (ArrayType) mirror;
                    className = classTypeMirror.getComponentType().toString() + "[]";
                }
            }
            paraClzNames.add(className);

        }
        return paraClzNames.toArray(new String[]{});
    }


    private void generateFile(){
        String fieldModule = "module";
        String fieldNameIdMap = "nameIdMap";
        String fieldReqMap = "reqMap";
        String fieldRspMap = "rspMap";

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // 构建代码块
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = $S", fieldModule, module)
                .addStatement("$L = $T.create()", fieldNameIdMap, HashBiMap.class)
                .addStatement("$L = $T.create()", fieldReqMap, HashBasedTable.class)
                .addStatement("$L = $T.create()", fieldRspMap, HashBasedTable.class)
                ;

        for(String name : nameIdMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $S)", fieldNameIdMap, name, nameIdMap.get(name));
        }

        for(Table.Cell cell : reqMap.cellSet()){
            String row = (String) cell.getRowKey();
            String col = (String) cell.getColumnKey();
            if (col.equals(COL_METHOD)
                    || col.equals(COL_OWNER)) {
                codeBlockBuilder.addStatement("$L.put($S, $S, $S)", fieldReqMap, row, col, cell.getValue());
            }else if (col.equals(COL_PARAS)
                    || col.equals(COL_USERPARAS)){
                StringBuffer value = new StringBuffer();
                String[] paras = (String[]) cell.getValue();
                for (String para : paras){
                    value.append(para).append(".class, ");
                }
                codeBlockBuilder.addStatement("$L.put($S, $S, new Class[]{$L})", fieldReqMap, row, col, value);
            }else if (col.equals(COL_RSPSEQ)){
                StringBuffer value = new StringBuffer();
                String[][] rspSeq = (String[][]) cell.getValue();
                for (String[] aRspSeq : rspSeq) {
                    value.append("{");
                    for (String anARspSeq : aRspSeq) {
                        value.append("\"").append(anARspSeq).append("\", ");
                    }
                    value.append("}, ");
                }
                codeBlockBuilder.addStatement("$L.put($S, $S, new String[][]{$L})", fieldReqMap, row, col, value);
            }else if (col.equals(COL_TIMEOUT)
                    || col.equals(COL_TYPE)){
                codeBlockBuilder.addStatement("$L.put($S, $S, $L)", fieldReqMap, row, col, cell.getValue());
            }
        }

        for(Table.Cell cell : rspMap.cellSet()){
            String row = (String) cell.getRowKey();
            String col = (String) cell.getColumnKey();
            if (col.equals(COL_ID)) {
                codeBlockBuilder.addStatement("$L.put($S, $S, $S)", fieldRspMap, row, col, cell.getValue());
            }else if (col.equals(COL_CLZ)){
                codeBlockBuilder.addStatement("$L.put($S, $S, $L.class)", fieldRspMap, row, col, cell.getValue());
            }else if (col.equals(COL_DELAY)){
                codeBlockBuilder.addStatement("$L.put($S, $S, $L)", fieldRspMap, row, col, cell.getValue());
            }
        }


        // 构建Class
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.FINAL)
                .addField(FieldSpec.builder(String.class,
                        fieldModule, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(BiMap.class, String.class, String.class),
                        fieldNameIdMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        fieldReqMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        fieldRspMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addStaticBlock(codeBlockBuilder.build())
                .addMethod(constructor.build())
//                .addType(TypeSpec.classBuilder("InnerClassTest").build())
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .build();

        // 生成源文件
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
