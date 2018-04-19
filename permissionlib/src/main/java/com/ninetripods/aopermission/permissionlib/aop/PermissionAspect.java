package com.ninetripods.aopermission.permissionlib.aop;

import android.app.Fragment;
import android.content.Context;
import com.ninetripods.aopermission.permissionlib.PermissionRequestActivity;
import com.ninetripods.aopermission.permissionlib.annotation.NeedPermission;
import com.ninetripods.aopermission.permissionlib.annotation.PermissionCanceled;
import com.ninetripods.aopermission.permissionlib.annotation.PermissionDenied;
import com.ninetripods.aopermission.permissionlib.bean.CancelBean;
import com.ninetripods.aopermission.permissionlib.bean.DenyBean;
import com.ninetripods.aopermission.permissionlib.interf.IPermission;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 面向切面编程（AOP，Aspect-oriented programming）对程序设置埋点，运行到设置的埋点执行我们插入的逻辑，以达到监控程序行为目的.
 * AspectJ实际上是对AOP编程思想的一个实践，它不是一个新的语言，它就是一个代码编译器（ajc），在Java编译器的基础上增加了一些它自己的关键字识别和编译方法.
 * 它就是一个编译器+一个库，可以让开发者最大限度的发挥，实现形形色色的AOP程序.
 * 非侵入式监控： 可以在不修监控目标的情况下监控其运行，截获某类方法，甚至可以修改其参数和运行轨迹.
 * 支持编译期和加载时代码注入，不影响性能.
 * 在不侵入原有代码的基础上，增加新的代码.
 *
 *  AspectJ原理剖析
 1、Join Points(连接点)
 Join Points，简称JPoints，是AspectJ的核心思想之一，它就像一把刀，把程序的整个执行过程切成了一段段不同的部分。
 例如，构造方法调用、调用方法、方法执行、异常等等，这些都是Join Points，实际上，也就是你想把新的代码插在程序的哪个地方，
 是插在构造方法中，还是插在某个方法调用前，或者是插在某个方法中，这个地方就是Join Points，当然，不是所有地方都能给你插的，
 只有能插的地方，才叫Join Points。

 2、Pointcuts(切入点)
 告诉代码注入工具，在何处注入一段特定代码的表达式。例如，在哪些 joint points 应用一个特定的 Advice。
 切入点可以选择唯一一个，比如执行某一个方法，也可以有多个选择,可简单理解为带条件的Join Points，作为我们需要的代码切入点。

 3、Advice（通知）
 如何注入到我的class文件中的代码。典型的 Advice 类型有 before、after 和 around，
 分别表示在目标方法执行之前、执行后和完全替代目标方法执行的代码。 上面的例子中用的就是最简单的Advice——Before。

 4、Aspect（切面）: Pointcut 和 Advice 的组合看做切面。例如，我们在应用中通过定义一个 pointcut 和给定恰当的advice，添加一个日志切面。

 5、Weaving（织入）: 注入代码（advices）到目标位置（joint points）的过程。

 Around确实实现了Before和After的功能，但是要注意的是，Around和After是不能同时作用在同一个方法上的，会产生重复切入的问题

 * Created by mq on 2018/3/6 上午11:33
 * mqcoder90@gmail.com
 */
@Aspect
public class PermissionAspect {

    private static final String PERMISSION_REQUEST_POINTCUT =
            "execution(@com.ninetripods.aopermission.permissionlib.annotation.NeedPermission * *(..))";

    /**
     * 注解并加上了类似正则表达式的过滤条件.
     * execution是在被切入的方法中，call是在调用被切入的方法前或者后。
     * 对于Call来说：
     * Call（Before）
     * Pointcut{
     * Pointcut Method
     * }
     * Call（After）
     *
     * 对于Execution来说：
     * Pointcut{
     * execution（Before）
     * Pointcut Method
     * execution（After）
     * }
     */
    @Pointcut(PERMISSION_REQUEST_POINTCUT + " && @annotation(needPermission)") public void requestPermissionMethod(NeedPermission needPermission) {
    }

    /**
     * Around实现了Before和After的功能在方法的开始结束地方切入,Before在方法之前，After在方法之后切入.
     * @param joinPoint
     * @param needPermission
     */
    @Around("requestPermissionMethod(needPermission)") public void AroundJoinPoint(final ProceedingJoinPoint joinPoint, NeedPermission needPermission) {

        Context context = null;
        final Object object = joinPoint.getThis();
        if (object instanceof Context) {
            context = (Context) object;
        } else if (object instanceof Fragment) {
            context = ((Fragment) object).getActivity();
        } else if (object instanceof android.support.v4.app.Fragment) {
            context = ((android.support.v4.app.Fragment) object).getActivity();
        }
        if (context == null || needPermission == null) return;

        PermissionRequestActivity.PermissionRequest(context, needPermission.value(),
                needPermission.requestCode(), new IPermission() {
                    @Override
                    public void PermissionGranted() {
                        try {
                            joinPoint.proceed();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }

                    /**
                     *  权限被deny与cancel 都是通过接口会掉到代码中请求权限的地方如ctivity，Fragment，Service等
                     * @param requestCode
                     * @param denyList
                     */
                    @Override public void PermissionDenied(int requestCode, List<String> denyList) {
                        Class<?> cls = object.getClass();
                        Method[] methods = cls.getDeclaredMethods();
                        if (methods == null || methods.length == 0) return;
                        for (Method method : methods) {
                            //过滤不含自定义注解PermissionDenied的方法
                            boolean isHasAnnotation = method.isAnnotationPresent(PermissionDenied.class);
                            if (isHasAnnotation) {
                                method.setAccessible(true);
                                //获取方法类型
                                Class<?>[] types = method.getParameterTypes();
                                if (types == null || types.length != 1) return;
                                //获取方法上的注解
                                PermissionDenied aInfo = method.getAnnotation(PermissionDenied.class);
                                if (aInfo == null) return;
                                //解析注解上对应的信息
                                DenyBean bean = new DenyBean();
                                bean.setRequestCode(requestCode);
                                bean.setDenyList(denyList);
                                try {
                                    method.invoke(object, bean);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    @Override
                    public void PermissionCanceled(int requestCode) {
                        Class<?> cls = object.getClass();
                        Method[] methods = cls.getDeclaredMethods();
                        if (methods == null || methods.length == 0) return;
                        for (Method method : methods) {
                            //过滤不含自定义注解PermissionCanceled的方法
                            boolean isHasAnnotation = method.isAnnotationPresent(PermissionCanceled.class);
                            if (isHasAnnotation) {
                                method.setAccessible(true);
                                //获取方法类型
                                Class<?>[] types = method.getParameterTypes();
                                if (types == null || types.length != 1) return;
                                //获取方法上的注解
                                PermissionCanceled aInfo = method.getAnnotation(PermissionCanceled.class);
                                if (aInfo == null) return;
                                //解析注解上对应的信息
                                CancelBean bean = new CancelBean();
                                bean.setRequestCode(requestCode);
                                try {
                                    method.invoke(object, bean);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
    }

}
