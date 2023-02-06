package com.happysnaker.handler.intercept.impl;

import com.happysnaker.config.ConfigManager;
import com.happysnaker.config.RobotConfig;
import com.happysnaker.handler.intercept.Intercept;
import com.happysnaker.utils.StringUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import javax.naming.CannotProceedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后置拦截器，对回复的语句做自定义替换
 *
 * @author Happysnaker
 * @description
 * @date 2022/7/2
 * @email happysnaker@foxmail.com
 */
@Intercept(order = 0)
public class ReplyAfterInterceptor extends AdaptInterceptor {

    @Override
    public List<MessageChain> interceptAfter(MessageEvent e, List<MessageChain> mc) {
        List<MessageChain> ans = new ArrayList<>();
        if (mc == null) {
            return null;
        }
        for (MessageChain chain : mc) {
            String content = getContent(chain);
            boolean v = false;  // 是否做了替换
            for (Map<String, String> map : RobotConfig.replyReplace) {
                for (Map.Entry<String, String> it : map.entrySet()) {
                    if (content.contains(it.getKey())) {
                        content = content.replace(it.getKey(), it.getValue());
                        v = true;
                    }
                }
            }
            try {
                ans.add(v ? parseMiraiCode(content, e) : chain);
            } catch (CannotProceedException ex) {
                ConfigManager.recordFailLog(e, StringUtil.getErrorInfoFromException(ex));
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        return ans;
    }
}
