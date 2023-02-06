package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.cron.BilibiliSubscribeCronJob;
import com.happysnaker.cron.RobotCronJob;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.permission.Permission;
import com.happysnaker.utils.MapGetter;
import com.happysnaker.utils.StringUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订阅命令
 */
@MessageHandler(priority = 1024)
public class SubscribeCommandEventHandler extends DefaultCommandEventHandlerManager {
    public static final String SUBSCRIBE_COMMAND = "订阅";

    public SubscribeCommandEventHandler() {
        super.registerKeywords(SUBSCRIBE_COMMAND);
    }

    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasGroupAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("没有足够的权限");
        }
        String content = getPlantContent(event);
        List<String> strings = StringUtil.splitSpaces(content);
        if (strings.size() < 4) {
            return buildMessageChainAsSingletonList("订阅命令格式: #订阅 平台 类型 ID At成员列表");
        }
        try {
            if (strings.get(1).equals("bilibili")) {
                int type = Integer.parseInt(strings.get(2));
                if (type < 0 || type > 1) {
                    return buildMessageChainAsSingletonList("不合法的类型，仅支持 0 或 1");
                }
                long groupId = Long.parseLong(getGroupId(event));
                Map<String, Object> map = new HashMap<>();
                map.put("platform", "bilibili");
                map.put("type", type);
                map.put("key", strings.get(3));
                map.put("atMembers", strings.subList(4, strings.size()));
                map.put("pushGroup", groupId);
                RobotConfig.subscribe.add(map);
                BilibiliSubscribeCronJob cronJob = new BilibiliSubscribeCronJob(new MapGetter(map));
                RobotCronJob.addCronTask(cronJob);
            }
            return buildMessageChainAsSingletonList(String.format("订阅 %s 成功", strings.get(3)));
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
    }
}
