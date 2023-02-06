package com.happysnaker.handler.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.proxy.Context;
import com.happysnaker.exception.FileUploadException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.utils.*;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 坎公报表
 *
 * @author Happysnaker
 * @description
 * @date 2022/1/16
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1)
@Deprecated // 代码待重构
public class GtReportMessageEventHandler extends GroupMessageEventHandler {
    public static final String BATTLE_REPORT = "会战报表";
    public static final String FRONTLINE_REPORTING = "前线报道";
    public static final String BATTLE_STATISTICS = "会战统计";
    public static final String WHO_NOT_SHOOT1 = "谁未出刀";
    public static final String WHO_NOT_SHOOT2 = "谁没出刀";
    public static final String URGE_KNIFE = "催刀";
    public static final String URGE_KNIFE_ALL = "一键催刀";
    public static final String CHECK_KNIFE = "查刀";

    private static final String BATTLE_REPORT_URL = "https://www.bigfun.cn/api/feweb?target=kan-gong-guild-report%2Fa&date=";
    public static final String FRONTLINE_REPORTING_URL = "https://www.bigfun.cn/api/feweb?target=kan-gong-guild-boss-info%2Fa";
    public static final String GET_MEMBER_URL = "https://www.bigfun.cn/api/feweb?target=kan-gong-guild-log-filter/a";

    private Set<String> keywords;

    public static final int TOTAL_COUNT = 3;
    public static final String DATA = "data";
    public static final String USER_NAME = "user_name";
    public static final String DAMAGE_TOTAL = "damage_total";
    public static final String DAMAGE_LIST = "damage_list";
    public static final String DAMAGE = "damage";
    public static final String BOSS_NAME = "boss_name";
    public static final String IS_KILL = "is_kill";
    public static final String BOSS = "boss";
    public static final String LEVEL = "level";
    public static final String TOTAL_HP = "total_hp";
    public static final String REMAIN_HP = "remain_hp";
    public static final String NAME = "name";
    public static final String TYPE = "elemental_type_cn";


    public GtReportMessageEventHandler() {
        keywords = new HashSet<>(16);
        keywords.add(BATTLE_REPORT);
        keywords.add(FRONTLINE_REPORTING);
        keywords.add(BATTLE_STATISTICS);
        keywords.add(WHO_NOT_SHOOT1);
        keywords.add(WHO_NOT_SHOOT2);
        keywords.add(URGE_KNIFE);
        keywords.add(URGE_KNIFE_ALL);
        keywords.add(CHECK_KNIFE);
    }

    /**
     * 回复消息的接口
     *
     * @param event 经过 proxyContent 处理后的消息
     * @param ctx
     * @return
     */
    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        String cookie = getCookie(getGroupId(event));
        if (cookie != null && !cookie.isEmpty()) {
            String content = getPlantContent(event);
            try {
                // 会战报表
                if (content.contains(BATTLE_REPORT)) {
                    return OfUtil.ofList(doParseBattleReport(cookie));
                }
                // 前线报道
                else if (content.contains(FRONTLINE_REPORTING)) {
                    return OfUtil.ofList(doParseFrontlineReporting(cookie));
                }
                // 会战统计
                else if (content.contains(BATTLE_STATISTICS)) {
                    // 整体统计
                    if (content.equals(BATTLE_STATISTICS)) {
                        return OfUtil.ofList(doParseBattleStatistics(cookie));
                    }
                    // 对玩家统计
                    return OfUtil.ofList(doParseBattleStatistics(cookie, content.replace(BATTLE_STATISTICS, "").trim(), event));
                }
                // 谁没出刀
                else if (content.contains(WHO_NOT_SHOOT1) || content.contains(WHO_NOT_SHOOT2)) {
                    return OfUtil.ofList(doParseCheck(cookie, event));
                }
                // 一键催刀
                else if (content.contains(URGE_KNIFE_ALL)) {
                    // 一键催到会催未出满刀的所有人
                    Pair<Set<String>, Set<String>> pair = getNotDoPeople(cookie, event);
                    Set<String> param = pair.getKey();
                    param.addAll(pair.getValue());
                    return OfUtil.ofList(doUrge(event, param));
                }
                // 催刀
                else if (content.contains(URGE_KNIFE)) {
                    return OfUtil.ofList(doUrge(event, getNotDoPeople(cookie, event).getKey()));
                }
                // 查刀
                else if (content.contains(CHECK_KNIFE)) {
                    return check(event, cookie);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logError(event, e);
                return OfUtil.ofList(new MessageChainBuilder().append("发生了一条意料之外的错误").build());
            }
        }
        return OfUtil.ofList(new MessageChainBuilder().append("该群暂未配置相关信息").build());
    }

