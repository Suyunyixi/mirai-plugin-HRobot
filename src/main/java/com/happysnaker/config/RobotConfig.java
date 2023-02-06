package com.happysnaker.config;

import net.mamoe.mirai.utils.MiraiLogger;

import java.io.File;
import java.util.*;

/**
 * 机器人配置，调用者需直接调用此配置而非在本地线程保存，因为配置是可能被重载的
 * @author Happysnaker
 * @description
 * @date 2022/1/15
 * @email happysnaker@foxmail.com
 */
public class RobotConfig {
    // ------------ 系统配置 ----------------
    /**
     * 插件版本，每次更新请更新此配置
     */
    public static final String CURRENT_VERSION = "3.4";
    
    /**
     * 主配置文件名
     */
    public static String mainConfigPathName = "config.yaml";

    /**
     * 敏感词库
     */
    public static String sensitiveWordPathName = "sensitiveWord.txt";

    /**
     * 配置文件存放的目录路径
     */
    public static File configFolder;

    /**
     * 数据存放的目录路径
     */
    public static File dataFolder;

    /**
     * 日志
     */
    public volatile static MiraiLogger logger;


    /**
     * 是否启动机器人，可动态关闭开启，关闭后机器人不在接受任何消息（除了开启消息）
     */
    public static volatile boolean enableRobot = true;

    // ------------ 用户配置 ----------------
    /**
     * 主菜单，回复 help 或 帮助 获取
     */
    public static String menu;

    /**
     * 机器人管理员，索引 0 为超级管理员
     */
    public static List<String> administrator = new ArrayList<>();

    /**
     * 群管理员
     */
    public static List<String> groupAdministrator = new ArrayList<>();

    /**
     * 坎公管理员
     */
    public static List<String> gtAdministrator = new ArrayList<>();

    /**
     * 坎公配置
     */
    public static List<Map<String, Object>> gtConfig = new ArrayList<>();

    /**
     * 需要排除的群
     */
    public static List<String> include = new ArrayList<>();

    /**
     * 包括的群
     */
    public static List<String> exclude = new ArrayList<>();

    /**
     * 涩图撤回时间，单位 s
     */
    public static int pictureWithdrawalTime = 30;

    /**
     * 检测自定关键词的相似度，换句话说，当相似度达到 customKeywordSimilarity 时，即认为触发自定义关键词，此项范围: (0, 1.0]
     */
    public static double customKeywordSimilarity = 0.8;

    /**
     * 自定义关键词及回复，value 允许是一条 mirai 消息，这意味着回复消息可以是表情或者图片等，此 Map {key - val} 可能是 {自定义关键词 - 自定义回复}，也有可能是 {群号 - 嵌套的 Map}，因此自定义关键词不允许是 bot 的所有的群号，当 {key - val} 为 {群号 - 嵌套的 Map} 形式，说明此嵌套的 Map 只在特定群内生效
     */
    public static Map<String, Object> customKeyword = new HashMap<>();

    /**
     * 命令前缀，任何命令需以此前缀开头，用以区分普通消息
     */
    public static String commandPrefix = "#";

    /**
     * 请求网络 API 的超时事件，以 ms 为单位
     */
    public static int timeout = 3000;

    /**
     * 发送鸡汤时有一定概率发送毒鸡汤，这个概率是 [0, 1] 之间的小数
     */
    public static double duChickenSoupProbability = 0.2;

    /**
     * 聊天是否需要 @ 机器人，如果此字段为 false，则任意消息机器人都会回复
     */
    public static volatile boolean enableAt = true;

    /**
     * 自定义回复替代，可以将回复消息内的关键词替换为自定义语句
     */
    public static List<Map<String, String>> replyReplace = new ArrayList<>();

    /**
     * 需要自动审批的群，map 中 key 为群号，val 为验证消息
     */
    public static List<Map<String, String>> autoApproval = new ArrayList<>();

    /**
     * 敏感词库
     */
    public static Set<String> sensitiveWord = new HashSet<>();

    /**
     * 敏感词检测是是否忽略无意义的词
     */
    public static boolean skipIsMeaninglessWord = true;

    public static int skipStep = 3;

    /**
     * 当检测到多少个敏感词后撤回该条消息
     */
    public static int withdrawalThreshold = 3;

    /**
     * 表示哪个群开启了敏感词检测
     */
    public static List<String> enableSensitiveWordDetection = new ArrayList<>();

    /**
     * 每天定时执行的任务，key 分别是 hour、minute、groupId、content、image、count，指示每天的几点几分向哪个群发送什么内容，内容是否需要附带一张美图，一共需要执行几次
     */
    public static List<Map<String, Object>> periodicTask = new ArrayList<>();

    /**
     * 颜色开关
     */
    public static boolean colorSwitch = true;

    /**
     * 颜色图片发送策略 <br/>
     * <ul>
     *     <li>数值 0 表示不发送任何消息</li>
     *     <li>数值 1 表示仅发送图片链接，不发送图片</li>
     *     <li>数值 2 表示仅发送图片，不发送图片链接</li>
     *     <li>数值 3 表示既发送图片，又发送图片链接</li>
     *     <li>数值 4 表示仅图片链接，与 1 的区别在于 1 上传了腾讯服务器，而 4 发送原始链接，手机 QQ 中可能会被视为恶意链接</li>
     * </ul>
     */
    public volatile static int colorStrategy = 2;

    /**
     * 俄罗斯轮盘赌，key 分别是 totalNum, realNum, base，含义可参考配置文件
     */
    public static Map<String, Object> russianRoulette = new HashMap<>();
    /**
     * 订阅配置，keySet 为 platform、type、key、atMember
     */
    public static List<Map<String, Object>> subscribe = new ArrayList<>();
}
