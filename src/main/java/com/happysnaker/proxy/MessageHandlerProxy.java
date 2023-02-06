package com.happysnaker.proxy;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.handler.MessageEventHandler;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.handler.intercept.Interceptor;
import com.happysnaker.handler.intercept.Intercept;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Happysnaker
 * @description
 * @date 2022/1/16
 * @email happysnaker@foxmail.com
 */
public class MessageHandlerProxy implements MessageEventHandler {
    // 扫描所有的类
    public final String packageName = "com.happysnaker";
    private final List<MessageEventHandler> handlers = new ArrayList<>();
    private final List<Interceptor> interceptors = new ArrayList<>();




    /**
     * 扫描 jar 包下的所有类，并添加 MessageHandler 和
     */
    public MessageHandlerProxy() throws Exception {
        Reflections reflections = new Reflections("com.happysnaker");
        Set<Class<?>> handlerCs = reflections.getTypesAnnotatedWith(MessageHandler.class);
        Set<Class<?>> inspectCs = reflections.getTypesAnnotatedWith(Intercept.class);


        for (Class<?> c : handlerCs) {
            if (Modifier.isAbstract(c.getModifiers())) {
                continue;
            }
            try {
                handlers.add((MessageEventHandler) c.getConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Class<?> c : inspectCs) {
            try {
                interceptors.add((Interceptor) c.getConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
                throw  e;
            }
        }

        handlers.sort((h1, h2) -> {
            MessageHandler a1 = h1.getClass().getAnnotation(MessageHandler.class);
            MessageHandler a2 = h2.getClass().getAnnotation(MessageHandler.class);
            // 按照高 ->低排序
            return a2.priority() - a1.priority();
        });

        interceptors.sort((i1, i2) -> {
            Intercept a1 = i1.getClass().getAnnotation(Intercept.class);
            Intercept a2 = i2.getClass().getAnnotation(Intercept.class);
            // 按照高 ->低排序
            return a2.order() - a1.order();
        });
        RobotConfig.logger.info("HRobot handlers and filters loading finished, total " + handlers.size() + " handlers, " + interceptors.size() + " inspectors.");
    }


    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        if (ctx == null) {
            ctx = new Context(handlers, interceptors);
        }
        ctx. execute(event);
        return null;
    }

    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        for (Interceptor filter : interceptors) {
            if (filter.interceptBefore(event)) {
                // 如果事件被指明过滤，则不回复
                return false;
            }
        }
        return true;
    }
}