    public List<MessageChain> check(MessageEvent event, String cookie) throws IOException, FileUploadException {
        String content = getPlantContent(event).replace(CHECK_KNIFE, "").trim();

        boolean testLine = false;
        if (content.startsWith("折线图")) {
            content = content.replace("折线图", "").trim();
            testLine = true;
        }

        final List<String> ms = StringUtil.splitSpaces(content);
        Map<String, Object> msg = null;
        List<String> dates = (List<String>) ((Map) IOUtil.sendAndGetResponseMap(new URL(GET_MEMBER_URL), "GET", getHeaders(cookie), null).get("data")).get("date");

        List<Pair<String, List<Pair<String, Double>>>> datasets = new ArrayList<>();
        Map<String, List<Pair<String, Double>>> map = new HashMap<>();

        Collections.reverse(dates);
        for (String date : dates) {
            msg = IOUtil.sendAndGetResponseMap(new URL(BATTLE_REPORT_URL + date), "GET", getHeaders(cookie), null);
            List<Map<String, Object>> datas = (List<Map<String, Object>>) msg.getOrDefault(DATA, new ArrayList<>());

            Set<String> doneSet = new HashSet<>();
            for (Map<String, Object> data : datas) {
                String username = (String) data.get(USER_NAME);
                doneSet.add(username);
                List damageList = (List) data.get(DAMAGE_LIST);
                map.putIfAbsent(username, new ArrayList<>());
                if (damageList != null) {
                    if (damageList.size() == TOTAL_COUNT) {
                        map.get(username).add(Pair.of(date, 3.0));
                    } else {
                        // 出了但是未出满的仔
                        map.get(username).
                                add(Pair.of(date, (double) damageList.size()));
                    }
                } else {
                    // 一刀没出
                    map.get(username).add(Pair.of(date, 0.1));
                }
            }

            for (String m : ms) {
                map.putIfAbsent(m, new ArrayList<>());
                if (!doneSet.contains(m)) {
                    // 一刀没出
                    map.get(m).add(Pair.of(date, 0.1));
                }
            }
        }
        for (String m : ms) {
            if (m.contains(m)) {
                datasets.add(Pair.of(m, map.get(m)));
            }
        }
        String f = testLine ? ChartUtil.generateALineChart(datasets, "查刀", "日期", "出刀数") : ChartUtil.generateHistogram(datasets, "查刀", "日期", "出刀数");
        return buildMessageChainAsSingletonList(uploadImage(event, f));
    }

    /**
     * 获取对 Boss 造成最大伤害的成员
     *
     * @param data
     * @param bossName
     * @return
     */
    private String getMostDamageUser(Map<String, Object> data, String bossName) {
        long d = 0;
        String ans = "";
        for (Map.Entry<String, Object> it : data.entrySet()) {
            Map<String, Object> map = (Map<String, Object>) it.getValue();
            long dmg = (long) ((Map) map.getOrDefault(bossName, new HashMap<>())).getOrDefault("totalDamage", 0l);
            if (dmg > d) {
                d = dmg;
                ans = it.getKey();
            }
        }
        return ans;
    }


    /**
     * 获取对应群的 gt cookie
     *
     * @param groupId
     * @return
     */
    private String getCookie(String groupId) {
        List<Map<String, Object>> gtConfigs = RobotConfig.gtConfig;
        String cookie = null;
        // 获取对应群的 cookie
        for (Map<String, Object> gtConfig : gtConfigs) {
            String gid = (String) gtConfig.getOrDefault("groupId", null);
            if (gid == null || gid.isEmpty() || gid.equals(groupId)) {
                cookie = (String) gtConfig.getOrDefault("gtCookie", null);
                break;
            }
        }
        return cookie;
    }

