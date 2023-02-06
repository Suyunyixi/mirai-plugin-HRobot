package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.permission.Permission;
import com.happysnaker.starter.HRobotStarter;
import com.happysnaker.config.ConfigManager;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * 机器人整体性的配置
 * @author Happysnaker
 * @description
 * @date 2022/2/22
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1024)
public class SystemCommandEventHandler extends DefaultCommandEventHandlerManager {
    public static String reloadConfigCommand = "重载配置";
    public static String saveConfigCommand = "保存配置";
    public static String showConfigStatusCommand = "查看配置状态";
    public static String showConfigLogCommand = "查看配置日志";
    public static String shutdown = "关机";
    public static String boot = "开机";


    public SystemCommandEventHandler() {
        super.registerKeywords(saveConfigCommand);
        super.registerKeywords(showConfigStatusCommand);
        super.registerKeywords(showConfigLogCommand);
        super.registerKeywords(reloadConfigCommand);
        super.registerKeywords(shutdown).registerKeywords(boot);
    }


    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        String content = getPlantContent(event);
        try {
            if (content.equals(saveConfigCommand)) {
                return doSaveConfig(event);
            } else if (content.equals(showConfigStatusCommand)) {
                return doShowConfigStatusCommand(event);
            } else if (content.equals(showConfigLogCommand)) {
                return doShowConfigLogCommand(event);
            } else if (content.equals(reloadConfigCommand)) {
                return doReloadConfigCommand(event);
            } else if (content.equals(shutdown)) {
                return doShutDown(event);
            } else if (content.equals(boot)) {
                return doBoot(event);
            }
        } catch (InsufficientPermissionsException e) {
            throw e;
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
        return null;
    }



    private List<MessageChain> doShutDown(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasSuperAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        RobotConfig.enableRobot = false;
        return buildMessageChainAsSingletonList("走啦，期待下次见面!");
    }

    private List<MessageChain> doBoot(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasSuperAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        RobotConfig.enableRobot = true;
        return buildMessageChainAsSingletonList("我又回来了！");
    }


    public List<MessageChain> doReloadConfigCommand(MessageEvent event) throws InsufficientPermissionsException, CanNotParseCommandException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        try {
            HRobotStarter.initRobotConfig(null);
            return buildMessageChainAsSingletonList("重载成功!");
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
    }

    public List<MessageChain> doShowConfigLogCommand(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        String log = checkLogStatus();
        return log == null ? buildMessageChainAsSingletonList("暂无日志") : buildMessageChainAsSingletonList(log);
    }

    public List<MessageChain> doShowConfigStatusCommand(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        int num = getSuccessNum();
        return num == 0 ? buildMessageChainAsSingletonList("The command tree is clean now") : buildMessageChainAsSingletonList("There are " + num + " command successfully be done，it may be dirty now, try to check the log!");
    }

    /**
     * 保存配置，命令格式 "#保存配置"，命令需要提供 bot 管理员权限
     *
     * @param event
     * @return
     */
    public List<MessageChain> doSaveConfig(MessageEvent event) throws InsufficientPermissionsException, CanNotParseCommandException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("您的权限不足，无法执行此操作");
        }
        try {
            ConfigManager.writeConfig();
            flush();
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
        return buildMessageChainAsSingletonList("配置保存成功！");
    }
}
