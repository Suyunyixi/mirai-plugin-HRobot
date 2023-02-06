package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.permission.Permission;
import com.happysnaker.utils.OfUtil;
import com.happysnaker.utils.Pair;
import com.happysnaker.utils.RobotUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.Collections;
import java.util.List;

/**
 * 添加定时任务命令
 * @author Happysnaker
 * @description
 * @date 2022/7/3
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1024)
public class PeriodCommandEventHandler extends DefaultCommandEventHandlerManager {
    public static final String ADD_PERIOD_TASK = "设置定时任务";

    public PeriodCommandEventHandler() {
        registerKeywords(ADD_PERIOD_TASK);
    }

    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("权限不足");
        }
        try {
            // 不能获取纯文本，要获取 mirai 编码
            String content = getContent(event).replace(RobotConfig.commandPrefix + ADD_PERIOD_TASK, "").trim();
            Pair<String, String> keyVal = getKeyVal(content);
            String[] split = keyVal.getKey().split("-");
            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);
            int count = Integer.parseInt(split[2]);
            int image = split.length == 4 ? Integer.parseInt(split[3]) : 0;
            // 如果永久任务，则允许调用 #保存配置 保存
            RobotConfig.periodicTask.add(
                    OfUtil.ofMap(OfUtil.ofList("hour", "minute", "groupId", "count", "image", "content"),
                            OfUtil.ofList(hour, minute, getGroupId(event), 0, image == 1, keyVal.getValue())
                    )
            );
            MessageChain message = parseMiraiCode(keyVal.getValue(), event);
            Contact contact = event.getSubject();
            RobotUtil.submitSendRandomMsgTask(hour, minute,
                    count <= 0 ? Integer.MAX_VALUE : count,
                    Collections.singletonList(getContent(message)), contact);
            return buildMessageChainAsSingletonList("任务提交成功！");
        } catch (Exception e) {
            throw new CanNotParseCommandException(e.getMessage());
        }
    }

    /**
     * 提取时间及回复
     *
     * @param content
     * @return
     * @throws CanNotParseCommandException
     */
    private Pair<String, String> getKeyVal(String content) throws CanNotParseCommandException {
        int l = content.indexOf('{');
        int r = content.indexOf('}');
        if (l == -1 || r == -1) {
            throw new CanNotParseCommandException("未检测到关键字，请以{}包裹关键字，日期格式为 hour-minute-count-image");
        }
        String keyword = content.substring(l + 1, r);
        String val = content.replace("{" + keyword + "}", "");
        return new Pair<>(keyword.trim(), val.trim());
    }
}
