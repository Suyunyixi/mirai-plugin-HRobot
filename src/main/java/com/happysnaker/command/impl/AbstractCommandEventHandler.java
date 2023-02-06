package com.happysnaker.command.impl;

import com.happysnaker.command.CommandHandler;
import com.happysnaker.command.CommandHandlerManager;
import com.happysnaker.config.RobotConfig;
import com.happysnaker.proxy.Context;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.impl.GroupMessageEventHandler;
import com.happysnaker.utils.OfUtil;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * 抽象命令处理类，命令处理者需继承此类，并实现 {@link #success(MessageEvent)} 和 {@link #fail(MessageEvent, Throwable)} 方法
 *
 * @author Happysnaker
 * @description
 * @date 2022/2/15
 * @email happysnaker@foxmail.com
 */
@Slf4j
public abstract class AbstractCommandEventHandler
        extends GroupMessageEventHandler
        implements CommandHandler, CommandHandlerManager {
    /**
     * 模板方法模式，命令处理的入口
     *
     * @param event 经过 proxyContent 处理后的消息
     * @param ctx   上下文
     * @return {@link MessageChain}
     */
    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        List<MessageChain> ans;
        try {
            // 解析命令
            ans = parseCommand(event);
        } catch (CanNotParseCommandException e) {
            // fail callback
            fail(event, e);
            return OfUtil.ofList(buildMessageChain(getQuoteReply(event),
                    "命令解析出错，错误原因：" + e.getMessage()));
        } catch (InsufficientPermissionsException e) {
            // fail callback
            fail(event, "权限不足：" + e.getMessage());
            return OfUtil.ofList(buildMessageChain(getQuoteReply(event),
                    "对不起，您没有足够的权限，说明：" + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // fail callback
            fail(event, e);
            return OfUtil.ofList(buildMessageChain(getQuoteReply(event),
                    "异常错误，错误原因：" + e.getMessage()));
        }
        if (ans != null) {
            // success callback
            success(event);
        }
        return ans;
    }

    /**
     * 返回去除命令前缀的纯文本消息
     *
     * @param event {@link MessageEvent}
     * @return
     */
    @Override
    public String getPlantContent(MessageEvent event) {
        return super.getPlantContent(event).replaceFirst(RobotConfig.commandPrefix, "").trim();
    }


    /**
     * 只有当消息包含命令前缀时才会返回 true，子类允许拥有自己的逻辑，但子类在执行具体逻辑前必须先询问此类的 shouldHandle，如果此类 shouldHandle 返回 false，则子类应当立马返回 false<br/><br/>
     * <pre><code>
     *     public boolean shouldHandle(MessageEvent event) {
     *         if (!super.shouldHandle(event)) {
     *             return false;
     *         }
     *         //具体的逻辑
     *     }</code></pre>
     *
     * @param event
     * @return
     */
    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        return (super.getPlantContent(event).startsWith(RobotConfig.commandPrefix));
    }


    /**
     * 解析命令，子类必须要实现的方法
     *
     * @see CommandHandler#parseCommand
     */
    @Override
    public abstract List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException;


}
