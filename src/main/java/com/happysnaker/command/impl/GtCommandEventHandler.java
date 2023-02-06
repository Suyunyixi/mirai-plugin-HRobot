package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.permission.Permission;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

import java.util.*;
import java.util.stream.Collectors;

/** 坎公成员管理
 * @author Happysnaker
 * @date 2022/2/18
 * @email happysnaker@foxmail.com
 * @deprecated 坎公功能在未来可能会被重构或废弃，设置成员的方式可能会有所改变
 */
@MessageHandler(priority = 1024)
@Deprecated
public class GtCommandEventHandler extends DefaultCommandEventHandlerManager {
    public static final String addGtMembersCommand = "添加坎公公会成员";
    public static final String clearGtMembersCommand = "清空坎公公会成员";
    public static final String setGtMembersCommand = "设置坎公公会成员";
    public static final String seeGtMembersCommand = "查看坎公公会成员";
    public static final String removeGtMembersCommand = "移除坎公公会成员";
    public static final String setGtCookieCommand = "设置坎公cookie";

    public GtCommandEventHandler() {
        super.registerKeywords(setGtCookieCommand);
        super.registerKeywords(setGtMembersCommand);
        super.registerKeywords(seeGtMembersCommand);
        super.registerKeywords(removeGtMembersCommand);
        super.registerKeywords(clearGtMembersCommand);
        super.registerKeywords(addGtMembersCommand);
    }

    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        List<MessageChain> ans = new ArrayList<>();
        String content = getPlantContent(event);
        try {
            if (content.startsWith(setGtMembersCommand)) {
                ans.add(doSetGtMembers(event));
            } else if (content.startsWith(setGtCookieCommand)) {
                ans.add(doSetGtCookie(event));
            } else if (content.startsWith(seeGtMembersCommand)) {
                ans.add(doSeeGtMembers(event));
            } else if (content.startsWith(removeGtMembersCommand)) {
                ans.add(doRemoveGtMembers(event));
            } else if (content.startsWith(clearGtMembersCommand)) {
                ans.add(doClearGtMembers(event));
            } else if (content.startsWith(addGtMembersCommand)) {
                ans.add(doAddGtMembers(event));
            }
        } catch (InsufficientPermissionsException e) {
            throw e;
        } catch (Exception e) {
            throw new CanNotParseCommandException(e);
        }
        return ans;
    }


    public MessageChain doAddGtMembers(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasGtAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        List<String> gtMembers = getGtMembers(event);
        Set<String> book = new HashSet<>(gtMembers);
        if (gtMembers == null || gtMembers.isEmpty()) {
            return buildMessageChain("该群暂未配置成员信息，无法添加成员，请先配置。\n默认将所有群成员视为公会成员。");
        }
        String content = getPlantContent(event).replace(addGtMembersCommand, "").trim();
        String[] strings = content.split("\\s+");
        int num = 0;
        for (String string : strings) {
            if (!book.contains(string)) {
                gtMembers.add(string);
                num++;
            }
        }
        addIfAbsent(getGroupId(event), "members", gtMembers);
        return buildMessageChain("配置成功，新增成员数 " + num);
    }

    public MessageChain doClearGtMembers(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasGtAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        List<String> gtMembers = getGtMembers(event);
        if (gtMembers == null || gtMembers.isEmpty()) {
            return buildMessageChain("该群暂未配置成员信息，无法清空成员，请先配置。\n默认将所有群成员视为公会成员。");
        }
        // 置空
        addIfAbsent(getGroupId(event), "members", new ArrayList<>());
        return buildMessageChain("清空成功");
    }

    public MessageChain doRemoveGtMembers(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasGtAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        String content = getPlantContent(event).replace(removeGtMembersCommand, "").trim();
        String[] strings = content.split("\\s+");
        List<String> gtMembers = getGtMembers(event);
        if (gtMembers == null || gtMembers.isEmpty()) {
            return buildMessageChain("该群暂未配置成员信息，无法移除成员，请先配置。\n默认将所有群成员视为公会成员。");
        }
        int num = 0;
        for (String string : strings) {
            if (gtMembers.remove(string)) {
                num++;
            }
        }
        addIfAbsent(getGroupId(event), "members", gtMembers);
        System.out.println("num = " + num);
        return buildMessageChain("移除成员成功，共移除 " + num + " 名成员");
    }

    public MessageChain doSeeGtMembers(MessageEvent event) throws InsufficientPermissionsException {
        System.out.println("Permission.hasGtAdmin(getSenderId(event)) = " + Permission.hasGtAdmin(getSenderId(event)));
        if (!Permission.hasGtAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        Set<String> groupNames = new HashSet<>(getMembersGroupName(event));
        List<String> gtMembers = getGtMembers(event);
        StringBuilder sb = new StringBuilder(
                "群 " + getGroupId(event) + " 的工会成员配置为：\n");
        int num = 0;
        if (gtMembers != null || gtMembers.isEmpty()) {
            for (String gtMember : gtMembers) {
                sb.append("- " + gtMember);
                if (!groupNames.contains(gtMember)) {
                    sb.append(" (群内未检测到该成员)");
                    num++;
                }
                sb.append("\n");
            }
        } else {
            sb.append("当前群无工会成员配置，默认将全部群成员视为公会成员");
        }
        if (num > 0) {
            sb.append("有 " + num + " 名成员未在群内检测到，催刀时可能无法搜寻到这些玩家，若想更好的使用此功能，请督促这些玩家更改群名片为游戏名称");
        }
        return buildMessageChain(sb.toString());
    }

    /**
     * 设置坎公 cookie，命令格式 "#设置坎公cookie qq群 cookie"，该命令需要机器人管理员设置
     *
     * @param event
     * @return
     */
    public MessageChain doSetGtCookie(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        String content = getPlantContent(event).replace(RobotConfig.commandPrefix, "");
        content = content.trim();
        String[] strs = content.trim().split("\\s+");
        String groupId = strs[1];
        String cookie = strs[2];
        // cookie 中可能存在空格，如果遵守约定的话，从第二项开始应该全部属于 cookie 项
        for (int i = 2; i < strs.length; i++) {
            if (!strs[i].isEmpty()) {
                cookie += strs[i];
            }
        }
        addIfAbsent(groupId, "gtCookie", cookie);
        return buildMessageChain("配置成功，注意如果不执行保存配置命令，配置将会在机器人重启时失效。");
    }

    /**
     * 设置坎公成员，命令格式 "#设置坎公公会成员 成员1 成员2 成员3"，注意此命令将配置到对应 event 群的配置中，该命令不应由机器人管理员执行，应当由对应群的 坎公管理员 执行
     *
     * @param event
     * @return
     */
    public MessageChain doSetGtMembers(MessageEvent event) throws InsufficientPermissionsException {
        if (!Permission.hasGtAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        String content = getPlantContent(event);
        String[] members = content.replace(setGtMembersCommand, "").trim().split("\\s+");
        if (members == null || members.length == 0) {
            return new MessageChainBuilder().append("未检测到任何成员信息").build();
        }
        List<String> m = Arrays.stream(members).collect(Collectors.toList());
        addIfAbsent(getGroupId(event), "members", m);
        return buildMessageChain("配置成功！");
    }

    /**
     * 检测坎公配置，如果存在 groupId = gid 的项，则 put 该项 key、val，如果不存在，则新建 groupId = gid 的项，并 put 该项 key、val
     *
     * @param gid
     * @param key
     * @param val
     */
    private void addIfAbsent(String gid, String key, Object val) {
        for (Map<String, Object> map : RobotConfig.gtConfig) {
            String groupId = (String) map.getOrDefault("groupId", "");
            if (groupId.equals(gid.trim())) {
                map.put(key, val);
                return;
            }
        }
        Map<String, Object> map = new HashMap<>(8);
        map.put("groupId", gid);
        map.put(key, val);
        RobotConfig.gtConfig.add(map);
    }

    private List<String> getGtMembers(MessageEvent event) {
        String gid = getGroupId(event);
        for (Map<String, Object> map : RobotConfig.gtConfig) {
            if (map.getOrDefault("groupId", "").equals(gid)) {
                return (List<String>) map.getOrDefault("members", new ArrayList<>());
            }
        }
        return new ArrayList<>();
    }
}
