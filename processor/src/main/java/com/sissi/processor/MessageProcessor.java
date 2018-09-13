package com.sissi.processor;

import com.google.auto.service.AutoService;
import com.sissi.annotation.Consumer;
import com.sissi.annotation.Message;
import com.sissi.annotation.Request;
import com.sissi.annotation.Response;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.tools.Diagnostic;

/**
 * Created by Sissi on 2018/9/3.
 */

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.sissi.annotation.Message",
        "com.sissi.annotation.Request",
        "com.sissi.annotation.Response",
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class MessageProcessor extends AbstractProcessor {

    private boolean bDone = false;

    private Map<String, String> reqParaMap = new HashMap<>();

    private Map<String, String[][]> reqRspsMap = new HashMap<>();

    private Map<String, Integer> reqTimeoutMap = new HashMap<>();

    private Map<String, String> rspClazzMap = new HashMap<>();

    private Set<String> reqSet = new HashSet<>();

    private Set<String> rspSet = new HashSet<>();

    private String packageName;

    private String className;

    private Messager messager;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "############# IN");

        if (bDone){
            return true;
        }
        bDone = true;

        if (collectInfo(roundEnvironment)) {
            generateFile();
        }

        messager.printMessage(Diagnostic.Kind.NOTE, "############# OUT");

        return true;
    }


    private boolean collectInfo(RoundEnvironment roundEnvironment){
        reqParaMap.clear();
        reqRspsMap.clear();
        reqTimeoutMap.clear();
        rspClazzMap.clear();
        reqSet.clear();
        rspSet.clear();

        Set<? extends Element> msgSet = roundEnvironment.getElementsAnnotatedWith(Message.class);

        if (null==msgSet || !msgSet.iterator().hasNext()){
            return false;
        }

        TypeElement msgDefClass = (TypeElement) msgSet.iterator().next();

        // 获取“请求-响应”相关信息
        List<? extends Element> msgElements = msgDefClass.getEnclosedElements();
        Request request;
        Response response;
        Class clz;
        String reqParaFullName;
        String rspClazzFullName;
        String reqName;
        String rspName;
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

                reqSet.add(reqName);

//                messager.printMessage(Diagnostic.Kind.NOTE, "request: "+reqName
//                        + " reqParaFullName: "+reqParaFullName
//                        + " rspSeq: "+request.rspSeq()
//                        + " timeout: "+request.timeout());

            }else if (null != (response = element.getAnnotation(Response.class))){
                rspName = element.getSimpleName().toString();

                // 获取响应对应的消息体类型
                try {
                    clz = response.value();
                    rspClazzFullName = clz.getCanonicalName();
                }catch (MirroredTypeException mte) {
                    DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    rspClazzFullName = classTypeElement.getQualifiedName().toString();
                }

//                messager.printMessage(Diagnostic.Kind.NOTE, "response: "+rspName
//                        + " rspClazzFullName: "+rspClazzFullName);

                rspClazzMap.put(rspName, rspClazzFullName);

                rspSet.add(rspName);

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
                List<DeclaredType> classTypeMirrors = (List<DeclaredType>) mte.getTypeMirrors();
                for (DeclaredType classTypeMirror : classTypeMirrors) {
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
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

        messager.printMessage(Diagnostic.Kind.NOTE, "msgDefClass="+msgDefClass.getQualifiedName()
                + "\ngen packageName="+packageName
                + "\ngen className="+className);

        return true;
    }


    private void generateFile(){
        String fieldNameReqSet = "reqSet";
        String fieldNameRspSet = "rspSet";
        String fieldNameReqParaMap = "reqParaMap";
        String fieldNameReqRspsMap = "reqRspsMap";
        String fieldNameReqTimeoutMap = "reqTimeoutMap";
        String fieldNameRspClazzMap = "rspClazzMap";

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // 构建代码块
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = new $T<>()", fieldNameReqSet, HashSet.class)
                .addStatement("$L = new $T<>()", fieldNameRspSet, HashSet.class)
                .addStatement("$L = new $T<>()", fieldNameReqParaMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameReqRspsMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameReqTimeoutMap, HashMap.class)
                .addStatement("$L = new $T<>()", fieldNameRspClazzMap, HashMap.class)
                ;

        for (String req : reqSet){
            codeBlockBuilder.addStatement("$L.add($S)", fieldNameReqSet, req);
        }

        for (String rsp : rspSet){
            codeBlockBuilder.addStatement("$L.add($S)", fieldNameRspSet, rsp);
        }

        for(String req : reqParaMap.keySet()){
            codeBlockBuilder.addStatement("$L.put($S, $L.class)", fieldNameReqParaMap, req, reqParaMap.get(req));
        }

        for(String req : reqRspsMap.keySet()){
            StringBuffer value = new StringBuffer();
            String[][] rspSeq = reqRspsMap.get(req);
            for (int i=0; i<rspSeq.length; ++i) {
                value.append("{");
                for (int j=0; j<rspSeq[i].length; ++j) {
                    value.append("\"" + rspSeq[i][j] + "\", ");
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


        // 构建Class
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Set.class, String.class),
                        fieldNameReqSet, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Set.class, String.class),
                        fieldNameRspSet, Modifier.PUBLIC, Modifier.STATIC)
                        .build())
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
