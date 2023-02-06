package com.happysnaker.handler.impl;


import com.happysnaker.api.BingApi;
import com.happysnaker.api.PixivApi;
import com.happysnaker.config.RobotConfig;
import com.happysnaker.proxy.Context;
import com.happysnaker.exception.CanNotSendMessageException;
import com.happysnaker.exception.FileUploadException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.utils.IOUtil;
import com.happysnaker.utils.StringUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * 分享一些图片，你懂的
 *
 * @author Happysnaker
 * @description
 * @date 2022/1/20
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1)
public class ImageShareMessageEventHandler extends GroupMessageEventHandler {
    public final String chickenSoup = "鸡汤";
    public final String mysteriousImage = "神秘代码";
    public final String landscapeImage = "风景图";
    public final String beautifulImage = "美图";
    public final String seImage = "涩图";
    public final String seImagePlus = "高清涩图";
    public final String beautifulImageUrl = PixivApi.beautifulImageUrl;
    public final String chickenSoupUrl = PixivApi.chickenSoupUrl;
    public final String duChickenSoupUrl = PixivApi.duChickenSoupUrl;

    @Deprecated
    public final String pixivSearchApi = PixivApi.pixivSearchApi;

    private final Set<String> keywords = new HashSet<>();

    public ImageShareMessageEventHandler() {
        keywords.add(chickenSoup);
        keywords.add(mysteriousImage);
        keywords.add(landscapeImage);
        keywords.add(beautifulImage);
        keywords.add(seImage);
        keywords.add(seImagePlus);
    }

    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        String content = getPlantContent(event);
        List<MessageChain> ans = new ArrayList<>();
        try {
            // 鸡汤
            if (content.contains(chickenSoup)) {
                ans.add(doParseChickenSoup(event));
            }

            // 神秘代码
            if (content.startsWith(mysteriousImage)) {
                // 需要分割 tag
                List<String> tags = getTags(content, mysteriousImage);
                ans.add(doParseMysteriousImage(event, tags));
            }

            // 涩图 或 高清涩图
            if (content.startsWith(seImage) || content.startsWith(seImagePlus)) {
                String tem = content.startsWith(seImage) ? seImage : seImagePlus;
                //检测到涩图需要自动撤回，因此自定义逻辑处理，处理完返回 null，不需要抽象类的逻辑
                doParseSeImageAndSend(event, getTags(content, tem), tem.equals(seImagePlus));
                return null;
            }


            // 美图
            if (content.contains(beautifulImage)) {
                ans.add(doParseBeautifulImage(event));
            }

            // 风景图
            if (content.contains(landscapeImage)) {
                ans.add(doParseLandscapeImage(event));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logError(event, e);
            ans.add(new MessageChainBuilder().append("意外地失去了与地球上的通信...\n错误原因：").append(e.getMessage()).build());
        }
        return ans;
    }

    /**
     * 根据 plantContent 获取 tag
     *
     * @param content plantContent
     * @param tem     关键词
     * @return 返回去除关键词后分割空格检索出的 tags
     */
    private List<String> getTags(String content, String tem) {
        List<String> tags = new ArrayList<>();
        List<String> strings = StringUtil.splitSpaces(content.replace(tem, ""));
        for (String s : strings) {
            if (!s.isEmpty() && !s.equals(tem)) {
                tags.add(s.trim());
            }
        }
        return tags;
    }

    /**
     * 获取风景图
     *
     * @param event
     * @return
     * @throws MalformedURLException
     * @throws FileUploadException
     */
    private MessageChain doParseLandscapeImage(MessageEvent event) throws MalformedURLException, FileUploadException {
        return new MessageChainBuilder()
                .append(uploadImage(event, new URL(BingApi.getRandomImageUrl())))
                .build();
    }

    /**
     * 获取美图
     *
     * @param event
     * @return
     * @throws MalformedURLException
     * @throws FileUploadException
     */
    private MessageChain doParseBeautifulImage(MessageEvent event) throws MalformedURLException, FileUploadException {
        return new MessageChainBuilder()
                .append(uploadImage(event, new URL(beautifulImageUrl)))
                .build();
    }

    /**
     * 获取涩图
     *
     * @param event
     * @param tags
     * @param isPlus 是否是高清涩图
     * @throws IOException
     * @throws FileUploadException
     */
    private void doParseSeImageAndSend(MessageEvent event, List<String> tags, boolean isPlus) throws IOException, FileUploadException, CanNotSendMessageException {
        if (!RobotConfig.colorSwitch) {
            return;
        }
        String imgUrl = PixivApi.searchImage(tags, true, !isPlus);
        if (StringUtil.isNullOrEmpty(imgUrl)) {
            buildMessageChain("查无此图");
            return;
        }
        info(String.format("R18 color image url: %s", imgUrl));

        if (RobotConfig.colorStrategy == 4) {
            sendMsg(buildMessageChain(getQuoteReply(event), "颜色图片原始链接：" + imgUrl), event);
            return;
        }

        // 可访问的地址，腾讯很恶心，原地址可能会被腾讯视为恶意网站，所以这里上传一下服务器，用腾讯内部地址
        Image image = uploadImage(event, new URL(imgUrl));
        String reachableImageUrl = Image.queryUrl(image);
        if (StringUtil.isNullOrEmpty(reachableImageUrl)) {
            reachableImageUrl = imgUrl;
        }

        switch (RobotConfig.colorStrategy) {
            case 0:
                return;
            case 1:
                sendMsg(buildMessageChain(getQuoteReply(event), "颜色图片链接：" + reachableImageUrl), event);
                break;
            case 2:
                sendMsg(buildMessageChain(image), event, RobotConfig.pictureWithdrawalTime * 1000L);
                break;
            case 3:
                sendMsg(buildMessageChain(getQuoteReply(event), "颜色图片链接：" + reachableImageUrl), event);
                sendMsg(buildMessageChain(image), event, RobotConfig.pictureWithdrawalTime * 1000L);
                break;
        }
    }


    /**
     * 获取神秘代码
     *
     * @param event
     * @param tags
     * @return
     * @throws IOException
     * @throws FileUploadException
     */
    private MessageChain doParseMysteriousImage(MessageEvent event, List<String> tags) throws IOException, FileUploadException {
        String imgUrl = PixivApi.searchImage(tags, false, false);
        if (StringUtil.isNullOrEmpty(imgUrl)) {
            return buildMessageChain("查无此图");
        }
        info(String.format("Not r18 color image url: %s", imgUrl));
        return new MessageChainBuilder()
                .append(uploadImage(event, new URL(imgUrl))).build();
    }

    /**
     * 获取鸡汤
     *
     * @param event
     * @return
     * @throws IOException
     * @throws FileUploadException
     */
    private MessageChain doParseChickenSoup(MessageEvent event) throws IOException, FileUploadException {
        String chickenSoupUrl = this.chickenSoupUrl;
        // 恶搞，生成毒鸡汤
        if (Math.random() < RobotConfig.duChickenSoupProbability) {
            chickenSoupUrl = this.duChickenSoupUrl;
        }
        Map<String, Object> map = IOUtil.sendAndGetResponseMap(new URL(chickenSoupUrl), "GET", null, null);
        String text = (String) ((Map<?, ?>) (map.get("data"))).get("text");
        return new MessageChainBuilder()
                .append(text)
                .append(uploadImage(event, new URL(beautifulImageUrl)))
                .build();
    }

    /**
     * 不需要 @ 机器人，检测到以关键词开头的事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        return startWithKeywords(event, keywords);
    }
}
