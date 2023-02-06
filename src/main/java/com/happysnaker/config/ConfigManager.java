package com.happysnaker.config;

import com.happysnaker.cron.RobotCronJob;
import com.happysnaker.utils.IOUtil;
import com.happysnaker.utils.OfUtil;
import com.happysnaker.utils.StringUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static com.happysnaker.utils.RobotUtil.getContent;
import static com.happysnaker.utils.RobotUtil.getSenderId;

/**
 * @author Happysnaker
 * @description
 * @date 2022/2/15
 * @email happysnaker@foxmail.com
 */
public class ConfigManager {
    public static Set<String> configNames = OfUtil.ofSet(
            "administrator",
            "gtAdministrator",
            "groupAdministrator",
            "menu",
            "gtConfig",
            "include",
            "exclude",
            "pictureWithdrawalTime",
            "customKeywordSimilarity",
            "customKeyword",
            "commandPrefix",
            "timeout",
            "duChickenSoupProbability",
            "enableAt",
            "replyReplace",
            "autoApproval",
            "skipIsMeaninglessWord",
            "withdrawalThreshold",
            "enableSensitiveWordDetection",
            "skipStep",
            "periodicTask",
            "colorSwitch",
            "russianRoulette",
            "colorStrategy",
            "subscribe"
    );


    /**
     * 用于消息处理失败时记录日志
     *
     * @param event
     * @param errorMsg
     */
    public static void recordFailLog(MessageEvent event, String errorMsg) {
        RobotCronJob.service.schedule(new TimerTask() {
            @Override
            public void run() {
                String filePath = ConfigManager.getDataFilePath("error.log");
                try {
                    IOUtil.writeToFile(new File(filePath), formatLog(event) + "\n错误日志：" + errorMsg + "\n\n");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }

    public static String formatLog(MessageEvent event) {
        if (event == null) {
            return "[" + StringUtil.formatTime() + "]";
        }
        String content = getContent(event);
        String sender = getSenderId(event);
        if (!(event instanceof GroupMessageEvent)) {
            return "[sender:" + sender + "-" + StringUtil.formatTime() + "] -> " + content;
        }
        long groupId = ((GroupMessageEvent) event).getGroup().getId();
        return "[sender:" + sender + " - group:" + groupId + " - " + StringUtil.formatTime() + "] -> " + content;
    }

    /**
     * 获取主配置文件路径
     *
     * @return
     */
    public static String getConfigFilePath() {
        return RobotConfig.configFolder + "/" + RobotConfig.mainConfigPathName;
    }

    /**
     * 根据配置文件名返回配置路径
     *
     * @param fileName
     * @return
     */
    public static String getConfigFilePath(String fileName) {
        return RobotConfig.configFolder + "/" + fileName;
    }

    /**
     * 根据数据文件名返回存放数据文件的路径
     *
     * @param fileName
     * @return
     */
    public static String getDataFilePath(String fileName) {
        return RobotConfig.dataFolder + "/" + fileName;
    }

    /**
     * 将配置转换未 map 的形式返回
     *
     * @return
     */
    public static Map<String, Object> getConfigMap() {
        try {
            Field[] fields = RobotConfig.class.getDeclaredFields();
            Map<String, Object> map = new HashMap<>();
            for (Field field : fields) {
                if (configNames.contains(field.getName())) {
                    map.put(field.getName(), field.get(null));
                }
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将配置动态写入配置文件，这个方法是串行的
     *
     * @throws Exception
     */
    public synchronized static void writeConfig() throws Exception {
        File file = new File(getConfigFilePath());
        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            new Yaml().dump(new TreeMap<>(getConfigMap()), new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        }
    }

    /**
     * 配置类模板，在第一次创建配置文件时会写入此模板
     */
    public static String TEMPLATE = "";

    static {
        InputStream inputStream = ConfigManager.class.getClassLoader().getResourceAsStream("config/config_template.yaml");
        try {
            assert inputStream != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                TEMPLATE += line + "\n";
            }
            TEMPLATE = TEMPLATE.replace("$CURRENT_VERSION", RobotConfig.CURRENT_VERSION);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            RobotConfig.logger.info("配置文件模板写入有误，请手动编写配置文件，可参考 Github 中的配置模板");
            System.exit(1);
        }
    }
}
