package com.happysnaker.command;

import com.happysnaker.utils.StringUtil;
import net.mamoe.mirai.event.events.MessageEvent;

/**
 * 命令处理器管理者
 *
 * @author Happysnaker
 * @description
 * @date 2022/2/23
 * @email happysnaker@foxmail.com
 */
public interface CommandHandlerManager {
    /**
     * 当命名事件执行成功时需要做的事
     *
     * @param event
     */
    void success(MessageEvent event);

    /**
     * 当命名事件执行失败时需要做的事
     *
     * @param event
     */
    void fail(MessageEvent event, String errorMsg);

    /**
     * 当命名事件执行失败时需要做的事
     *
     * @param event
     */
    default void fail(MessageEvent event, Throwable e) {
        fail(event, StringUtil.getErrorInfoFromException(e));
    }
}
