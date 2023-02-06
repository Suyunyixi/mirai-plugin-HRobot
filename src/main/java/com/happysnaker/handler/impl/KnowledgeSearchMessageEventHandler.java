package com.happysnaker.handler.impl;

import com.happysnaker.api.BaiKeApi;
import com.happysnaker.proxy.Context;
import com.happysnaker.exception.CanNotSendMessageException;
import com.happysnaker.exception.FileUploadException;
import com.happysnaker.handler.MessageHandler;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Happysnaker
 * @description
 * @date 2022/2/26
 * @email happysnaker@foxmail.com
 */
@MessageHandler
public class KnowledgeSearchMessageEventHandler extends GroupMessageEventHandler {
    public static final String BAIDU_BAIKE = "百度百科";

    private final HashSet<String> keywords = new HashSet<>();

    public KnowledgeSearchMessageEventHandler() {
        keywords.add(BAIDU_BAIKE);
    }


    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        try {
            return parseBaidu(event);
        } catch (Exception e) {
            logError(event, e);
            e.printStackTrace();
            return buildMessageChainAsSingletonList("发生了错误，错误原因：" + e.getMessage());
        }
    }

    protected List<MessageChain> parseBaidu(MessageEvent event) throws MalformedURLException, FileUploadException, CanNotSendMessageException {
        String content = getPlantContent(event).replace(BAIDU_BAIKE, "");
        Map<String, String> map = BaiKeApi.search(content);
        if (map == null) {
            return buildMessageChainAsSingletonList(getQuoteReply(event), "检索失败，换个关键词试试吧");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("标题：");
        sb.append(map.get("title")).append("\n");
        sb.append("描述：");
        sb.append(map.get("desc")).append("\n");
        sb.append(map.get("content")).append("\n");
        if (map.get("image") != null) {
            return buildMessageChainAsSingletonList(sb.toString(), uploadImage(event, new URL(map.get("image"))));
        }
        return buildMessageChainAsSingletonList(sb.toString());
    }

    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        return startWithKeywords(event, keywords);
    }
}
