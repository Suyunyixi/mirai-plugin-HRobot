package com.happysnaker.handler.intercept.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.handler.intercept.Intercept;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;

/**
 * 前置拦截器，控制 Robot 的开关，检查 include 和 exclude 的事件
 * @author Happysnaker
 * @description
 * @date 2022/1/30
 * @email happysnaker@foxmail.com
 */
@Intercept
public class BaseBeforeInterceptor extends AdaptInterceptor {


    /**
     * 返回真则拦截该事件
     * @param event
     * @return
     */
    @Override
    public boolean interceptBefore(MessageEvent event) {
        // 如果如何，放行开机命令
        if (getContent(event).equals(RobotConfig.commandPrefix + "开机")) {
            return false;
        }
        System.out.println("RobotConfig.enableRobot = " + RobotConfig.enableRobot);
        if (!RobotConfig.enableRobot)               return true;
        if (!(event instanceof GroupMessageEvent))  return false;
        String gid = String.valueOf(((GroupMessageEvent) event).getGroup().getId());
        if (RobotConfig.include.isEmpty()) {
            return RobotConfig.exclude.contains(gid);
        }
        return !RobotConfig.include.contains(gid);
    }
}
