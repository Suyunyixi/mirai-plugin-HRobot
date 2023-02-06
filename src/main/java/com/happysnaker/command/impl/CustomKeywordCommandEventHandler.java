package com.happysnaker.command.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.exception.CanNotParseCommandException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.handler.impl.CustomKeywordMessageEventHandler;
import com.happysnaker.permission.Permission;
import com.happysnaker.utils.Pair;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Happysnaker
 * @description
 * @date 2022/2/24
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = 1024)
public class CustomKeywordCommandEventHandler extends DefaultCommandEventHandlerManager {
    public static final String SET_KEYWORD = "设置关键字回复";
    public static final String SET_GROUP_KEYWORD = "设置群内关键字回复";
    public static final String REMOVE_KEYWORD = "移除关键字";
    public static final String REMOVE_GROUP_KEYWORD = "移除群内关键字";
    public static final String CLEAR_KEYWORD = "清空关键字";
    public static final String CLEAR_GROUP_KEYWORD = "清空群内关键字";
    public static final String CLEAR_ALL = "清空所有关键字";
    public static final String SEE_GROUP_KEYWORD = "查看群内关键字";

    public CustomKeywordCommandEventHandler() {
        super.registerKeywords(SET_KEYWORD);
        super.registerKeywords(SET_GROUP_KEYWORD);
        super.registerKeywords(REMOVE_GROUP_KEYWORD);
        super.registerKeywords(REMOVE_KEYWORD);
        super.registerKeywords(CLEAR_KEYWORD);
        super.registerKeywords(CLEAR_GROUP_KEYWORD);
        super.registerKeywords(CLEAR_ALL);
        super.registerKeywords(SEE_GROUP_KEYWORD);
    }

    /**
     * @param event 命令事件
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    @Override
    public List<MessageChain> parseCommand(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        String content = getPlantContent(event);
        if (content.contains(SET_KEYWORD)) {
            return setKeyword(event);
        } else if (content.contains(SET_GROUP_KEYWORD)) {
            return setGroupKeyword(event);
        } else if (content.contains(REMOVE_KEYWORD)) {
            return removeKeyword(event);
        } else if (content.contains(REMOVE_GROUP_KEYWORD)) {
            return removeGroupKeyword(event);
        } else if (content.contains(CLEAR_KEYWORD)) {
            return clearKeyword(event);
        } else if (content.contains(CLEAR_GROUP_KEYWORD)) {
            return clearGroupKeyword(event);
        } else if (content.contains(CLEAR_ALL)) {
            return clearAll(event);
        }
        return super.parseCommand(event);
    }

    /**
     * 设置全局关键字回复，至少需要管理员权限
     *
     * @param event
     * @return
     * @throws CanNotParseCommandException
     */
    private List<MessageChain> setKeyword(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        // 去除命令
        String content = getContent(event).replace(RobotConfig.commandPrefix + SET_KEYWORD, "").trim();
        Pair<String, String> pair;
        if (hasQuote(event)) {
            pair = Pair.of(content, getContent(getQuoteMessageChain(event)));
        } else {
            pair = getKeyVal(content);
        }
        String regexKey =  StringEscapeUtils.unescapeJava(pair.getKey());
        RobotConfig.customKeyword.put(regexKey, pair.getValue());
        return buildMessageChainAsSingletonList("添加全局自定义关键字 " + regexKey + " 及回复成功！");
    }

    /**
     * 设置群内关键字
     * @param event
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    @SuppressWarnings("unchecked")
    private List<MessageChain> setGroupKeyword(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasGroupAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException("权限不足");
        }
        // 去除命名
        String content = getContent(event).replace(RobotConfig.commandPrefix + SET_GROUP_KEYWORD, "").trim();
        Pair<String, String> pair;
        if (hasQuote(event)) {
            pair = Pair.of(content, getContent(getQuoteMessageChain(event)));
        } else {
            pair = getKeyVal(content);
        }

        RobotConfig.customKeyword.putIfAbsent(getGroupId(event), new HashMap<>());
        Map<String, Object> gMap = (Map<String, Object>) RobotConfig.customKeyword.get(getGroupId(event));
        gMap.put(pair.getKey(), pair.getValue());
        return buildMessageChainAsSingletonList("添加群内自定义关键字 " + pair.getKey().replace(CustomKeywordMessageEventHandler.REGEX_PREFIX, "") + " 及回复成功！");
    }


    /**
     * 移除全局
     * @param event
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    private List<MessageChain> removeKeyword(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        String content = getContent(event).replace(RobotConfig.commandPrefix + REMOVE_KEYWORD, "").trim();
        if (RobotConfig.customKeyword.remove(content) == null) {
            return buildMessageChainAsSingletonList("不包含此全局关键字：" + content);
        }
        return buildMessageChainAsSingletonList("移除成功");
    }

    /**
     * 移除群内
     * @param event
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    private List<MessageChain> removeGroupKeyword(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasGroupAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        String content = getContent(event).replace(RobotConfig.commandPrefix + REMOVE_GROUP_KEYWORD, "").trim();
        if (!RobotConfig.customKeyword.containsKey(getGroupId(event))) {
            return buildMessageChainAsSingletonList("此群暂无任何群内关键字配置");
        }
        Map<String, Object> gMap = (Map<String, Object>) RobotConfig.customKeyword.get(getGroupId(event));
        if (gMap.remove(content) == null) {
            return buildMessageChainAsSingletonList("不包含此群内关键字：" + content);
        }
        return buildMessageChainAsSingletonList("移除成功");
    }


    /**
     * 仅清空全局
     * @param event
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    private List<MessageChain> clearKeyword(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        Map<String, Object> map = new HashMap<>();
        Set<String> allGroupId = getBotsAllGroupId();
        for (Map.Entry<String, Object> entry : RobotConfig.customKeyword.entrySet()) {
            if (allGroupId.contains(entry.getKey())) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        RobotConfig.customKeyword = map;
        return buildMessageChainAsSingletonList("已清空所有全局关键字");
    }

    /**
     * 清空群内
     * @param event
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    private List<MessageChain> clearGroupKeyword(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasGroupAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        RobotConfig.customKeyword.remove(getGroupId(event));
        return buildMessageChainAsSingletonList("已清空群 " + getGroupId(event) + " 内的所有关键字");
    }

    /**
     * 清空全部
     * @param event
     * @return
     * @throws CanNotParseCommandException
     * @throws InsufficientPermissionsException
     */
    private List<MessageChain> clearAll(MessageEvent event) throws CanNotParseCommandException, InsufficientPermissionsException {
        if (!Permission.hasSuperAdmin(getSenderId(event))) {
            throw new InsufficientPermissionsException();
        }
        RobotConfig.customKeyword = new HashMap<>();
        return buildMessageChainAsSingletonList("已清空所有关键字信息");
    }


    /**
     * 提取关键字及回复
     * @param content
     * @return
     * @throws CanNotParseCommandException
     */
    private Pair<String, String> getKeyVal(String content) throws CanNotParseCommandException {
        int l = content.indexOf('{');
        int r = content.indexOf('}');
        if (l == -1 || r == -1) {
            throw new CanNotParseCommandException("未检测到关键字，请以{}包裹关键字，注意{}为英文字符.");
        }
        String keyword = content.substring(l + 1, r);
        if (getBotsAllGroupId().contains(keyword)) {
            throw new CanNotParseCommandException("不正确的关键字格式，关键字不得以机器人所有的群号");
        }
        String val = content.replace("{" + keyword + "}", "");
        return new Pair<>(keyword.trim(), val.trim());
    }
}
