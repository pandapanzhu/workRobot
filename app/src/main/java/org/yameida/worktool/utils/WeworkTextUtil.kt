package org.yameida.worktool.utils

import android.view.accessibility.AccessibilityNodeInfo
import com.blankj.utilcode.util.LogUtils
import org.yameida.worktool.Constant
import org.yameida.worktool.model.WeworkMessageBean
import org.yameida.worktool.service.WeworkController
import org.yameida.worktool.service.getRoot
import org.yameida.worktool.service.sleep
import org.yameida.worktool.utils.AccessibilityUtil.findAllByClazz
import org.yameida.worktool.utils.AccessibilityUtil.findAllOnceByClazz
import java.util.*
import kotlin.collections.ArrayList

/**
 * 消息特征分析工具类
 */
object WeworkTextUtil {

    /*
文字 1tv 0iv (文字)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.LinearLayout
------------ depth: 4 className: android.widget.FrameLayout
--------------- depth: 5 className: android.widget.TextView

图片 0tv 1iv (图片)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.ImageView

视频 2tv 2iv (视频大小、视频时长、缩略图、播放按钮)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.ImageView
------------ depth: 4 className: android.widget.ImageView
------------ depth: 4 className: android.view.View
------------ depth: 4 className: android.widget.TextView
------------ depth: 4 className: android.widget.TextView

腾讯文档 2tv 2iv (标题、创建者、图标、缩略图) (需要和视频区分 视频子节点数>3)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.RelativeLayout
--------------- depth: 5 className: android.widget.LinearLayout
------------------ depth: 6 className: android.widget.LinearLayout
--------------------- depth: 7 className: android.widget.TextView
--------------------- depth: 7 className: android.widget.TextView
------------------ depth: 6 className: android.widget.ImageView
--------------- depth: 5 className: android.widget.ImageView

链接 3tv 1iv (标题、副标题、下方来源、图标)
发送纯文本但含链接被识别为网页 (纯文本、网页title、网址域名、图标)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.TextView
------------ depth: 4 className: android.widget.RelativeLayout
--------------- depth: 5 className: android.widget.LinearLayout
------------------ depth: 6 className: android.widget.TextView
--------------- depth: 5 className: android.widget.ImageView
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.view.View
--------- depth: 3 className: android.widget.TextView

文件 3tv 1iv (文件名、文件大小、下方来源、图标) (需要和链接区分 文件大小特征匹配)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.RelativeLayout
--------------- depth: 5 className: android.widget.TextView
--------------- depth: 5 className: android.widget.TextView
------------ depth: 4 className: android.widget.ImageView
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.view.View
--------- depth: 3 className: android.widget.TextView

小程序 3tv 2iv (标题、副标题、下方来源、小程序icon、图标)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.LinearLayout
------------ depth: 4 className: android.widget.ImageView
------------ depth: 4 className: android.widget.TextView
--------- depth: 3 className: android.widget.TextView
--------- depth: 3 className: android.widget.ImageView
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.view.View
--------- depth: 3 className: android.widget.TextView

合并聊天记录 2tv 0iv (标题、摘要)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.TextView
------------ depth: 4 className: android.widget.TextView

收集表 6tv 0iv (标题、副标题、行1、行2、行3、下方来源)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.TextView
------ depth: 2 className: android.widget.TextView
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.TextView
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.TextView
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.TextView
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.view.View
--------- depth: 3 className: android.widget.TextView

接龙 2tv 1iv (内容、接龙标识、跳转按钮)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.LinearLayout
------------ depth: 4 className: android.widget.TextView
--------- depth: 3 className: android.widget.LinearLayout
------------ depth: 4 className: android.widget.TextView
------------ depth: 4 className: android.widget.ImageView

语音 4tv 2iv (空、语音时长、转写文字、转写状态、语音图片、转写图标)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.LinearLayout
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.TextView
------------ depth: 4 className: android.widget.RelativeLayout
--------------- depth: 5 className: android.widget.RelativeLayout
------------------ depth: 6 className: android.widget.ImageView
--------------- depth: 5 className: android.widget.ImageView
------------ depth: 4 className: android.widget.TextView
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.TextView
--------- depth: 3 className: android.widget.TextView

名片 5tv 1iv (机构名、姓名、别名、职务、下方来源、头像)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.TextView
--------- depth: 3 className: android.widget.LinearLayout
------------ depth: 4 className: android.widget.TextView
--------- depth: 3 className: android.widget.TextView
--------- depth: 3 className: android.widget.TextView
------ depth: 2 className: android.widget.ImageView
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.view.View
--------- depth: 3 className: android.widget.TextView

位置 1tv 2iv (地址、位置、定位柄)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.RelativeLayout
--------- depth: 3 className: android.widget.FrameLayout
------------ depth: 4 className: android.widget.ImageView
--------- depth: 3 className: android.widget.TextView
--------- depth: 3 className: android.widget.ImageView

带回复引用文本 3tv 0iv (引用发言人、引用发言内容、本次消息内容)
 depth: 0 className: android.widget.RelativeLayout
--- depth: 1 className: android.widget.RelativeLayout
------ depth: 2 className: android.widget.LinearLayout
--------- depth: 3 className: android.widget.RelativeLayout
------------ depth: 4 className: android.widget.LinearLayout
--------------- depth: 5 className: android.widget.RelativeLayout
------------------ depth: 6 className: android.view.View
------------------ depth: 6 className: android.widget.RelativeLayout
--------------------- depth: 7 className: android.widget.TextView
--------------------- depth: 7 className: android.widget.LinearLayout
------------------------ depth: 8 className: android.widget.RelativeLayout
--------------------------- depth: 9 className: android.widget.RelativeLayout
------------------------------ depth: 10 className: android.widget.TextView
--------------- depth: 5 className: android.widget.TextView

------------------------------总结------------------------------
图片 0tv 1iv (图片)
视频 2tv 2iv (视频大小、视频时长、缩略图、播放按钮)
链接 3tv 1iv (标题、副标题、下方来源、图标)
文件 3tv 1iv (文件名、文件大小、下方来源、图标) (需要和链接区分)
链接 2tv 1iv (文件名、副标题、图标) (微信*用户发的链接不带下方来源 需要和接龙和链接区分)
链接 1tv 1iv (文件名、图标) (微信*用户发的链接不带副标题和下方来源)
文件 2tv 1iv (文件名、文件大小、图标) (微信*用户发的文件不带下方来源 需要和接龙和链接区分)
小程序 3tv 2iv (标题、副标题、下方来源、小程序icon、图标)
合并聊天记录 2tv 0iv (标题、摘要)
收集表 6tv 0iv (标题、副标题、行1、行2、行3、下方来源)
接龙 2tv 1iv (内容、接龙标识、跳转按钮)
语音 4tv 2iv (空、语音时长、转写文字、转写状态、语音图片、转写图标)
名片 5tv 1iv (机构名、姓名、别名、职务、下方来源、头像)
位置 1tv 2iv (地址、位置、定位柄)
带回复引用文本 1tv 2iv (引用发言人、引用发言内容、本次消息内容)
     */