    /**
     * 获取会战分析接口的 map
     *
     * @param cookie
     * @return Pair, key 是 memberData, val 是 bossNames
     * @throws Exception
     */
    public Pair<Map<String, Object>, Set<String>> getParseBattleStatisticsMap(String cookie) throws Exception {
        String numberOfCuts = "count";
        String totalDamage = "totalDamage";
        List<String> dates = Collections.unmodifiableList((List<String>) ((Map) IOUtil.sendAndGetResponseMap(new URL(GET_MEMBER_URL), "GET", getHeaders(cookie), null).get("data")).get("date"));
        Set<String> bossNames = new HashSet<>(4);
        Map<String, Object> memberData = new HashMap<>();
        for (String date : dates) {
            Map<String, Object> msg = null;
            msg = IOUtil.sendAndGetResponseMap(new URL(BATTLE_REPORT_URL + date), "GET", getHeaders(cookie), null);
            if (msg.getOrDefault("message", "").equals("服务器内部错误")) {
                break;
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) msg.getOrDefault(DATA, new ArrayList<>());
            for (Map<String, Object> dat : data) {
                String userName = (String) dat.get(USER_NAME);
                List<Map<String, Object>> damageList = (List<Map<String, Object>>) dat.get(DAMAGE_LIST);
                // 成员
                Map<String, Object> member = (Map<String, Object>) memberData.getOrDefault(userName, new HashMap<>());
                long count = 0; // 总出刀次数
                long tot = 0; // 总伤害
                for (Map<String, Object> damage : damageList) {
                    String bossName = (String) damage.get(BOSS_NAME);
                    bossNames.add(bossName);
                    long dmg = NumUtil.objectToLong(damage.get(DAMAGE));
                    // 首领统计
                    Map<String, Object> bossMap = (Map<String, Object>) member.getOrDefault(bossName, new HashMap<>());
                    bossMap.put(numberOfCuts, (long) bossMap.getOrDefault(numberOfCuts, 0l) + 1l);
                    bossMap.put(totalDamage, (long) bossMap.getOrDefault(totalDamage, 0l) + dmg);
                    member.put(bossName, bossMap);
                    count++;
                    tot += dmg;
                }
                member.put(numberOfCuts, (long) member.getOrDefault(numberOfCuts, 0l) + count);
                member.put(totalDamage, (long) member.getOrDefault(totalDamage, 0l) + tot);
                memberData.put(userName, member);
            }
//            calendar.set(Calendar.HOUR_OF_DAY, -24);
        }
        return new Pair<Map<String, Object>, Set<String>>(memberData, bossNames);
    }


