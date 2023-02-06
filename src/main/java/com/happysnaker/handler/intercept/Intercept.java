package com.happysnaker.handler.intercept;


import java.lang.annotation.*;

/**
 * 标记该类成为以一个拦截器
 *
 * @author happysnakers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Intercept {
    /**
     * <p><strong>后置拦截方法的调用顺序，order 越高，则越先调用</strong></p>
     * <p>前置拦截方法是无状态的，无优先级概念</p>
     * <P><strong>但是对于后置拦截而言，前一个拦截器的输出将作为下一个输入，因此是有顺序的</strong></P>
     */
    int order() default 1;
}