    /**
     * 企微消息类型 TEXT_TYPE
     * @see WeworkMessageBean.TEXT_TYPE
     */
    fun getTextType(node: AccessibilityNodeInfo?, isGroup: Boolean = true): Int {
        if (node == null) return WeworkMessageBean.TEXT_TYPE_UNKNOWN
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= Constant.LONG_INTERVAL) {
            node.refresh()
            AccessibilityUtil.findOnceByClazz(node, Views.ProgressBar) ?: break
            LogUtils.e("发现加载项 等待加载完成...")
            sleep(Constant.POP_WINDOW_INTERVAL / 5)
        }
        val tvList = findAllOnceByClazz(node, Views.TextView)
        val tvCount = tvList.size
        val ivCount = findAllOnceByClazz(node, Views.ImageView).size
        LogUtils.v("tvCount: $tvCount ivCount: $ivCount")
        return when {
            tvCount == 1 && ivCount == 0 -> WeworkMessageBean.TEXT_TYPE_PLAIN
            tvCount == 1 && ivCount == 1 -> WeworkMessageBean.TEXT_TYPE_LINK
            tvCount == 0 && ivCount == 1 -> WeworkMessageBean.TEXT_TYPE_IMAGE
            tvCount == 2 && ivCount == 2 -> {
                val parent = tvList[0].parent
                if ((tvList[0].text?.toString() ?: "").matches("直播中".toRegex())) {
                    WeworkMessageBean.TEXT_TYPE_CHANNELS_LIVE
                } else if (parent != null && parent.childCount > 3) {
                    WeworkMessageBean.TEXT_TYPE_VIDEO
                } else {
                    WeworkMessageBean.TEXT_TYPE_OFFICE
                }
            }
            tvCount == 3 && ivCount == 1 -> {
                if (isFileSize(tvList[1].text?.toString())) {
                    WeworkMessageBean.TEXT_TYPE_FILE
                } else {
                    WeworkMessageBean.TEXT_TYPE_LINK
                }
            }
            tvCount == 3 && ivCount == 2 -> WeworkMessageBean.TEXT_TYPE_MICROPROGRAM
            tvCount == 2 && ivCount == 0 -> WeworkMessageBean.TEXT_TYPE_CHAT_RECORD
            tvCount == 6 && ivCount == 0 -> WeworkMessageBean.TEXT_TYPE_COLLECTION
            tvCount == 2 && ivCount == 1 -> {
                if (isSolitaire(tvList[1].text?.toString())) {
                    WeworkMessageBean.TEXT_TYPE_SOLITAIRE
                } else if (isFileSize(tvList[1].text?.toString())) {
                    WeworkMessageBean.TEXT_TYPE_FILE
                } else {
                    WeworkMessageBean.TEXT_TYPE_LINK
                }
            }
            tvCount == 4 && ivCount == 2 -> WeworkMessageBean.TEXT_TYPE_VOICE
            tvCount == 5 && ivCount == 1 -> WeworkMessageBean.TEXT_TYPE_CARD
            tvCount == 1 && ivCount == 2 -> {
                if ((tvList[0].text?.toString() ?: "").matches("[0-9]+:[0-9]+".toRegex()))
                    WeworkMessageBean.TEXT_TYPE_VIDEO
                else
                    WeworkMessageBean.TEXT_TYPE_LOCATION
            }
            tvCount == 3 && ivCount == 0 -> WeworkMessageBean.TEXT_TYPE_REPLY
            tvCount == 0 && ivCount == 0 -> WeworkMessageBean.TEXT_TYPE_NOTIFY_ROBOT
            tvCount == 1 && ivCount == 3 -> WeworkMessageBean.TEXT_TYPE_CHANNELS_VIDEO
            else -> WeworkMessageBean.TEXT_TYPE_UNKNOWN
        }
    }

    /**
     * 企微消息类型 TEXT_TYPE
     * @see WeworkMessageBean.TEXT_TYPE
     */
    fun getTextTypeFromItem(node: AccessibilityNodeInfo?, isGroup: Boolean = true): Int {
        if (node == null) return WeworkMessageBean.TEXT_TYPE_UNKNOWN
        //消息主体
        val relativeLayoutItem = AccessibilityUtil.findOnceByClazz(node, Views.RelativeLayout, limitDepth = 1)
        if (relativeLayoutItem != null && relativeLayoutItem.childCount >= 2) {
            if (Views.ImageView.equals(relativeLayoutItem.getChild(relativeLayoutItem.childCount - 2).className)) {
                LogUtils.v("头像在左边 本条消息发送者为其他联系人")
                var textType = WeworkMessageBean.TEXT_TYPE_UNKNOWN
                val relativeLayoutContent =
                    AccessibilityUtil.findOnceByClazz(relativeLayoutItem, Views.RelativeLayout, limitDepth = 2)
                if (relativeLayoutContent != null) {
                    textType = getTextType(relativeLayoutContent)
                    LogUtils.v("textType: $textType")
                    return textType
                }
            } else if (Views.ImageView.equals(relativeLayoutItem.getChild(relativeLayoutItem.childCount - 1).className)) {
                LogUtils.v("头像在右边 本条消息发送者为自己")
                var textType = WeworkMessageBean.TEXT_TYPE_UNKNOWN
                val subLayout = relativeLayoutItem.getChild(relativeLayoutItem.childCount - 2)
                if (subLayout.childCount > 0) {
                    textType = WeworkTextUtil.getTextType(subLayout)
                    LogUtils.v("textType: $textType")
                    return textType
                }
            }
        }
        return WeworkMessageBean.TEXT_TYPE_UNKNOWN
    }

    /**
     * 企微消息 发送者
     * sender 0其他人 1机器人自己 2unknown(如系统消息)
     */
    private fun getSender(node: AccessibilityNodeInfo?, isGroup: Boolean = true): Int {
        if (node == null) return WeworkMessageBean.TEXT_TYPE_UNKNOWN
        //消息主体
        val relativeLayoutItem = AccessibilityUtil.findOnceByClazz(node, Views.RelativeLayout, limitDepth = 1)
        if (relativeLayoutItem != null && relativeLayoutItem.childCount >= 2) {
            if (Views.ImageView.equals(relativeLayoutItem.getChild(0).className)) {
                return 0
            } else if (Views.ImageView.equals(relativeLayoutItem.getChild(1).className)) {
                return 1
            }
        }
        return 2
    }

    /**
     * 是否为消息上方时间
     */
    fun isDate(date: String): Boolean {
        return date.matches(".*?([上下]午)[\\s ]+?[0-9]+:[0-9]+".toRegex())
    }

    /**
     * 是否为文件上方时间
     */
    fun isFileSize(size: String?): Boolean {
        return size?.matches("[0-9\\.]+[BKMG]".toRegex()) ?: false
    }

    /**
     * 是否为接龙
     */
    fun isSolitaire(text: String?): Boolean {
        return text?.contains("参与接龙") ?: false
    }

    /**
     * 群聊 提取发言人昵称
     * 适用于左侧发言者
     * @param item 消息item节点
     */
    fun getNameList(item: AccessibilityNodeInfo): List<String> {
        val nameList = ArrayList<String>()
        val node = AccessibilityUtil.findOnceByClazz(item, Views.ViewGroup)
        if (node != null) {
            val textViewList = findAllOnceByClazz(node, Views.TextView)
            for (textView in textViewList) {
                if (textView.text != null) {
                    nameList.add(textView.text.toString())
                }
            }
        }
        return nameList
    }

    /**
     * 长按消息条目
     * 复制、转发、回复、收藏、置顶、多选、日程、待办、翻译、删除
     * 适用左侧发言者
     * @param node 消息列表节点
     * @param replyTextType 带回复消息类型
     * @param replyNick 待回复人姓名
     * @param replyContent 待回复内容
     * @param key 复制、转发、回复、收藏、多选
     * @return true 进行了长按 否则 false
     */
    fun longClickMessageItem(
        node: AccessibilityNodeInfo?,
        replyTextType: Int,
        replyNick: String?,
        replyContent: String,
        vararg key: String
    ): Boolean {
        if (node == null) return false
        for (i in 0 until node.childCount) {
            val item = node.getChild(node.childCount - 1 - i) ?: continue
            val nameList = getNameList(item)
            if (nameList.isEmpty()) {
                val backNode = getMessageListNode(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT)
                if (backNode != null) {
                    val textTypeFromItem = getTextTypeFromItem(item)
                    val sender = getSender(item)
                    if ((replyNick != null && sender == 1) || (replyNick == null && sender == 0)) {
                        continue
                    }
                    if ((replyTextType == WeworkMessageBean.TEXT_TYPE_IMAGE)
                        && (replyTextType == textTypeFromItem)) {
                        LogUtils.d("nameList: $nameList\nreplyContent: $replyContent")
                        return longClickMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT, *key)
                    }
                    if ((replyTextType == WeworkMessageBean.TEXT_TYPE_FILE || replyTextType == WeworkMessageBean.TEXT_TYPE_VIDEO)
                        && replyContent.contains("###")) {
                        val replyContentList = replyContent.split("###")
                        if (AccessibilityUtil.findOnceByText(backNode, replyContentList[0]) != null
                            && AccessibilityUtil.findOnceByText(backNode, replyContentList[1]) != null) {
                            LogUtils.d("nameList: $nameList\nreplyContent: $replyContent")
                            return longClickMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP, *key)
                        }
                    }
                    val textNode = AccessibilityUtil.findOnceByText(backNode, replyContent)
                    if (textNode != null && replyContent.isNotEmpty()) {
                        LogUtils.d("nameList: $nameList\nreplyContent: $replyContent")
                        return longClickMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT, *key)
                    }
                }
            }
            for (name in nameList) {
                if (name == replyNick) {
                    val backNode = getMessageListNode(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP)
                    if (backNode != null) {
                        val textTypeFromItem = getTextTypeFromItem(item)
                        if ((replyTextType == WeworkMessageBean.TEXT_TYPE_IMAGE)
                            && (replyTextType == textTypeFromItem)) {
                            LogUtils.d("nameList: $nameList\nreplyContent: $replyContent")
                            return longClickMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP, *key)
                        }
                        if ((replyTextType == WeworkMessageBean.TEXT_TYPE_FILE || replyTextType == WeworkMessageBean.TEXT_TYPE_VIDEO)
                            && replyContent.contains("###")) {
                            val replyContentList = replyContent.split("###")
                            if (AccessibilityUtil.findOnceByText(backNode, replyContentList[0]) != null
                                && AccessibilityUtil.findOnceByText(backNode, replyContentList[1]) != null) {
                                LogUtils.d("nameList: $nameList\nreplyContent: $replyContent")
                                return longClickMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP, *key)
                            }
                        }
                        val textNode = AccessibilityUtil.findOnceByText(backNode, replyContent)
                        if (textNode != null && replyContent.isNotEmpty()) {
                            LogUtils.d("nameList: $nameList\nreplyContent: $replyContent")
                            return longClickMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP, *key)
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * 长按消息条目
     * 复制、转发、回复、收藏、置顶、多选、日程、待办、翻译、删除、撤回
     * 适用自己发言者
     * @param node 消息列表节点
     * @param replyTextType 带回复消息类型
     * @param replyContent 待回复内容
     * @param key 复制、转发、回复、收藏、多选
     * @return true 进行了长按 否则 false
     */
    fun longClickMyMessageItem(
        node: AccessibilityNodeInfo?,
        replyTextType: Int,
        replyContent: String,
        key: String
    ): Boolean {
        if (node == null) return false
        for (i in 0 until node.childCount) {
            val item = node.getChild(node.childCount - 1 - i) ?: continue
            val frontNode = getMyMessageListNode(item)
            if (frontNode != null) {
                val textType = getTextTypeFromItem(item)
                if (replyTextType == WeworkMessageBean.TEXT_TYPE_UNKNOWN || replyTextType == textType) {
                    if (replyTextType == WeworkMessageBean.TEXT_TYPE_IMAGE) {
                        return longClickMyMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT, key)
                    }
                    if ((replyTextType == WeworkMessageBean.TEXT_TYPE_FILE || replyTextType == WeworkMessageBean.TEXT_TYPE_VIDEO)
                        && replyContent.contains("###")) {
                        val replyContentList = replyContent.split("###")
                        if (AccessibilityUtil.findOnceByText(frontNode, replyContentList[0]) != null
                            && AccessibilityUtil.findOnceByText(frontNode, replyContentList[1]) != null) {
                            return longClickMyMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP, key)
                        }
                    }
                    val textNode = AccessibilityUtil.findOnceByText(frontNode, replyContent, exact = true)
                    if (textNode != null && replyContent.isNotEmpty()) {
                        return longClickMyMessageItem(item, WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT, key)
                    }
                }
            }
        }
        return false
    }

    private fun longClickMessageItem(item: AccessibilityNodeInfo, roomType: Int, vararg key: String): Boolean {
        val backNode = getMessageListNode(item, roomType)
        if ("单击" in key) {
            return AccessibilityUtil.clickByNode(WeworkController.weworkService, backNode)
        }
        AccessibilityUtil.performLongClickWithSon(backNode)
        sleep(Constant.POP_WINDOW_INTERVAL)
        val optionRvList = findAllByClazz(getRoot(), Views.RecyclerView, Views.ViewGroup)
        for (optionRv in optionRvList) {
            val keyTv = AccessibilityUtil.findOnceByText(optionRv, *key, exact = true)
            if (keyTv != null) {
                AccessibilityUtil.performClick(keyTv)
                return true
            }
        }
        return false
    }

    private fun longClickMyMessageItem(item: AccessibilityNodeInfo, roomType: Int, key: String): Boolean {
        val frontNode = getMyMessageListNode(item)
        if (key == "单击") {
            return AccessibilityUtil.clickByNode(WeworkController.weworkService, frontNode)
        }
        AccessibilityUtil.performLongClickWithSon(frontNode)
        sleep(Constant.POP_WINDOW_INTERVAL)
        val optionRvList = findAllByClazz(getRoot(), Views.RecyclerView, Views.ViewGroup)
        for (optionRv in optionRvList) {
            val keyTv = AccessibilityUtil.findOnceByText(optionRv, key, exact = true)
            if (keyTv != null) {
                AccessibilityUtil.performClick(keyTv)
                if (AccessibilityExtraUtil.loadingPage("CustomDialog", timeout = Constant.POP_WINDOW_INTERVAL)) {
                    AccessibilityUtil.findTextAndClick(getRoot(), "确定", "我知道了", exact = true)
                }
                return true
            }
        }
        return false
    }

    /**
     * 提取消息主体框节点(昵称下面的气泡框)
     * 适用于左侧发言者
     * @param item 消息item节点
     */
    private fun getMessageListNode(item: AccessibilityNodeInfo, roomType: Int): AccessibilityNodeInfo? {
        if (roomType in arrayOf(WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT, WeworkMessageBean.ROOM_TYPE_EXTERNAL_CONTACT)) {
            val node = AccessibilityUtil.findOnceByClazz(item, Views.ImageView)
            if (node != null) {
                return AccessibilityUtil.findBackNode(node)
            }
        } else if (roomType in arrayOf(WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP, WeworkMessageBean.ROOM_TYPE_EXTERNAL_GROUP)) {
            val node = AccessibilityUtil.findOnceByClazz(item, Views.ViewGroup)
            if (node != null) {
                return AccessibilityUtil.findBackNode(node)
            }
        }
        return null
    }

    /**
     * 提取消息主体框节点(昵称下面的气泡框)
     * 适用于自己发言者
     * @param item 消息item节点
     */
    private fun getMyMessageListNode(item: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val node = AccessibilityUtil.findAllOnceByClazz(item, Views.ImageView).lastOrNull()
        if (node?.parent?.getChild(0) != node) {
            return AccessibilityUtil.findFrontNode(node)
        }
        return null
    }
}