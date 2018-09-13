package com.sissi.annotation;

/**
 * Created by Sissi on 2018/9/10.
 */

/**
 * 用来指明注解生成类的消费者,意义在于指导注解处理器生成正确的包名以便于消费者访问.
 *
 * 例如:
 * package com.kedacom.app;
 *
 * @Consumer(Message.class)
 * ClassA{}
 *
 * Message为注解类, 经注解处理器处理后会生成相关的类,如Message$$Generated.java, 此谓"注解生成类" ,
 * 此例中该类的完整路径将会是com.kedacom.app.Message$$Generated, 与ClassA,此谓"注解生成类的消费者" ,处于同一包内,
 * 这样以方便ClassA访问,不会出现访问不到的情形. 做到这点就是通过对消费者声明@Consumer注解,这样注解处理器就能感知
 * 该注解的消费者,并把注解生成类与注解生成类的消费者置于同一包名下.
 *
 * 注意: 同一个注解的消费者只能是唯一的,多于一个消费者只有其中一个有效,其余被忽略. 比如:
 *  * @Consumer(Message.class)
 * ClassA{}
 *
 *  * @Consumer(Message.class)
 * ClassB{}
 * 则ClassB的被忽略.
 * */

public @interface Consumer {
    Class[] value(); // 可消费的注解列表
}
