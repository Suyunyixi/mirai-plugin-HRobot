package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.permission.Permission;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * @author happysnaker
 * @date 2022/10/27
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1024)
public class ColorImageCommandEventHandler extends DefaultCommandEventHandlerManager {
    public static String openSe = "开启涩图";
    public static String closeSe = "关闭涩图";
    public static String updateSeStrategy = "设置涩图发送模式";


    public ColorImageCommandEventHandler() {
        super.registerKeywords(openSe).registerKeywords(closeSe).registerKeywords(updateSeStrategy);
    }

    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        String content = getPlantContent(event);
        try {
            if (content.equals(openSe)) {
                return doOpenSe(event);
            } else if (content.equals(closeSe)) {
                return doCloseSe(event);
            } else if (content.startsWith(updateSeStrategy)) {
                return doUpdateSeStrategy(event);
            }
        } catch (InsufficientPermissionsException e) {
            throw e;
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
        return null;
    }

    private List<MessageChain> doOpenSe(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        RobotConfig.colorSwitch = true;
        return buildMessageChainAsSingletonList("已开启颜色图片");
    }

    private List<MessageChain> doCloseSe(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        RobotConfig.colorSwitch = false;
        return buildMessageChainAsSingletonList("已关闭颜色图片");
    }

    private List<MessageChain> doUpdateSeStrategy(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        int mode = -1;
        try {
            mode = Integer.parseInt(getPlantContent(event).replace(updateSeStrategy, ""));
        } catch (Exception e) {
            return buildMessageChainAsSingletonList("无法识别的模式，仅支持 0、1、2、3、4 模式");
        }
        String message = "无法识别的模式，仅支持 0、1、2、3 模式";
        switch (mode) {
            case 0:
                message = "已设置模式：不发送任何消息";
                break;
            case 1:
                message = "已设置模式：上传图片，发送图片上传链接链接";
                break;
            case 2:
                message = "已设置模式：仅发送图片";
                break;
            case 3:
                message = "已设置模式：即发送图片，且发送图片链接";
                break;
            case 4:
                message = "已设置模式：仅发送图片原始链接";
                break;
        }
        RobotConfig.colorStrategy = 0 <= mode && mode <= 4 ? mode : RobotConfig.colorStrategy;
        return buildMessageChainAsSingletonList(getQuoteReply(event), message);
    }
}
