package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.permission.Permission;
import com.happysnaker.utils.OfUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Happysnaker
 * @description
 * @date 2022/7/2
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1024)
public class GroupManageCommandEventHandler extends DefaultCommandEventHandlerManager {
    @Deprecated
    public static final String ENABLE_AUTO_APPROVE = "开启自动审批";
    @Deprecated
    public static final String DISABLE_AUTO_APPROVE = "关闭自动审批";
    public static final String ENABLE_SENSITIVE_WORD_DETECTION = "开启敏感词检测";
    public static final String DISABLE_SENSITIVE_WORD_DETECTION = "关闭敏感词检测";

    public GroupManageCommandEventHandler() {
        registerKeywords(ENABLE_AUTO_APPROVE);
        registerKeywords(DISABLE_AUTO_APPROVE);
        registerKeywords(ENABLE_SENSITIVE_WORD_DETECTION);
        registerKeywords(DISABLE_SENSITIVE_WORD_DETECTION);
    }

    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("权限不足");
        }
        try {
            String content = super.getPlantContent(event);
            String gid = getGroupId(event);
            if (content.startsWith(DISABLE_AUTO_APPROVE)) {
                List<Map<String, String>> copy = new ArrayList<>(RobotConfig.autoApproval);
                for (Map<String, String> map : copy) {
                    if (map.containsKey(getGroupId(event))) {
                        RobotConfig.autoApproval.remove(map);
                    }
                }
                return buildMessageChainAsSingletonList("关闭自动审批成功");
            } else if (content.startsWith(ENABLE_AUTO_APPROVE)) {
                String s = content.replaceFirst(ENABLE_AUTO_APPROVE, "").trim();
                RobotConfig.autoApproval.add(OfUtil.ofMap(gid, s));
                return buildMessageChainAsSingletonList("开启自动审批成功，审批验证消息：" + s);
            } else if (content.startsWith(ENABLE_SENSITIVE_WORD_DETECTION)) {
                if (!RobotConfig.enableSensitiveWordDetection.contains(gid)) {
                    RobotConfig.enableSensitiveWordDetection.add(gid);
                }
                return buildMessageChainAsSingletonList("本群已开启敏感词检测");
            } else if (content.startsWith(DISABLE_SENSITIVE_WORD_DETECTION)) {
                RobotConfig.enableSensitiveWordDetection.remove(gid);
                return buildMessageChainAsSingletonList("本群已关闭敏感词检测");
            }
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
        return null;
    }
}
