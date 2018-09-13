package com.sissi.processor;

import com.google.auto.service.AutoService;
import com.sissi.annotation.Consumer;
import com.sissi.annotation.Message;
import com.sissi.annotation.SerializeEnumAsInt;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.tools.Diagnostic;

/**
 * Created by Sissi on 2018/9/6.
 */

@SupportedAnnotationTypes({
        "com.sissi.annotation.SerializeEnumAsInt",
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@AutoService(Processor.class)
public class SerializationProcessor extends AbstractProcessor {

    private boolean bDone = false;

    private Messager messager;

    private Set<String> serializeEnumAsIntSet = new HashSet<>();

    private String packageName;

    private String className;

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

        serializeEnumAsIntSet.clear();

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(SerializeEnumAsInt.class);
        TypeElement typeElement;
        for (Element element : elements){
            typeElement = (TypeElement) element;
//            messager.printMessage(Diagnostic.Kind.NOTE, "SerializeEnumAsInt element="+typeElement.getQualifiedName());
            if (ElementKind.ENUM == typeElement.getKind()){
                // 修饰的枚举类型则直接针对该枚举类型生效
                serializeEnumAsIntSet.add(typeElement.getQualifiedName().toString());
            }else{
                // 修饰的非枚举类型则针对该类型的直接内部枚举类型生效
                List<? extends Element> subElements = element.getEnclosedElements();
                for (Element subElement : subElements){
//                    messager.printMessage(Diagnostic.Kind.NOTE, "subElement="+subElement.getSimpleName());
                    if (ElementKind.ENUM != subElement.getKind()){
                        continue;
                    }

                    serializeEnumAsIntSet.add(((TypeElement)subElement).getQualifiedName().toString());
                }
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
                for (Class clz : clzs){
                    if (SerializeEnumAsInt.class.getCanonicalName().equals(clz.getCanonicalName())){
                        PackageElement packageElement = (PackageElement) element.getEnclosingElement();
                        packageName = packageElement.getQualifiedName().toString();
                        found = true;
                        break;
                    }
                }
            }catch (MirroredTypesException mte) {
                List<DeclaredType> classTypeMirrors = (List<DeclaredType>) mte.getTypeMirrors();
                for (DeclaredType classTypeMirror : classTypeMirrors) {
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    if (SerializeEnumAsInt.class.getCanonicalName().equals(classTypeElement.getQualifiedName().toString())){
                        PackageElement packageElement = (PackageElement) element.getEnclosingElement();
                        packageName = packageElement.getQualifiedName().toString();
                        found = true;
                        break;
                    }
                }

            }
        }

        // 获取待生成文件的类名
        className = SerializeEnumAsInt.class.getSimpleName()+"$$Generated";

        messager.printMessage(Diagnostic.Kind.NOTE, "\ngen packageName="+packageName
                + "\ngen className="+className);

        return true;
    }

    private void generateFile(){
        String fieldNameSerializeEnumAsIntSet = "serializeEnumAsIntSet";

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // 构建代码块
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = new $T<>()", fieldNameSerializeEnumAsIntSet, HashSet.class)
                ;
        for (String serializeEnumAsInt : serializeEnumAsIntSet){
            codeBlockBuilder.addStatement("$L.add($L.class)", fieldNameSerializeEnumAsIntSet, serializeEnumAsInt);
        }

        // 构建Class
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Set.class, Class.class),
                        fieldNameSerializeEnumAsIntSet, Modifier.PUBLIC, Modifier.STATIC)
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
