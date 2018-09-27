package com.sissi.vconfsdk.processor;

import com.google.auto.service.AutoService;
import com.sissi.vconfsdk.annotation.Consumer;
import com.sissi.vconfsdk.annotation.Get;
import com.sissi.vconfsdk.annotation.Message;
import com.sissi.vconfsdk.annotation.Notification;
import com.sissi.vconfsdk.annotation.Request;
import com.sissi.vconfsdk.annotation.Response;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Created by Sissi on 2018/9/3.
 */

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.sissi.vconfsdk.annotation.Message",
        "com.sissi.vconfsdk.annotation.Request",
        "com.sissi.vconfsdk.annotation.Response",
        "com.sissi.vconfsdk.annotation.Notification",
        "com.sissi.vconfsdk.annotation.Get",
        "com.sissi.vconfsdk.annotation.Set",
        "com.sissi.vconfsdk.annotation.Consumer",
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MessageProcessor extends AbstractProcessor {

    private boolean bDone = false;

    private Map<String, String> reqParaMap = new HashMap<>();

    private Map<String, String[][]> reqRspsMap = new HashMap<>();

    private Map<String, Integer> reqTimeoutMap = new HashMap<>();

    private Map<String, String> rspClazzMap = new HashMap<>();

    private Map<String, Integer> rspDelayMap = new HashMap<>();

    private Map<String, String> ntfClazzMap = new HashMap<>();

    private Map<String, Integer> ntfDelayMap = new HashMap<>();

    private Map<String, String> getParaClazzMap = new HashMap<>();

    private Map<String, String> getResultClazzMap = new HashMap<>();

    private Map<String, String> setParaClazzMap = new HashMap<>();

    private String packageName;

    private String className;

    private Messager messager;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (bDone){
            return true;
        }
        bDone = true;

        messager = processingEnv.getMessager();

        if (collectInfo(roundEnvironment)) {
            generateFile();
        }

        return true;
    }


    private boolean collectInfo(RoundEnvironment roundEnvironment){
        reqParaMap.clear();
        reqRspsMap.clear();
        reqTimeoutMap.clear();
        rspClazzMap.clear();
        ntfClazzMap.clear();
        getParaClazzMap.clear();
        getResultClazzMap.clear();
        setParaClazzMap.clear();

        Set<? extends Element> msgSet = roundEnvironment.getElementsAnnotatedWith(Message.class);

        if (null==msgSet || !msgSet.iterator().hasNext()){
            return false;
        }

        TypeElement msgDefClass = (TypeElement) msgSet.iterator().next();

        // 获取“请求-响应”相关信息
        List<? extends Element> msgElements = msgDefClass.getEnclosedElements();
        Request request;
        Response response;
        Notification notification;
        Get get;
        com.sissi.vconfsdk.annotation.Set set;
        Class clz;
        String reqParaFullName;
        String rspClazzFullName;
        String ntfClazzFullName;
        String getParaFullName;
        String getResultFullName;
        String setParaFullName;
        String reqName;
        String rspName;
        String ntfName;
        String getName;
        String setName;
        for (Element element : msgElements){
            if (ElementKind.ENUM_CONSTANT != element.getKind()){
                continue;
            }

            if (null != (request = element.getAnnotation(Request.class))){
                reqName = element.getSimpleName().toString();
                // 获取请求参数
                try {
                    clz = request.reqPara();
                    reqParaFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    reqParaFullName = classTypeElement.getQualifiedName().toString();
                }

                reqParaMap.put(reqName, reqParaFullName);

                // 获取响应序列
                if (0 == request.rspSeq2().length){
                    reqRspsMap.put(reqName, new String[][]{request.rspSeq()});
                }else if (0 == request.rspSeq3().length){
                    reqRspsMap.put(reqName, new String[][]{request.rspSeq(), request.rspSeq2()});
                }else{
                    reqRspsMap.put(reqName, new String[][]{request.rspSeq(), request.rspSeq2(), request.rspSeq3()});
                }

                // 获取超时时长
                reqTimeoutMap.put(reqName, request.timeout());

//                messager.printMessage(Diagnostic.Kind.NOTE, "request: "+reqName
//                        + " reqParaFullName: "+reqParaFullName
//                        + " rspSeq: "+request.rspSeq()
//                        + " timeout: "+request.timeout());

            }else if (null != (response = element.getAnnotation(Response.class))){
                rspName = element.getSimpleName().toString();

                // 获取响应对应的消息体类型
                try {
                    clz = response.clz();
                    rspClazzFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    rspClazzFullName = classTypeElement.getQualifiedName().toString();
                }

//                messager.printMessage(Diagnostic.Kind.NOTE, "response: "+rspName
//                        + " rspClazzFullName: "+rspClazzFullName);

                rspClazzMap.put(rspName, rspClazzFullName);

                rspDelayMap.put(rspName, response.delay());

            }else if (null != (notification = element.getAnnotation(Notification.class))){
                ntfName = element.getSimpleName().toString();

                // 获取通知对应的消息体类型
                try {
                    clz = notification.clz();
                    ntfClazzFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    ntfClazzFullName = classTypeElement.getQualifiedName().toString();
                }

//                messager.printMessage(Diagnostic.Kind.NOTE, "ntfName: "+ntfName
//                        + " ntfClazzFullName: "+ntfClazzFullName);

                ntfClazzMap.put(ntfName, ntfClazzFullName);

                ntfDelayMap.put(ntfName, notification.delay());

            }else if (null != (get = element.getAnnotation(Get.class))){
                getName = element.getSimpleName().toString();
                // 获取请求参数
                try {
                    clz = get.para();
                    getParaFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    getParaFullName = classTypeElement.getQualifiedName().toString();
                }

                getParaClazzMap.put(getName, getParaFullName);

                // 获取结果
                try {
                    clz = get.result();
                    getResultFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    getResultFullName = classTypeElement.getQualifiedName().toString();
                }
                getResultClazzMap.put(getName, getResultFullName);

//                messager.printMessage(Diagnostic.Kind.NOTE, "getName: "+getName
//                        + " getParaFullName: "+getParaFullName
//                        + " result class: "+ getResultFullName);

            }else if (null != (set = element.getAnnotation(com.sissi.vconfsdk.annotation.Set.class))){
                setName = element.getSimpleName().toString();

                // 获取响应对应的消息体类型
                try {
                    clz = set.value();
                    setParaFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    setParaFullName = classTypeElement.getQualifiedName().toString();
                }

//                messager.printMessage(Diagnostic.Kind.NOTE, "setName: "+setName
//                        + " setParaFullName: "+setParaFullName);

                setParaClazzMap.put(setName, setParaFullName);
            }

        }


        // 获取待生成文件的包名
        Set<? extends Element> consumerSet = roundEnvironment.getElementsAnnotatedWith(Consumer.class);
        Consumer consumer;
        Class[] clzs;
        boolean found = false;
        for (Element element:consumerSet){
            if (found){
                break;
            }
            consumer = element.getAnnotation(Consumer.class);
            try {
                clzs = consumer.value();
                for (Class cls : clzs){
//                    messager.printMessage(Diagnostic.Kind.NOTE, "Message.class.getCanonicalName(): "+Message.class.getCanonicalName()
//                            + "\n clz name="+clz.getCanonicalName());
                    if (Message.class.getCanonicalName().equals(cls.getCanonicalName())){
                        PackageElement packageElement = (PackageElement) element.getEnclosingElement();
                        packageName = packageElement.getQualifiedName().toString();
                        messager.printMessage(Diagnostic.Kind.NOTE, "packageName: "+packageName);
                        found = true;
                        break;
                    }
                }
            }catch (MirroredTypesException mte) {
                List<? extends TypeMirror> classTypeMirrors = mte.getTypeMirrors();
                DeclaredType declaredType;
                for (TypeMirror classTypeMirror : classTypeMirrors) {
                    declaredType = (DeclaredType) classTypeMirror;
                    TypeElement classTypeElement = (TypeElement) declaredType.asElement();
//                    messager.printMessage(Diagnostic.Kind.NOTE, "Message.class: " + Message.class.getCanonicalName()
//                            + "\nclz name=" + classTypeElement.getQualifiedName().toString());
                    if (Message.class.getCanonicalName().equals(classTypeElement.getQualifiedName().toString())){
                        PackageElement packageElement = (PackageElement) element.getEnclosingElement();
                        packageName = packageElement.getQualifiedName().toString();
                        found = true;
                        break;
                    }
                }

            }
        }

        // 获取待生成文件的类名
        className = Message.class.getSimpleName()+"$$Generated";

//        messager.printMessage(Diagnostic.Kind.NOTE, "msgDefClass="+msgDefClass.getQualifiedName()
//                + "\ngen packageName="+packageName
//                + "\ngen className="+className);

        return true;
    }


    private void generateFile(){
        String fieldNameReqParaMap = "reqParaMap";
        String fieldNameReqRspsMap = "reqRspsMap";
        String fieldNameReqTimeoutMap = "reqTimeoutMap";
        String fieldNameRspClazzMap = "rspClazzMap";
        String fieldNameRspDelayMap = "rspDelayMap";
        String fieldNameNtfClazzMap = "ntfClazzMap";
        String fieldNameNtfDelayMap = "ntfDelayMap";
        String fieldNameGetParaClazzMap = "getParaClazzMap";
        String fieldNameGetResultClazzMap = "getResultClazzMap";
        String fieldNameSetParaClazzMap = "setParaClazzMap";

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // 构建代码块
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = new $T<>()", fieldNameReqParaMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameReqRspsMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameReqTimeoutMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameRspClazzMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameRspDelayMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameNtfClazzMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameNtfDelayMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameGetParaClazzMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameGetResultClazzMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameSetParaClazzMap, HashMap.class)
                ;

        for(String req : reqParaMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameReqParaMap, req, reqParaMap.get(req));
        }

        for(String req : reqRspsMap.keySet()){
            StringBuffer value = new StringBuffer();
            String[][] rspSeq = reqRspsMap.get(req);
            for (String[] aRspSeq : rspSeq) {
                value.append("{");
                for (String anARspSeq : aRspSeq) {
                    value.append("\"").append(anARspSeq).append("\", ");
                }
                value.append("}, ");
            }
            codeBlockBuilder.addStatement("$L.put($S, new String[][]{$L})", fieldNameReqRspsMap, req, value);
        }

        for(String req : reqTimeoutMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L)", fieldNameReqTimeoutMap, req, reqTimeoutMap.get(req));
        }

        for(String rsp : rspClazzMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameRspClazzMap, rsp, rspClazzMap.get(rsp));
        }

        for(String rsp : rspDelayMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L)", fieldNameRspDelayMap, rsp, rspDelayMap.get(rsp));
        }

        for(String ntf : ntfClazzMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameNtfClazzMap, ntf, ntfClazzMap.get(ntf));
        }

        for(String ntf : ntfDelayMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L)", fieldNameNtfDelayMap, ntf, ntfDelayMap.get(ntf));
        }

        for(String get : getParaClazzMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameGetParaClazzMap, get, getParaClazzMap.get(get));
        }

        for(String get : getResultClazzMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameGetResultClazzMap, get, getResultClazzMap.get(get));
        }

        for(String set : setParaClazzMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameSetParaClazzMap, set, setParaClazzMap.get(set));
        }

        // 构建Class
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class),
                        fieldNameReqParaMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String[][].class),
                        fieldNameReqRspsMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Integer.class),
                        fieldNameReqTimeoutMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class),
                        fieldNameRspClazzMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Integer.class),
                        fieldNameRspDelayMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class),
                        fieldNameNtfClazzMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Integer.class),
                        fieldNameNtfDelayMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class),
                        fieldNameGetParaClazzMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class),
                        fieldNameGetResultClazzMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Class.class),
                        fieldNameSetParaClazzMap, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addStaticBlock(codeBlockBuilder.build())
                .addMethod(constructor.build())
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
