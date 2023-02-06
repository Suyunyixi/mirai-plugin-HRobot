package com.happysnaker.handler.impl;

import com.happysnaker.api.MiguApi;
import com.happysnaker.api.TongZhongApi;
import com.happysnaker.proxy.Context;
import com.happysnaker.handler.MessageHandler;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 音乐分享
 *
 * @author Happysnaker
 * @description
 * @date 2022/1/29
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1)
public class MusicShareMessageEventHandler extends GroupMessageEventHandler {
    private final Set<String> keywords = new HashSet<>();
    private final String MUSIC_KEYWORD = "音乐";
    @Deprecated
    private final String MIGU_MUSIC_KEYWORD = "咪咕";

    public MusicShareMessageEventHandler() {
        keywords.add(MUSIC_KEYWORD);
        keywords.add(MIGU_MUSIC_KEYWORD);
    }



    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        String content = getPlantContent(event);
        try {
            if (content.startsWith(MUSIC_KEYWORD)) {
                return music(event);
            } else if (content.startsWith(MIGU_MUSIC_KEYWORD)) {
                return miguMusic(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logError(event, e);
            return buildMessageChainAsSingletonList("出错了呢，换首歌试试吧！\n" + e.getMessage());
        }
        return null;
    }

    @Deprecated
    protected List<MessageChain> miguMusic(MessageEvent event) throws IOException {
        String content = getPlantContent(event);
        String name = content.substring(MIGU_MUSIC_KEYWORD.length()).trim();
        return buildMessageChainAsSingletonList(MiguApi.search(name));
    }

    protected List<MessageChain> music(MessageEvent event) throws IOException {
        List<MessageChain> ans = new ArrayList<>();
        String content = getPlantContent(event);
        // ffmpeg -i 稻香.mp3 -f wav test.wav
        // ffmpeg -i test.wav -ar 8000 -ab 12.2k -ac 1 test.amr
        String name = content.substring(MUSIC_KEYWORD.length()).trim();
        try {
            MusicShare music = TongZhongApi.getSongUrl(name);
            if (music != null) {
                ans.add(new MessageChainBuilder().append(music).build());
            } else {
                ans.add(buildMessageChain("查无此歌"));
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return ans;
    }

//    protected

    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        return startWithKeywords(event, keywords);
    }
}
