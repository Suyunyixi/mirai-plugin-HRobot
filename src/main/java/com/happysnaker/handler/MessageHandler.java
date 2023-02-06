package com.happysnaker.handler;

import java.lang.annotation.*;

/**
 * 标记该类成为以一个消息事件处理者
 *
 * @author happysnakers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MessageHandler {
    /**
     * 处理消息的优先级，如果有多个处理者都对消息感兴趣，那么调用优先级最高的处理者处理
     */
    int priority() default 1;
}