    /**
     * 催刀
     *
     * @param event
     * @param names 催刀的成员
     * @return
     */
    public MessageChain doUrge(MessageEvent event, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return buildMessageChain("暂无未出刀成员，赞！");
        }
//        Set<String> gtMembers = (Set<String>) RobotConfig.gtConfig.get(1);
        MessageChainBuilder builder = new MessageChainBuilder();
        for (String name : names) {
            Long id = getMemberIdByName(event, name);
            if (id > 0) {
                At at = new At(id);
                builder.add(at);
            }
        }
        builder.add("\n" + "还不赶快去打公会战！\n\n" + "注：如未配置公会成员，此行为会将群内所有成员视为公会成员，这可能会导致多余@，如已配置，请保证公会成员名称完全等于群昵称");
        return builder.build();
    }

    /**
     * 检查谁未出刀
     *
     * @param cookie
     * @param event
     * @return
     */
    public MessageChain doParseCheck(String cookie, MessageEvent event) {
        Pair<Set<String>, Set<String>> pair = getNotDoPeople(cookie, event);
        StringBuilder sb = new StringBuilder();
        sb.append("以下用户暂未出刀：\n");
        if (pair.getKey().size() != 0) {
            for (String s : pair.getKey()) {
                sb.append("  -" + s + "\n");
            }
        }
        sb.append("未出刀用户总数：" + pair.getKey().size() + "\n\n");
        sb.append("以下用户出了刀，但未出满三刀：\n");
        if (pair.getValue().size() != 0) {
            for (String s : pair.getValue()) {
                sb.append("  -" + s + "\n");
            }
        }
        sb.append("出了刀但未出满刀的用户总数：" + pair.getValue().size() + "\n\n");
        sb.append("注：本功能使用的前提是玩家游戏内用户名必须完全等于QQ群内名称，否则无效");
        return new MessageChainBuilder().append(sb.toString()).build();
    }

    /**
     * 获取 GT 公会的成员，如果没有配置或配置为空就返回 null
     *
     * @param event
     * @return
     */
    private Set<String> getGtMembers(MessageEvent event) {
        String groupId = getGroupId(event);
        for (Map<String, Object> map : RobotConfig.gtConfig) {
            String id = (String) map.getOrDefault("groupId", null);
            if (id != null && id.equals(groupId)) {
                if (!map.containsKey("members") || ((List) map.get("members")).isEmpty()) {
                    return null;
                }
                return new HashSet<>((List<String>) map.get("members"));
            }
        }
        return null;
    }

    /**
     * 获取未出刀的成员，如果没有配置工会成员，则默认所有群成员都是工会成员
     *
     * @param cookie
     * @param event
     * @return Pair，key 是没出刀的玩家集合，val 是出刀但是未出满刀的玩家
     */
    public Pair<Set<String>, Set<String>> getNotDoPeople(String cookie, MessageEvent event) {
        // 获取配置成员
        Set<String> members = getGtMembers(event);
        // 如果没有配置，那么则是同群成员
        if (members == null) {
            members = new HashSet<>(getMembersGroupName(event));
        }
        Map<String, Object> msg = null;
        try {
            msg = IOUtil.sendAndGetResponseMap(new URL(BATTLE_REPORT_URL), "GET", getHeaders(cookie), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Set<String> s = new HashSet<>();
        Set<String> s1 = new HashSet<>();
        Set<String> s2 = new HashSet<>();
        List<Map<String, Object>> datas = (List<Map<String, Object>>) msg.getOrDefault(DATA, new ArrayList<>());
        for (Map<String, Object> data : datas) {
            String username = (String) data.get(USER_NAME);
            List damageList = (List) data.get(DAMAGE_LIST);
            if (damageList != null) {
                if (damageList.size() == TOTAL_COUNT) {
                    // 出满刀的仔
                    s.add(username);
                } else {
                    // 出了但是未出满的仔
                    s2.add(username);
                }
            }
        }
        for (String member : members) {
            // 如果既没出满，又没不完全出刀，那么一定没出刀
            if (!s.contains(member) && !s2.contains(member)) {
                s1.add(member);
            }
        }
        return new Pair<>(s1, s2);
    }


    /**
     * 会战统计，对具体玩家的具体统计
     *
     * @param cookie
     * @param username
     * @return
     */
    public MessageChain doParseBattleStatistics(String cookie, String username, MessageEvent event) throws IOException, FileUploadException {
        String numberOfCuts = "count";
        String totalDamage = "totalDamage";
        Pair<Map<String, Object>, Set<String>> p = null;
        try {
            p = getParseBattleStatisticsMap(cookie);
        } catch (Exception e) {
            return new MessageChainBuilder().append("芜湖，发生错误了，错误原因 :" + e.getCause()).build();
        }
        Map<String, Object> memberData = p.getKey();
        Set<String> bossNames = p.getValue();
        if (!memberData.containsKey(username)) {
            return new MessageChainBuilder().append("未查询到用户名为 " + username + " 的用户，请检查玩家名是否正确，注意玩家名不得带空格").build();
        }
        Map<String, Object> map = (Map<String, Object>) memberData.get(username);

        Map<String, Long> bossTotDamage = getBossTotalDamage(memberData, bossNames);
        long t = bossTotDamage.getOrDefault(totalDamage, 0l);
        StringBuilder sb = new StringBuilder();
        sb.append("用户名：" + username + "\n");
        sb.append("  总伤害：" + map.getOrDefault(totalDamage, 0) + "\n");
        sb.append("  总出刀：" + map.getOrDefault(numberOfCuts, 0) + "\n");
        sb.append("  总伤害占比：" + NumUtil.getPercentage((long) map.getOrDefault(totalDamage, 0), t) + "\n\n");
        Map<String, Long> dataset1 = new HashMap<>();
        Map<String, Long> dataset2 = new HashMap<>();

        for (String bossName : bossNames) {

            Map<String, Object> bossMap = (Map<String, Object>) map.getOrDefault(bossName, new HashMap<>());
            sb.append("  " + bossName + "：\n");
            sb.append("    对怪物总伤害：" + bossMap.getOrDefault(totalDamage, 0) + "\n");
            sb.append("    对怪物总出刀：" + bossMap.getOrDefault(numberOfCuts, 0) + "\n");
            sb.append("    对怪物伤害占比：" + NumUtil.getPercentage((long) bossMap.getOrDefault(totalDamage, 0l), bossTotDamage.getOrDefault(bossName, 0l)) + "\n");

            dataset1.put(bossName, NumUtil.objectToLong(bossMap.getOrDefault(numberOfCuts, 0l)));
            dataset2.put(bossName, NumUtil.objectToLong(bossMap.getOrDefault(totalDamage, 0l)));
        }
        sb.append("\n");
        String s1 = ChartUtil.generateAPieChart(dataset1, username + " 的出刀情况");
        String s2 = ChartUtil.generateAPieChart(dataset2, username + " 的输出情况");
        return new MessageChainBuilder().
                append(sb.toString()).
                append(uploadImage(event, s1))
                .append(uploadImage(event, s2))
                .build();
    }

    /**
     * 会战统计，总统计
     *
     * @param cookie
     * @return
     */
    public MessageChain doParseBattleStatistics(String cookie) {
        String numberOfCuts = "count";
        String totalDamage = "totalDamage";
        Pair<Map<String, Object>, Set<String>> p = null;
        try {
            p = getParseBattleStatisticsMap(cookie);
        } catch (Exception e) {
            return new MessageChainBuilder().append("芜湖，发生错误了，错误原因 :" + e.getCause()).build();
        }
        Map<String, Object> memberData = p.getKey();
        Set<String> bossNames = p.getValue();

        StringBuffer sb = new StringBuffer();
        TreeMap<Long, Map<String, Object>> sortMap = new TreeMap<>((a, b) -> (int) (b - a));

        for (Map.Entry<String, Object> it : memberData.entrySet()) {
            Map<String, Object> map = (Map<String, Object>) it.getValue();
            map.put(USER_NAME, it.getKey());
            sortMap.put((Long) map.get(totalDamage), map);
        }

        sortMap.forEach((k, v) -> {
            Map<String, Object> map = v;
            sb.append("用户名：" + map.get(USER_NAME) + "\n");
            sb.append("  总伤害：" + map.getOrDefault(totalDamage, 0) + "\n");
            sb.append("  总出刀：" + map.getOrDefault(numberOfCuts, 0) + "\n");

            for (String bossName : bossNames) {
                Map<String, Object> bossMap = (Map<String, Object>) map.getOrDefault(bossName, new HashMap<>());
                sb.append("  " + bossName + "：\n");
                sb.append("    对怪物总伤害：" + bossMap.getOrDefault(totalDamage, 0) + "\n");
                sb.append("    对怪物总出刀：" + bossMap.getOrDefault(numberOfCuts, 0) + "\n");
            }
            sb.append("\n");
        });
        sb.append("参与会战人数：" + sortMap.size() + "\n\n");
        for (String bossName : bossNames) {
            sb.append("对" + bossName + "造成伤害最高的是：" + getMostDamageUser(memberData, bossName) + "\n\n");
        }
        sb.append("总伤害最高的是：" + sortMap.get(sortMap.firstKey()).get(USER_NAME));
        return new MessageChainBuilder().append(sb.toString()).build();
    }

    /**
     * 前线报道
     *
     * @param cookie
     * @return
     */
    public MessageChain doParseFrontlineReporting(String cookie) {
        Map<String, Object> msg = null;
        try {
            System.out.println("cookie = " + cookie);
            msg = IOUtil.sendAndGetResponseMap(new URL(FRONTLINE_REPORTING_URL), "GET", getHeaders(cookie), null);
            System.out.println("msg = " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuffer sb = new StringBuffer();
        sb.append("******前线报道******\n");
        Map<String, Object> data = (Map<String, Object>) msg.getOrDefault(DATA, new HashMap<>());
        List<Map<String, Object>> bossList = (List<Map<String, Object>>) data.get(BOSS);

        for (Map<String, Object> boss : bossList) {
            sb.append(boss.get(NAME) + "：\n");
            sb.append("  等级：" + boss.get(LEVEL) + "\n");
            sb.append("  属性：" + boss.get(TYPE) + "\n");
            sb.append("  总血量：" + boss.get(TOTAL_HP) + "\n");
            sb.append("  剩余血量：" + boss.get(REMAIN_HP) + "\n");
            double percentage = NumUtil.objectToDouble(boss.get(REMAIN_HP)) / NumUtil.objectToDouble(boss.get(TOTAL_HP));
            String str = String.valueOf(percentage * 100);
            if (str.length() > 5) {
                str = str.substring(0, 5);
            }
            sb.append("  剩余血量占比：" + str + "%\n\n");
        }
        return new MessageChainBuilder().append(sb.toString()).build();
    }


    /**
     * 会战报表
     *
     * @param cookie
     * @return
     */
    public MessageChain doParseBattleReport(String cookie) {
        Map<String, Object> msg = null;
        try {
            msg = IOUtil.sendAndGetResponseMap(new URL(BATTLE_REPORT_URL), "GET", getHeaders(cookie), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int c = 0, maxDamage = 0;
        String maxDamageUser = "无";
        StringBuffer sb = new StringBuffer();
        sb.append("******今日会战报表******\n");
        List<Map<String, Object>> data = (List<Map<String, Object>>) msg.getOrDefault(DATA, new ArrayList<>());
        for (Map<String, Object> datum : data) {
            sb.append("用户名：" + datum.get(USER_NAME) + "\n");
            sb.append("总伤害：" + datum.get(DAMAGE_TOTAL) + "\n");
            List<Map<String, Object>> damageList = (List<Map<String, Object>>) datum.get(DAMAGE_LIST);
            int sum = 0;
            for (int i = 1; i <= TOTAL_COUNT; i++) {
                sb.append("  第" + i + "刀：");
                if (damageList == null || damageList.size() < i) {
                    sb.append("暂未出刀\n");
                    continue;
                }
                sb.append("\n");
                Map<String, Object> damage = damageList.get(i - 1);
                String isKill = (int) damage.get(IS_KILL) == 1 ? "是" : "否";
                sb.append("    boss：" + damage.get(BOSS_NAME) + "\n");
                sb.append("    伤害：" + damage.get(DAMAGE) + "\n");
                sb.append("    是否尾刀：" + isKill + "\n");
                sum += (int) damage.get(DAMAGE);
                c++;
            }
            if (sum > maxDamage) {
                maxDamage = sum;
                maxDamageUser = (String) datum.get(USER_NAME);
            }
            sb.append('\n');
        }
        sb.append("今日总出刀人数：" + data.size() + "\n");
        sb.append("今日出刀数：" + c + "\n");
        sb.append("今日伤害最高玩家：" + maxDamageUser + "\n");
        sb.append("最高伤害：" + maxDamage);
        return new MessageChainBuilder().append(sb.toString()).build();
    }

    /**
     * 获取所有用户对怪物造成的伤害
     * @param memberData
     * @param bossNames
     * @return
     */
    private Map<String, Long> getBossTotalDamage(Map<String, Object> memberData, Set<String> bossNames) {
        Map<String, Long> bossTotalDamage = new HashMap<>();
        long t = 0;
        String totalDamage = "totalDamage";
        for (Map.Entry<String, Object> entry : memberData.entrySet()) {
            Map<String, Object> m = (Map<String, Object>) entry.getValue();
            t += (long) m.getOrDefault(totalDamage, 0);
            for (String bossName : bossNames) {
                Map<String, Object> bossMap = (Map<String, Object>) m.getOrDefault(bossName, new HashMap<>());
                bossTotalDamage.put(
                        bossName,
                        bossTotalDamage.getOrDefault(bossName, 0l) + NumUtil.objectToLong(bossMap.getOrDefault(totalDamage, 0)));
            }
        }
        bossTotalDamage.put(totalDamage, t);
        return bossTotalDamage;
    }

    /**
     * 获取 heads，添加 gt cookie
     *
     * @param cookie
     * @return
     */
    private Map<String, String> getHeaders(String cookie) {
        Map<String, String> heads = new HashMap<>(10);
        heads.put("Cookie", cookie);
        return heads;
    }

    /**
     * 检测到关键词回复，不需要 at 机器人，只要以该关键词开头即可
     *
     * @param event
     * @return
     */
    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        return startWithKeywords(event, keywords);
    }
}

