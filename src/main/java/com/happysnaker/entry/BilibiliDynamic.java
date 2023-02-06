package com.happysnaker.entry;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * Bilibili 动态更新实体类，动态不单单指 UP 个人中心动态，也可以泛指番剧更新或其他内容更新
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class BilibiliDynamic {
    /**
     * 更新时间
     */
    public long pubTime;
    /**
     * 推送原因
     */
    public String pubAction;
    /**
     * 作者或内容名称
     */
    public String authName;
    /**
     * 发布时头像
     */
    public String face;
    /**
     * 封面，如果有多张，只会选取第一张
     */
    public String cover;
    /**
     * 描述
     */
    public String desc;
    /**
     * ID
     */
    public String id;
    /**
     * 跳转链接
     */
    public String jumpUrl;
    /**
     * 动态类型
     * <ul>
     *     <li>0：发布图片动态</li>
     *     <li>1：投稿文章</li>
     *     <li>2：投稿视频</li>
     *     <li>3：直播</li>
     *     <li>4：转发动态</li>
     *     <li>5：番剧更新</li>
     *     <li>5：发布文字动态</li>
     * </ul>
     */
    public int type;
    /**
     * 发布动态时上传的图片链接
     */
    public List<String> images;

    @Override
    public String toString() {
        return "BilibiliDynamic{" +
                "pubTime=" + pubTime +
                ", pubAction='" + pubAction + '\'' +
                ", authName='" + authName + '\'' +
                ", face='" + face + '\'' +
                ", cover='" + cover + '\'' +
                ", desc='" + desc + '\'' +
                ", id='" + id + '\'' +
                ", jumpUrl='" + jumpUrl + '\'' +
                ", type=" + type +
                ", images=" + images +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BilibiliDynamic that = (BilibiliDynamic) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}