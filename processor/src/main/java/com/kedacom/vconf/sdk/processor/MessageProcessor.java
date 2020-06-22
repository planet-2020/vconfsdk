package com.kedacom.vconf.sdk.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

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

import static com.kedacom.vconf.sdk.annotation.Request.GET;


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
    private Table<String, String, Object> reqMap = HashBasedTable.create();
    private Table<String, String, Object> rspMap = HashBasedTable.create();

    private String packageName;

    private String className = "Msg$$Generated";

    private Messager messager;

    private static String COL_ID = "id";
    private static String COL_OWNER = "owner";
    private static String COL_PARAS = "paras";
    private static String COL_USERPARAS = "userParas";
    private static String COL_TYPE = "type";
    private static String COL_RSPSEQ = "rspSeq";
    private static String COL_TIMEOUT = "timeout";
    private static String COL_CLZ = "clz";
    private static String COL_DELAY = "delay";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (bDone){
            return true;
        }
        bDone = true;

        messager = processingEnv.getMessager();

//        messager.printMessage(Diagnostic.Kind.NOTE, "START to generate msg file ... ");

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
        module = msgDefClass.getAnnotation(Message.class).module();
        if (module.trim().isEmpty()){
            throw new IllegalArgumentException(msgDefClass+": module name can not be empty!");
        }
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
                method = !method.isEmpty() ? method : element.getSimpleName().toString();

                reqMap.put(name, COL_ID, method);

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
                List<String[]> rspSeqList = new ArrayList<>();
                processRspSeqs(rspSeqList,
                        request.rspSeq(),
                        request.rspSeq2(),
                        request.rspSeq3(),
                        request.rspSeq4()
                );
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


    private void processRspSeqs(List<String[]> rspSeqList, String[]... rspSeqs){
        for (String[] rspSeq : rspSeqs){
            for (int i=0; i<rspSeq.length; ++i){
                rspSeq[i] = module+"_"+rspSeq[i];
            }
            if (rspSeq.length>0){
                rspSeqList.add(rspSeq);
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
        String fieldReqMap = "reqMap";
        String fieldRspMap = "rspMap";
        String fieldReqName = "reqName";
        String fieldRspName = "rspName";
        String fieldRspNames = "rspNames";
        String fieldRspId = "rspId";

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // 构建代码块
        CodeBlock.Builder staticCodeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = $S", fieldModule, module)
                .addStatement("$L = $T.create()", fieldReqMap, HashBasedTable.class)
                .addStatement("$L = $T.create()", fieldRspMap, HashBasedTable.class)
                ;

        for(Table.Cell cell : reqMap.cellSet()){
            String row = (String) cell.getRowKey();
            String col = (String) cell.getColumnKey();
            if (col.equals(COL_ID)
                    || col.equals(COL_OWNER)) {
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $S)", fieldReqMap, row, col, cell.getValue());
            }else if (col.equals(COL_PARAS)
                    || col.equals(COL_USERPARAS)){
                StringBuffer value = new StringBuffer();
                String[] paras = (String[]) cell.getValue();
                for (String para : paras){
                    value.append(para).append(".class, ");
                }
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, new Class[]{$L})", fieldReqMap, row, col, value);
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
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, new String[][]{$L})", fieldReqMap, row, col, value);
            }else if (col.equals(COL_TIMEOUT)
                    || col.equals(COL_TYPE)){
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $L)", fieldReqMap, row, col, cell.getValue());
            }
        }

        for(Table.Cell cell : rspMap.cellSet()){
            String row = (String) cell.getRowKey();
            String col = (String) cell.getColumnKey();
            if (col.equals(COL_ID)) {
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $S)", fieldRspMap, row, col, cell.getValue());
            }else if (col.equals(COL_CLZ)){
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $L.class)", fieldRspMap, row, col, cell.getValue());
            }else if (col.equals(COL_DELAY)){
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $L)", fieldRspMap, row, col, cell.getValue());
            }
        }

        // 实现IMagicBook
        List<MethodSpec> methodSpecs = new ArrayList<>();
        MethodSpec getName = MethodSpec.methodBuilder("getName")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", className)
                .build();
        methodSpecs.add(getName);

        MethodSpec getChapter = MethodSpec.methodBuilder("getChapter")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addCode("return $S;\n", module)
                .build();
        methodSpecs.add(getChapter);

        MethodSpec isReqTypeGet = MethodSpec.methodBuilder("isReqTypeGet")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(boolean.class)
                .addCode("Object val = $L.row($L).get($S);\n" +
                                "if (null == val) return false;\n" +
                        "return $L == (int)val;\n",
                        fieldReqMap, fieldReqName, COL_TYPE, GET)
                .build();
        methodSpecs.add(isReqTypeGet);

        MethodSpec getReqId = MethodSpec.methodBuilder("getReqId")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(String.class)
                .addCode("return (String)$L.row($L).get($S);\n", fieldReqMap, fieldReqName, COL_ID)
                .build();
        methodSpecs.add(getReqId);

        MethodSpec getNativeMethodOwner = MethodSpec.methodBuilder("getNativeMethodOwner")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(String.class)
                .addCode("return (String)$L.row($L).get($S);\n", fieldReqMap, fieldReqName, COL_OWNER)
                .build();
        methodSpecs.add(getNativeMethodOwner);

        MethodSpec getNativeParaClasses = MethodSpec.methodBuilder("getNativeParaClasses")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class))))
                .addCode("return (Class<?>[])$L.row($L).get($S);\n", fieldReqMap, fieldReqName, COL_PARAS)
                .build();
        methodSpecs.add(getNativeParaClasses);

        MethodSpec getUserParaClasses = MethodSpec.methodBuilder("getUserParaClasses")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class))))
                .addCode("return (Class<?>[])$L.row($L).get($S);\n", fieldReqMap, fieldReqName, COL_USERPARAS)
                .build();
        methodSpecs.add(getUserParaClasses);

        MethodSpec getTimeout = MethodSpec.methodBuilder("getTimeout")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(int.class)
                .addCode("Object val = $L.row($L).get($S);\n" +
                                "if (null == val) return 5;\n" +
                                "return (int)val;\n",
                        fieldReqMap, fieldReqName, COL_TIMEOUT)
                .build();
        methodSpecs.add(getTimeout);

        MethodSpec getRspSeqs = MethodSpec.methodBuilder("getRspSeqs")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldReqName).build())
                .returns(String[][].class)
                .addCode("return (String[][])$L.row($L).get($S);\n", fieldReqMap, fieldReqName, COL_RSPSEQ)
                .build();
        methodSpecs.add(getRspSeqs);

        MethodSpec getRspId = MethodSpec.methodBuilder("getRspId")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldRspName).build())
                .returns(String.class)
                .addCode("return (String)$L.row($L).get($S);\n", fieldRspMap, fieldRspName, COL_ID)
                .build();
        methodSpecs.add(getRspId);

        MethodSpec getRspClazz = MethodSpec.methodBuilder("getRspClazz")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldRspName).build())
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                .addCode("return (Class<?>)$L.row($L).get($S);\n", fieldRspMap, fieldRspName, COL_CLZ)
                .build();
        methodSpecs.add(getRspClazz);

        MethodSpec getRspNames = MethodSpec.methodBuilder("getRspNames")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, fieldRspId).build())
                .returns(ParameterizedTypeName.get(List.class, String.class))
                .addStatement("$T<$T> $L = new $T<>()", List.class, String.class, fieldRspNames, ArrayList.class)
                .beginControlFlow("for ($T<$T, $T, Object> cell: $L.cellSet())", Table.Cell.class, String.class, String.class, fieldRspMap)
                .beginControlFlow("if ($S.equals(cell.getColumnKey()) && $L.equals(cell.getValue()))", COL_ID, fieldRspId)
                .addStatement("$L.add(cell.getRowKey())", fieldRspNames)
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $L", fieldRspNames)
                .build();
        methodSpecs.add(getRspNames);

        //        if (null == reqMap.row(reqName)) return null;
//        return (Class[]) reqMap.row(reqName).get(COL_PARAS);

        /*构建Class
        * 对于生成的类，我们不希望能通过常规手段访问，只允许通过反射访问（只让框架知道访问方式），
        * 所以我们将类及其成员的访问权限限制到最小。
        * */
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.FINAL)
                .addField(FieldSpec.builder(String.class,
                        fieldModule, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        fieldReqMap, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        fieldRspMap, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addMethod(constructor.build())
                .addStaticBlock(staticCodeBlockBuilder.build())
//                .addType(TypeSpec.classBuilder("InnerClassTest").build())
                .addSuperinterface(ClassName.get("com.kedacom.vconf.sdk.amulet", "IMagicBook"))
                .addMethods(methodSpecs)
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .build();

        // 生成源文件
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        messager.printMessage(Diagnostic.Kind.NOTE, "SUCCESS! generate file : "+ packageName+"."+className+".java");
    }

}
