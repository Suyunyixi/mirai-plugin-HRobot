package com.happysnaker.handler.impl;

import com.happysnaker.api.QingYunKeApi;
import com.happysnaker.config.RobotConfig;
import com.happysnaker.proxy.Context;
import com.happysnaker.exception.FileUploadException;
import com.happysnaker.exception.InsufficientPermissionsException;
import com.happysnaker.handler.MessageHandler;
import com.happysnaker.utils.OfUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.SingleMessage;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Happysnaker
 * @description
 * @date 2022/1/14
 * @email happysnaker@foxmail.com
 */
@MessageHandler(priority = -1) // 最低的优先级
public class GroupMessageEventHandler extends AbstractMessageEventHandler {

    /**
     * 获取群号
     *
     * @param event
     * @return
     */
    protected String getGroupId(MessageEvent event) {
        GroupMessageEvent group = (GroupMessageEvent) event;
        return String.valueOf(group.getGroup().getId());
    }

    /**
     * 是否是群聊事件
     *
     * @param event
     * @return
     */
    protected boolean isGroupMessageEvent(MessageEvent event) {
        return event instanceof GroupMessageEvent;
    }



    /**
     * 检查某人是否被 @
     *
     * @param event
     * @param qq
     * @return
     */
    protected boolean isAt(MessageEvent event, String qq) {
        if (qqList == null || qqList.isEmpty()) initBotQQ();
        for (SingleMessage message : event.getMessage()) {
            if (message instanceof At) {
                At at = (At) message;
                if (Long.parseLong(qq) == at.getTarget()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 机器人是否被 at，此方法会忽略 @all 消息
     *
     * @return
     */
    protected boolean isAtBot(MessageEvent event) {
        if (qqList == null) {
            initBotQQ();
        }
        for (Bot bot : Bot.getInstances()) {
            if (isAt(event, String.valueOf(bot.getId()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将消息转换为群聊消息
     *
     * @param event
     * @return 若转换失败，则返回 null
     */
    protected GroupMessageEvent getGroupMessageEvent(MessageEvent event) {
        try {
            return (GroupMessageEvent) event;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取群成员列表，不包含机器人自己
     *
     * @param event
     * @return 若获取失败则返回 null
     */
    protected ContactList<NormalMember> getMembers(MessageEvent event) {
        GroupMessageEvent group = getGroupMessageEvent(event);
        if (group != null) {
            return group.getGroup().getMembers();
        }
        return null;
    }

    /**
     * 以 List 的形式返回群内所有群成员的 群内名称，如果群内名称为空，则使用用户的QQ昵称替代
     *
     * @param event
     * @return
     */
    protected List<String> getMembersGroupName(MessageEvent event) {
        ContactList<NormalMember> members = null;
        List<String> ans = new ArrayList<>();
        if ((members = getMembers(event)) != null) {
            for (NormalMember member : members) {
                String s = member.getNameCard();
                if (s.isEmpty()) {
                    s = member.queryProfile().getNickname();
                }
                ans.add(s);
            }
        }
        return ans;
    }

    /**
     * 获取所有群成员的 QQ，不包含机器人自己
     *
     * @param event
     * @return
     */
    protected List<Long> getMembersIds(MessageEvent event) {
        ContactList<NormalMember> members = null;
        List<Long> ans = new ArrayList<>();
        if ((members = getMembers(event)) != null) {
            for (NormalMember member : members) {
                member.getNameCard();
                ans.add(member.getId());
            }
        }
        return ans;
    }

    /**
     * 获取群成员的 qq 号
     *
     * @param event
     * @param groupName 群成员的群名片，如果为空，则为群成员的昵称
     * @return 未搜索到返回 -1
     */
    protected Long getMemberIdByName(MessageEvent event, String groupName) {
        ContactList<NormalMember> members;
        long ans = -1;
        if ((members = getMembers(event)) != null) {
            for (NormalMember member : members) {
                String s = member.getNameCard();
                if (s.isEmpty())
                    s = member.queryProfile().getNickname();
                if (s.equals(groupName))
                    return member.getId();
            }
        }
        return ans;
    }


    /**
     * 将要回复的消息，默认使用青云客 API 消息回复，允许子类进行扩展，如果此消息返回 null，则不会尝试回复该消息
     *
     * @param event 经过 proxyContent 处理后的消息
     * @param ctx
     * @return 允许发送多条消息，因此需要返回一个消息列表
     */
    @Override
    public List<MessageChain> handleMessageEvent(MessageEvent event, Context ctx) {
        String content = getPlantContent(event);
        String help1 = "帮助", help2 = "help", help3 = "菜单";
        if (help1.equals(content) || help2.equals(content) || help3.equals(content)) {
            try {
                if (!RobotConfig.menu.isEmpty()) {
                    return buildMessageChainAsSingletonList(RobotConfig.menu);
                }
                return doHelp(event);
            } catch (FileUploadException | MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return OfUtil.ofList(replaceFaceFromContent(QingYunKeApi.getMessage(content)));
    }

    /**
     * 获取纯文本消息，不包含任何 mirai 编码内容，不包含任何 at 消息
     *
     * @param event
     * @return 返回处理后的消息，消息将去除首尾空格
     * @see AbstractMessageEventHandler#getPlantContent
     */
    @Override
    public String getPlantContent(MessageEvent event) {
        return super.getPlantContent(event).trim();
    }

    /**
     * 获取发送者权限
     *
     * @param event
     * @return 0 是成员、1 是管理员，2 是群主
     */
    protected int getSenderPermission(MessageEvent event) {
        MemberPermission permission = getGroupMessageEvent(event).getPermission();
        return permission.getLevel();
    }

    /**
     * 由配置决定是否需要 @ 机器人
     *
     * @param event
     * @return
     */
    @Override
    public boolean shouldHandle(MessageEvent event, Context ctx) {
        return isGroupMessageEvent(event) &&
                (isAtBot(event) || !RobotConfig.enableAt);
    }

    /**
     * 撤回一条消息，如果撤回失败请考虑是否是权限不够
     *
     * @param source
     * @return 成功返回 true，否则返回 false
     */
    protected boolean cancelMessage(MessageSource source) {
        try {
            MessageSource.recall(source);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 撤回一条消息，如果撤回失败请考虑是否是权限不够
     *
     * @param event
     * @return 成功返回 true，否则返回 false
     * @throws InsufficientPermissionsException 如果权限不够则抛出此异常
     */
    protected boolean cancelMessage(MessageEvent event) throws InsufficientPermissionsException {
        if (((GroupMessageEvent) event).getPermission().getLevel() > 0) {
            throw new InsufficientPermissionsException("权限不足");
        }
        return cancelMessage(event.getSource());
    }

    /**
     * 检查此事件是否是群事件，并检查事件消息是否以关键词开头
     *
     * @param event
     * @param keywords
     * @return 如果都为真返回 true
     */
    @Override
    public boolean startWithKeywords(MessageEvent event, Collection<String> keywords) {
        return isGroupMessageEvent(event) && super.startWithKeywords(event, keywords);
    }

    /**
     * 禁言群成员
     * @param qq 群成员 qq
     * @param group 群
     * @param durationSecond 秒
     */
    protected void mute(String qq, Group group, int durationSecond) {
        for (NormalMember member : group.getMembers()) {
            if (member.getId() == Long.parseLong(qq)) {
                member.mute(durationSecond);
                return;
            }
        }
    }

    /**
     * 禁言发言者
     * @param event 消息事件
     * @param durationSecond 时长、秒
     */
    protected void muteSend(MessageEvent event, int durationSecond) {
        getGroupMessageEvent(event).getSender().mute(durationSecond);
    }
}
