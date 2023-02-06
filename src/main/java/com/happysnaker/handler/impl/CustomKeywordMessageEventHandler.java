package com.happysnaker.handler.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.proxy.Context;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.utils.StringUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import javax.naming.CannotProceedException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 自定义关键词检测
 * @author Happysnaker
 * @description
 * @date 2022/2/22
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 0)
@SuppressWarnings("unchecked")
public class CustomKeywordMessageEventHandler extends GroupMessageEventHandler {
    public static final String REGEX_PREFIX = "#regex#";

    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        try {
            return buildMessageChainAsList(parseMiraiCode(ctx.getMessage(), event));
        } catch (CannotProceedException e) {
            e.printStackTrace();
            logError(event, StringUtil.getErrorInfoFromException(e));
            throw new  RuntimeException(e);
        }
    }

    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        Map<String, Object> keywordConfig = RobotConfig.customKeyword;
        if (getBotsAllGroupId().contains(getContent(event))) {
            return false;
        }
        String gid = getGroupId(event);
        // 优先群内回复，获取群内的 Map 定义
        if (keywordConfig.containsKey(gid)) {
            String reply = checkSimilarWordAndGetVal((Map<String, Object>) keywordConfig.get(gid), getContent(event));
            if (reply != null) {
                // 传递到 getReplyMessage 中执行
                ctx.setMessage(reply);
                return true;
            }
        }
        // 查找全局
        String reply = checkSimilarWordAndGetVal(keywordConfig, getContent(event));
        if (reply != null) {
            ctx.setMessage(reply);
            return true;
        }
        return false;
    }

    private String checkSimilarWordAndGetVal(Map<String, Object> map, String word) {
        Object ret = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            // 如果是正则表达式的话，则用正则表达式模式匹配
            if (key.startsWith(REGEX_PREFIX)) {
                key = key.replace(REGEX_PREFIX, "");
                if (Pattern.matches(key, word)) {
                    ret = val;
                    break;
                }
                continue;
            }
            int len = key.length();
            // 相似度检测
            double maximumEditDistance = (1 - RobotConfig.customKeywordSimilarity) * len;
            if (StringUtil.getEditDistance(key, word) <= maximumEditDistance) {
                ret = val;
                break;
            }
        }
        if (ret != null) {
            if (ret instanceof List) {
                List<String> list = (List<String>) ret;
                // 随机回复
                return list.get((int) (Math.random() * list.size()));
            }
            return (String) ret;
        }
        return null;
    }
}
