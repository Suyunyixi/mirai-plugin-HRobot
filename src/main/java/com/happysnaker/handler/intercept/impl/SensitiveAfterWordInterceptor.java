package com.happysnaker.handler.intercept.impl;

import com.happysnaker.config.RobotConfig;
import com.happysnaker.handler.intercept.Intercept;
import com.happysnaker.utils.Trie;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

/**
 * 关键词检测
 *
 * @author Happysnaker
 * @description
 * @date 2022/7/2
 * @email happysnaker@foxmail.com
 */
@Intercept
public class SensitiveAfterWordInterceptor extends AdaptInterceptor {
    private final Trie trie;

    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    public SensitiveAfterWordInterceptor() {
        trie = new Trie();
        for (String s : RobotConfig.sensitiveWord) {
            trie.insert(s);
        }
    }


    public boolean containsSensitiveWord(char[] text, int from) {
        int n = text.length;
        boolean skip = RobotConfig.skipIsMeaninglessWord;
        int skipStep = RobotConfig.skipStep;
        Trie node = trie;
        for (int i = from; i < n; i++) {
            char ch = text[i];
            // 如果没检测到，但是字符可以忽略，则忽略
            if (node.nextNode(ch) == null) {
                if (skip && !isChinese(ch) && !Character.isLetterOrDigit(ch)) {
                    continue;
                } else if (skipStep-- > 0) {
                    continue;
                } else {
                    return false;
                }
            }
            node = node.nextNode(ch);
            if (node.isStringEnd()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean interceptBefore(MessageEvent event) {
        if (!(event instanceof GroupMessageEvent)) {
            return false;
        }
        GroupMessageEvent groupEvent = (GroupMessageEvent) event;
        String gid = String.valueOf(groupEvent.getGroup().getId());
        if (!RobotConfig.enableSensitiveWordDetection.contains(gid)) {
            return false;
        }
        char[] str = getOnlyPlainContent(event).trim().toCharArray();
        int n = str.length, count = 0;
        for (int i = 0; i < n; i++) {
            if (containsSensitiveWord(str, i)) {
                count++;
                if (count >= RobotConfig.withdrawalThreshold) {
                    MessageChain chain = buildMessageChain(
                            new At(event.getSender().getId()),
                            "\n",
                            "警告，你的发言违反了群内相关规定，请不要发布色情及政治相关言论"
                    );
                    event.getSubject().sendMessage(chain);
                    MessageSource.recall(event.getSource());
                    return true;
                }
            }
        }
        return false;
    }
}
