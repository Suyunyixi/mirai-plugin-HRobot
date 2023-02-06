package com.happysnaker.api;

import com.alibaba.fastjson.JSONObject;
import com.happysnaker.entry.BilibiliDynamic;
import com.happysnaker.utils.IOUtil;
import com.happysnaker.utils.MapGetter;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class BilibiliApi {
    /**
     * 查询动态 API
     */
    public static final String SPACE_API = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space?offset=&host_mid=$UID&timezone_offset=-480";
    /**
     * 查询番剧 API
     */
    public static final String FAN_API = "https://api.bilibili.com/pgc/view/web/season?season_id=$SID";

    /**
     * 获取最近更新的番剧
     *
     * @param sid 番剧 id
     * @return
     * @throws IOException
     */
    public static BilibiliDynamic getLatestFanDrama(String sid) throws IOException {
        URL url = new URL(FAN_API.replace("$SID", sid));
        MapGetter getter = IOUtil.sendAndGetResponseMapGetter(url, "GET", null, null);
        MapGetter result = getter.getMapGetter("result");
        List<MapGetter> eps = result.getMapGetterList("episodes");
        eps.sort((a, b) -> Long.compare(b.getLong("pub_time"), a.getLong("pub_time")));
        if (eps.size() == 0) {
            return null;
        }
        MapGetter ep = eps.get(0);
        return new BilibiliDynamic()
                .setAuthName(result.getString("season_title"))
                .setId(result.getString("season_id", true))
                .setPubAction(ep.getString("share_copy"))
                .setPubTime(ep.getLong("pub_time") * 1000L)
                .setCover(ep.getString("cover"))
                .setFace(result.getString("cover"))
                .setDesc(ep.getString("long_title"))
                .setJumpUrl(ep.getString("short_link"));
    }

    /**
     * 获取一条最新的动态
     *
     * @param uid Up id
     * @return
     */
    public static BilibiliDynamic getLatestDynamic(String uid) throws IOException {
        List<BilibiliDynamic> dynamics = getDynamics(uid);
        if (dynamics.size() == 0) {
            return null;
        }
        dynamics.sort((a, b) -> Long.compare(b.pubTime, a.pubTime));
        return dynamics.get(0);
    }

    /**
     * 获取最新的几条动态
     *
     * @param uid Up id
     * @return
     */
    public static List<BilibiliDynamic> getDynamics(String uid) throws IOException {
        List<BilibiliDynamic> dynamics = new ArrayList<>();
        URL url = new URL(SPACE_API.replace("$UID", uid));
        MapGetter mapGetter = IOUtil.sendAndGetResponseMapGetter(url, "GET", null, null);
        for (MapGetter getter : mapGetter.getMapGetter("data").getMapGetterList("items")) {
            try {
                MapGetter author = getter.getMapGetter("modules").getMapGetter("module_author");
                BilibiliDynamic dynamic = new BilibiliDynamic();
                dynamics.add(dynamic
                        .setAuthName(author.getString("name"))
                        .setFace(author.getString("face"))
                        .setPubTime(author.getLong("pub_ts") * 1000L)
                        .setPubAction(author.getString("pub_action")));

                MapGetter dynamicMajor = getter.getMapGetter("modules")
                        .getMapGetter("module_dynamic")
                        .getMapGetter("major");

                switch (getter.getString("type")) {
                    case "DYNAMIC_TYPE_DRAW":
                        MapGetter draw = dynamicMajor.getMapGetter("draw");
                        MapGetter topic = getter.getMapGetter("modules")
                                .getMapGetter("module_dynamic")
                                .getMapGetter("topic");
                        String id_str = getter.getString("id_str");
                        if (topic == null) {
                            topic = new MapGetter(new HashMap());
                        }
                        List<String> images = new ArrayList<>();
                        dynamic.setType(0)
                                .setId(draw.getString("id", true))
                                .setImages(images)
                                .setDesc(topic.getStringOrDefault("name", "暂无话题信息"))
                                .setJumpUrl(String.format("https://t.bilibili.com/%s", id_str))
                                .setPubAction("发布了图文动态");
                        for (MapGetter item : draw.getMapGetterList("items")) {
                            images.add(item.getString("src"));
                        }
                        break;
                    case "DYNAMIC_TYPE_WORD":
                        MapGetter word_topic = getter.getMapGetter("modules")
                                .getMapGetter("module_dynamic")
                                .getMapGetter("topic");
                        String word_id_str = getter.getString("id_str");
                        if (word_topic == null) {
                            word_topic = new MapGetter(new HashMap());
                        }
                        dynamic.setType(0)
                                .setId("")
                                .setDesc(word_topic.getStringOrDefault("name", "暂无话题信息"))
                                .setJumpUrl(String.format("https://t.bilibili.com/%s", word_id_str))
                                .setPubAction("发布了文字动态");
                        break;
                    case "DYNAMIC_TYPE_ARTICLE":
                        MapGetter article = dynamicMajor.getMapGetter("article");
                        dynamic.setType(1)
                                .setCover(article.getItemFromList("covers", 0, String.class))
                                .setId(article.getString("id", true))
                                .setDesc(article.getString("desc"))
                                .setJumpUrl(article.getString("jump_url"));
                        break;
                    case "DYNAMIC_TYPE_AV":
                        MapGetter archive = dynamicMajor.getMapGetter("archive");
                        dynamic.setType(2)
                                .setId(archive.getString("bvid", true))
                                .setDesc(archive.getString("desc"))
                                .setJumpUrl(archive.getString("jump_url"));
                        break;
                    case "DYNAMIC_TYPE_LIVE_RCMD":
                        MapGetter liveRcmd = dynamicMajor.getMapGetter("live_rcmd");
                        MapGetter liveInfo = new MapGetter(JSONObject.parseObject(
                                liveRcmd.getString("content"), Map.class))
                                .getMapGetter("live_play_info");
                        dynamic.setType(3)
                                .setId(liveInfo.getString("live_od"))
                                .setCover(liveInfo.getString("cover"))
                                .setJumpUrl(liveInfo.getString("link"))
                                .setDesc(liveInfo.getString("title"));
                        break;
                    case "DYNAMIC_TYPE_FORWARD":
                        MapGetter desc = getter.getMapGetter("modules")
                                .getMapGetter("module_dynamic")
                                .getMapGetter("desc");
                        String self_id_str = getter.getString("id_str");
                        dynamic.setType(4)
                                .setId(self_id_str)
                                .setDesc(desc.getString("text"))
                                .setJumpUrl(String.format("https://t.bilibili.com/%s", self_id_str))
                                .setPubAction("转发了动态");
                        break;
                    default:
                        // 未知类型，不检查
                        dynamics.remove(dynamics.size() - 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dynamics;
    }
}
