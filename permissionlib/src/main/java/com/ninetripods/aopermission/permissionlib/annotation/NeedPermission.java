package com.ninetripods.aopermission.permissionlib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 自定义注解
 * 创建注解：
 * 这个注解周期声明在 class 文件上（RetentionPolicy.CLASS），
 * 可以注解构造函数和方法（ElementType.CONSTRUCTOR 和 ElementType.METHOD）
 *
 * Created by mq on 2018/3/6 上午11:34
 * mqcoder90@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NeedPermission {

    String[] value();

    int requestCode() default 0;
}
