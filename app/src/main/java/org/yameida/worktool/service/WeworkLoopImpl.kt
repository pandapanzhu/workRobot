package org.yameida.worktool.service

import android.graphics.Bitmap
import android.os.Message
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.text.isDigitsOnly
import com.blankj.utilcode.util.*
import com.hjq.toast.ToastUtils
import org.yameida.worktool.Constant
import org.yameida.worktool.Demo
import org.yameida.worktool.MyApplication
import org.yameida.worktool.activity.GetScreenShotActivity
import org.yameida.worktool.model.WeworkMessageBean
import org.yameida.worktool.observer.MultiFileObserver
import org.yameida.worktool.service.WeworkController.mainLoopRunning
import org.yameida.worktool.utils.*
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

/**
 * 获取数据类型 201 202 主循环
 */
object WeworkLoopImpl {

    val stopWords = arrayListOf("解析中")
    var logIndex = 0

    fun mainLoop() {
        if (!WeworkController.enableLoopRunning)
            return
        mainLoopRunning = true
        try {
            while (mainLoopRunning) {
                if (!isAtHome()) {
                    LogUtils.d("当前在房间: ")
                    getChatMessageList(needSend = true)
                    if (mainLoopRunning) {
                        goHome()
                    }
                    continue
                }
                if (!mainLoopRunning) break
                if (getChatroomList()) {
                    LogUtils.d("进入点击进入聊天页: ")
                    AccessibilityUtil.waitForPageMissing("WwMainActivity", "GlobalSearchActivity")
                    if (!getChatMessageList()) {
                        sleep(Constant.POP_WINDOW_INTERVAL)
                        LogUtils.d("重试获取聊天列表: ")
                        getChatMessageList()
                    }
                }
                if (!mainLoopRunning) break
                getFriendRequest()
                if (!mainLoopRunning) break
                checkRealName()
                if (!mainLoopRunning) break
                sleep(5000)
            }
        } catch (e: Exception) {
            LogUtils.e("ERROR mainLoop: " + e.message, e)
            error("ERROR mainLoop: $e")
            sleep(Constant.LONG_INTERVAL)
            MyLooper.getInstance().removeMessages(WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE)
            MyLooper.getInstance().sendMessage(Message.obtain().apply {
                what = WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE
                obj =
                    WeworkMessageBean().apply { type = WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE }
            })
        }
    }

    /**
     * 检查账号是否已实名 & 新号使用模拟环境
     * @return 通过检查 true 否则 false
     */
    private fun checkRealName(): Boolean {
        if (!SPUtils.getInstance("myInfo").getBoolean("realName", true)) {
            LogUtils.e("账号实名前请先关闭WorkTool主功能！")
            ToastUtils.show("账号实名前请先关闭WorkTool主功能！")
            MyApplication.launchIntent()
            sleep(5000)
            return false
        }
        if (SPUtils.getInstance().getBoolean("risk", false)) {
            LogUtils.e("环境监测异常，请勿使用本应用！")
            ToastUtils.show("环境监测异常，请勿使用本应用！")
            MyApplication.launchIntent()
            sleep(5000)
            return false
        }
        return true
    }

    /**
     * 读取通讯录好友请求
     */
    private fun getFriendRequest(): Boolean {
        val list = AccessibilityUtil.findAllOnceByText(getRoot(), "通讯录", exact = true)
        for (item in list) {
            val childCount = item.parent?.parent?.parent?.childCount
            if (childCount == 4 || childCount == 5) {
                if (item.parent != null && item.parent.childCount > 1) {
                    LogUtils.d("通讯录有红点")
                    AccessibilityUtil.performClick(item)
                    val hasRecommendFriend = AccessibilityUtil.findOneByText(
                        getRoot(),
                        "可能的同事",
                        exact = true,
                        timeout = Constant.POP_WINDOW_INTERVAL
                    )
                    if (hasRecommendFriend != null) {
                        LogUtils.d("有可能认识的人")
                        AccessibilityUtil.performClick(
                            AccessibilityUtil.findBackNode(
                                hasRecommendFriend
                            )
                        )
                        goHome()
                        return false
                    }
                    val addButton = AccessibilityUtil.findOneByText(getRoot(), "添加")
                    var parent = addButton
                    var son = addButton
                    while (parent != null && parent.className != Views.RecyclerView && parent.className != Views.ListView) {
                        son = parent
                        parent = parent.parent
                    }
                    if (parent != null && AccessibilityUtil.findAllOnceByClazz(
                            son,
                            Views.TextView
                        ).size > 1
                    ) {
                        LogUtils.d("有待添加客户")
                        AccessibilityUtil.performClick(son)
                        AccessibilityUtil.findTextAndClick(getRoot(), "新的")
                        sleep(Constant.POP_WINDOW_INTERVAL)
                        var retry = 5
                        while (retry-- > 0) {
                            val checkButton =
                                AccessibilityUtil.findOneByText(getRoot(), "查看", timeout = 2000)
                            if (checkButton == null) {
                                break
                            } else {
                                AccessibilityUtil.performClick(checkButton)
                                sleep(Constant.CHANGE_PAGE_INTERVAL)
                                val nameList = passFriendRequest()
                                if (nameList.isEmpty())
                                    break
                                //todo 可自定义执行任务
//                                Demo.test2(nameList[0])
                            }
                        }
                        return true
                    } else {
                        LogUtils.d("未发现待添加客户")
                    }
                } else {
                    LogUtils.v("通讯录无红点")
                }
            }
        }
        return false
    }

    /**
     * 聊天页
     * 1.获取群名
     * 2.获取消息列表
     * @param needInfer 是否需要推断@me并等待回复
     * @param timeout 在房间内等待回复的时长
     */
    fun getChatMessageList(
        needInfer: Boolean = !Constant.pushImage,
        imageCheck: Boolean = true,
        timeout: Long = 5000,
        titleList: ArrayList<String>? = null,
        needSend: Boolean = false
    ): Boolean {
        if (Constant.autoReply == 0) return true
        val roomType = WeworkRoomUtil.getRoomType()
        var titleList = titleList ?: WeworkRoomUtil.getRoomTitle()
        if (titleList.count { it.endsWith("…") } > 0) {
            LogUtils.d("title too long... try get full name titleList: ${titleList.joinToString()}")
            if (roomType == WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT || roomType == WeworkMessageBean.ROOM_TYPE_EXTERNAL_CONTACT) {
                titleList = WeworkRoomUtil.getFriendName()
            } else if (Constant.fullGroupName
                && (roomType == WeworkMessageBean.ROOM_TYPE_INTERNAL_GROUP || roomType == WeworkMessageBean.ROOM_TYPE_EXTERNAL_GROUP)
            ) {
                titleList = WeworkRoomUtil.getGroupName()
            }
        }
        if (roomType != WeworkMessageBean.ROOM_TYPE_UNKNOWN && titleList.size > 0) {
            val title = titleList.joinToString()
            LogUtils.v("聊天: $title")
            log("聊天: $title")
            val messageList = arrayListOf<WeworkMessageBean.SubMessageBean>()
            val messageList2 = arrayListOf<WeworkMessageBean.SubMessageBean>()
            val imageSet = LinkedHashSet<ByteArray>()
            do {
                messageList.clear()
                messageList2.clear()
                imageSet.clear()
                //聊天消息列表 1ListView 0RecycleView xViewGroup
                val list = AccessibilityUtil.findOneByClazz(getRoot(), Views.ListView)
                if (list != null) {
                    val childCount = list.childCount
                    LogUtils.v("消息条数: $childCount")
                    for (i in 0 until list.childCount) {
                        val item = list.getChild(childCount - 1 - i)
                        if (item != null && item.childCount > 0) {
                            messageList.add(parseChatMessageItem(item, titleList, roomType))
                        }
                    }
                }
                sleep(Constant.POP_WINDOW_INTERVAL / 5)
                LogUtils.v("双重校验聊天列表")
                val list2 = AccessibilityUtil.findOneByClazz(getRoot(), Views.ListView)
                if (list2 != null) {
                    val childCount = list2.childCount
                    LogUtils.v("list2消息条数: $childCount")
                    var imageCheck = imageCheck
                    if (Constant.enableMediaProject) {
                        for (i in 0 until childCount) {
                            val item = list2.getChild(childCount - 1 - i)
                            if (item != null && item.childCount > 0) {
                                val chatMessageItem = parseChatMessageItem(
                                    item,
                                    titleList,
                                    roomType,
                                    imageCheck,
                                    lastParse = true
                                )
                                if (chatMessageItem.imageRepeat == true) {
                                    chatMessageItem.imageRepeat = null
                                    imageCheck = false
                                }
                                if (chatMessageItem.image != null) {
                                    imageSet.add(chatMessageItem.image)
                                    chatMessageItem.image = null
                                    chatMessageItem.imageSize = null
                                }
                                messageList2.add(chatMessageItem)
                            }
                        }
                    } else {
                        for (i in 0 until list2.childCount) {
                            val item = list2.getChild(childCount - 1 - i)
                            if (item != null && item.childCount > 0) {
                                val chatMessageItem = parseChatMessageItem(
                                    item,
                                    titleList,
                                    roomType,
                                    imageCheck,
                                    lastParse = true
                                )
                                if (chatMessageItem.sender == 0 && chatMessageItem.textType == WeworkMessageBean.TEXT_TYPE_IMAGE) {
                                    imageCheck = false
                                }
                                messageList2.add(chatMessageItem)
                            }
                        }
                    }
                }
                if (messageList != messageList2) {
                    LogUtils.e("双重校验聊天列表失败")
                }
            } while (messageList != messageList2)
            if (messageList.isNotEmpty()) {
                messageList.reverse()
                val lastMessage = messageList.last()
                val prefix = (lastMessage.nameList.firstOrNull()
                    ?.replace("\\(.*\\)$".toRegex(), "") + ": ").replace("null:", "")
                val lastSyncMessage =
                    prefix + if (lastMessage.textType == WeworkMessageBean.TEXT_TYPE_IMAGE) {
                        "[图片]"
                    } else {
                        lastMessage.itemMessageList.lastOrNull()?.text
                    }
                SPUtils.getInstance("lastSyncMessage").put(titleList[0], lastSyncMessage)
                LogUtils.v("lastSyncMessage: ${titleList[0]}: $lastSyncMessage")
                if (Constant.enableMediaProject && Constant.pushImage) {
                    log("image: ${imageSet.size}")
                    if (imageSet.isNotEmpty()) {
                        val imageMessageList =
                            messageList.filter { it.textType == WeworkMessageBean.TEXT_TYPE_IMAGE }
                                .reversed()
                        LogUtils.v("lastImage: ${imageSet.first().size}")
                        SPUtils.getInstance("lastImage")
                            .put(titleList.joinToString(), imageSet.first().size)
                        imageSet.reversed().forEachIndexed { index, bytes ->
                            if (imageMessageList.size > index) {
                                val message = imageMessageList[index]
                            }
                        }
                    }
                    for (subMessageBean in messageList) {
                        subMessageBean.image = null
                        subMessageBean.imageSize = null
                        subMessageBean.imageRepeat = null
                    }
                } else if (!Constant.enableMediaProject && Constant.pushImage) {
                    log("createSet: ${MultiFileObserver.createSet.joinToString()}\nsaveSet: ${MultiFileObserver.saveSet.joinToString()}")
                    if (MultiFileObserver.saveSet.isNotEmpty()) {
                        val imageMessageList =
                            messageList.filter { it.textType == WeworkMessageBean.TEXT_TYPE_IMAGE }
                                .reversed()
                        MultiFileObserver.saveSet.reversed().forEachIndexed { index, targetPath ->
                            if (imageMessageList.size > index) {
                                val message = imageMessageList[index]
                                val save2Album = ImageUtils.save2Album(
                                    ImageUtils.getBitmap(targetPath),
                                    Bitmap.CompressFormat.PNG,
                                    true
                                )
                                LogUtils.v("save2Album $save2Album")
                            }
                        }
                        MultiFileObserver.saveSet.clear()
                    }
                    messageList.removeIf { it.textType == WeworkMessageBean.TEXT_TYPE_IMAGE }
                }
                if (needSend) {
                    WeworkController.weworkService.webSocketManager.send(
                        WeworkMessageBean(
                            null, null,
                            WeworkMessageBean.TYPE_RECEIVE_MESSAGE_LIST,
                            roomType,
                            titleList,
                            messageList,
                            null
                        )
                    )
                }

                mainLoopRunning = true;
                //推测是否回复并在房间等待指令
//                if (needInfer) {
//                    val lastMessage = messageList.lastOrNull()
//                    if (lastMessage != null && lastMessage.sender == 0) {
//                        when (Constant.replyStrategy) {
//                            1 -> {
//                                var tempContent = ""
//                                for (itemMessage in lastMessage.itemMessageList) {
//                                    if (itemMessage.text.contains("@" + Constant.myName)) {
//                                        tempContent = itemMessage.text
//                                    }
//                                }
//                                if (roomType == WeworkMessageBean.ROOM_TYPE_EXTERNAL_CONTACT
//                                    || roomType == WeworkMessageBean.ROOM_TYPE_INTERNAL_CONTACT
//                                    || tempContent.isNotBlank()
//                                ) {
//                                    LogUtils.v("推测需要回复: $tempContent")
//                                    val startTime = System.currentTimeMillis()
//                                    var currentTime = startTime
//                                    while (mainLoopRunning && currentTime - startTime < timeout) {
//                                        sleep(Constant.POP_WINDOW_INTERVAL / 5)
//                                        currentTime = System.currentTimeMillis()
//                                    }
//                                    return getChatMessageList(
//                                        needInfer = false,
//                                        titleList = titleList,
//                                        needSend = false
//                                    )
//                                }
//                            }
//
//                            2 -> {
//                                val startTime = System.currentTimeMillis()
//                                var currentTime = startTime
//                                while (mainLoopRunning && currentTime - startTime < timeout) {
//                                    sleep(Constant.POP_WINDOW_INTERVAL / 5)
//                                    currentTime = System.currentTimeMillis()
//                                }
//                                return getChatMessageList(
//                                    needInfer = false,
//                                    titleList = titleList,
//                                    needSend = false
//                                )
//                            }
//
//                            else -> return true
//                        }
//                    }
//                }
                return true
            } else {
                LogUtils.e("未找到聊天消息列表")
                error("未找到聊天消息列表")
            }
        } else if (Constant.autoPublishComment && WeworkController.weworkService.currentClass == "com.tencent.wework.moments.controller.MomentsIndexListActivity") {
            LogUtils.d("自动发表朋友圈")
            val tvGoPublish = AccessibilityUtil.findOneByText(getRoot(), "去发表", exact = true)
            if (tvGoPublish != null) {
                AccessibilityUtil.performClick(tvGoPublish)
                AccessibilityExtraUtil.loadingPage("MomentsEnterpriseNotificationListActivity")
                val tvPublishList = AccessibilityUtil.findAllByText(getRoot(), "发表", exact = true)
                LogUtils.d("发现${tvPublishList.size}条待发送")
                for (tvPublish in tvPublishList) {
                    AccessibilityUtil.performClick(tvPublish)
                }
            }
        } else {
            LogUtils.v("退出非聊天房间 ${WeworkController.weworkService.currentClass}")
        }
        return false
    }

    /**
     * 查看好友请求并通过
     */
    private fun passFriendRequest(): List<String> {
        val nameList = arrayListOf<String>()
        val imageView = AccessibilityUtil.findOneByClazz(getRoot(), Views.ImageView)
        if (imageView != null) {
            val textViewList =
                AccessibilityUtil.findAllOnceByClazz(imageView.parent, Views.TextView)
            val filter = textViewList.filter { it.text != null && it.text.toString() != "微信" }
            if (filter.isNotEmpty()) {
                val tvNick = filter[0]
                LogUtils.d("好友请求: " + tvNick.text)
                //设置标签
                if (AccessibilityUtil.findTextAndClick(getRoot(), "标签")) {
                    WeworkOperationImpl.setFriendTags(arrayListOf("worktool自动通过"))
                }
                AccessibilityUtil.findTextAndClick(getRoot(), "通过验证")
                var textNode = AccessibilityUtil.findOneByText(
                    getRoot(),
                    "完成",
                    "发消息",
                    "添加请求已过期，添加失败",
                    exact = true
                )
                if (textNode?.text?.toString() == "完成") {
                    AccessibilityUtil.performClick(textNode)
                }
                textNode = AccessibilityUtil.findOneByText(
                    getRoot(),
                    "发消息",
                    "添加请求已过期，添加失败",
                    exact = true
                )
                if (textNode?.text?.toString() == "添加请求已过期，添加失败") {
                    LogUtils.d("添加好友失败")
                } else {
                    val weworkMessageBean = WeworkMessageBean()
                    weworkMessageBean.type = WeworkMessageBean.GET_FRIEND_INFO
                    weworkMessageBean.friend = WeworkMessageBean.Friend().apply {
                        name = tvNick.text.toString()
                        newFriend = true
                    }
                    WeworkController.weworkService.webSocketManager.send(weworkMessageBean)
                    nameList.add(tvNick.text.toString())
                }
                //回到上一页
                var retry = 5
                while (retry-- > 0 && !isAtHome()) {
                    val textView = AccessibilityUtil.findOnceByText(
                        getRoot(),
                        "新的客户",
                        "新的居民",
                        "新的学员",
                        exact = true
                    )
                    if (textView == null) {
                        backPress()
                    }
                }
            }
        }
        return nameList
    }

    /**
     * 读取聊天列表
     */
    private fun getChatroomList(): Boolean {
        if (Constant.autoReply == 0) return true
        if (!isAtHome()) {
            goHome()
        }

        if (logIndex++ % 30 == 0) {
            LogUtils.d("读取首页聊天列表")
            if (logIndex % 120 == 0) log("读取首页聊天列表")
        }

        var hasNewMessage: AccessibilityNodeInfo? = null
        val list = AccessibilityUtil.findAllOnceByText(getRoot(), "消息", exact = true)
        for (item in list) {
            val childCount = item.parent?.parent?.parent?.childCount
            if (childCount == 4 || childCount == 5) {
                if (item.parent != null && item.parent.childCount > 1) {
                    LogUtils.d("消息有红点")
                    hasNewMessage = item
                }
            }
        }
        val listview = AccessibilityUtil.findOneByClazz(
            getRoot(),
            Views.RecyclerView,
            Views.ListView,
            Views.ViewGroup
        )
        if (listview != null && listview.childCount >= 2) {
            if (hasNewMessage != null) {
                //发现新消息
                if (checkUnreadChatRoom(listview)) {
                    //如果房间有红点 点击进入聊天页
                    return true
                } else {
                    AccessibilityUtil.clickByNode(WeworkController.weworkService, hasNewMessage)
                    sleep(Constant.POP_WINDOW_INTERVAL / 5)
                    AccessibilityUtil.clickByNode(WeworkController.weworkService, hasNewMessage)
                    sleep(Constant.POP_WINDOW_INTERVAL / 5)
                    //双击消息再试一次
                    if (checkUnreadChatRoom(listview)) {
                        //如果房间有红点 点击进入聊天页
                        return true
                    }
                }
            } else {
                //未发现新消息
                if (checkNoTipMessage(listview) == 1) {
                    //如果发现拉入群聊/修改群名/移出群聊 点击进入聊天页
                    return true
                } else if (checkNoSyncMessage(listview) == 1) {
                    //消息不一致
                    return true
                } else {
                    LogUtils.v("未发现新消息或无提示消息")
                }
            }
        } else {
            LogUtils.e("读取聊天列表失败")
            error("读取聊天列表失败")
        }
        if (logIndex % 600 == 0) {
            //让企微切换页面使APP保持活跃
            goHomeTab("通讯录")
            goHomeTab("消息")
            LogUtils.d("检查最近列表")
            log("检查最近列表")
            if (hasNewMessage()) {
                return false
            }
            var stop = false
            //滚动到顶端查看是否有无提示消息
            AccessibilityUtil.scrollToTop(
                WeworkController.weworkService,
                getRoot(),
                listener = object : AccessibilityUtil.OnScrollListener() {
                    override fun onScroll(): Boolean {
                        if (hasNewMessage()) {
                            stop = true
                            return true
                        }
                        return false
                    }
                })
            if (stop) {
                return false
            }
            if (listview != null && listview.childCount >= 2) {
                val onScrollListener = object : AccessibilityUtil.OnScrollListener() {
                    override fun onScroll(): Boolean {
                        if (hasNewMessage()) {
                            return true
                        }
                        if (checkNoTipMessage(listview) == 1) {
                            return true
                        }
                        if (checkNoSyncMessage(listview) != 0) {
                            return true
                        }
                        return false
                    }
                }
                //滚动前先获取一次
                onScrollListener.onScroll()
                AccessibilityUtil.scrollToBottom(
                    WeworkController.weworkService,
                    getRoot(),
                    listener = onScrollListener
                )
                LogUtils.v("回到消息列表顶部")
                for (item in list) {
                    val childCount = item.parent?.parent?.parent?.childCount
                    if (childCount == 4 || childCount == 5) {
                        AccessibilityUtil.clickByNode(WeworkController.weworkService, item)
                        sleep(Constant.POP_WINDOW_INTERVAL / 5)
                        AccessibilityUtil.clickByNode(WeworkController.weworkService, item)
                    }
                }
            }
        }
        return false
    }

    /**
     * 检查首页-聊天列表是否有未读红点并点击进入
     */
    private fun checkUnreadChatRoom(list: AccessibilityNodeInfo): Boolean {
        val spotNodeList = arrayListOf<AccessibilityNodeInfo>()
        for (i in 0 until list.childCount) {
            val item = list.getChild(i)
            if (item != null && Views.RelativeLayout.equals(item.className)) {
                if (item.childCount >= 2) {
                    val spotNode = item.getChild(1)
                    if (spotNode != null
                        && Views.TextView.equals(spotNode.className)
                        && spotNode.text != null
                        && spotNode.text.toString().replace("+", "").isDigitsOnly()
                    ) {
                        spotNodeList.add(spotNode)
                    }
                }
            }
        }
        if (spotNodeList.size > 0) {
            LogUtils.i("发现未读消息: " + spotNodeList.size + "条")
            log("发现未读消息: " + spotNodeList.size + "条")
            if (AccessibilityUtil.performClick(spotNodeList.firstOrNull())) {
                //进入聊天页 下一步 getChatMessageList
            } else {
                AccessibilityUtil.clickByNode(
                    WeworkController.weworkService,
                    spotNodeList.firstOrNull()?.parent
                )
            }
            return true
        } else {
            return false
        }
    }

    /**
     * 检查首页-聊天列表是否有拉入群聊/修改群名/移出群聊等无提示消息
     * @return -1当前列表不存在一周内消息 0未发现无提示消息 1发现无提示消息
     */
    private fun checkNoTipMessage(list: AccessibilityNodeInfo): Int {
        list.refresh()
        val listBriefList = arrayListOf<List<CharSequence>>()
        for (i in 0 until list.childCount) {
            val item = list.getChild(i)
            val tvList = AccessibilityUtil.findAllOnceByClazz(item, Views.TextView)
                .mapNotNull { it.text?.toString() }.filter { !it.startsWith("＠") }
            listBriefList.add(tvList)
            //tvList title/time/content
            if (tvList.size == 3) {
                //只查看最近一周内的消息
                if (tvList[1].isBlank() || tvList[1].contains("(刚刚)|(分钟前)|(上午)|(下午)|(昨天)|(星期)|(日程)|(会议)|(:)".toRegex())) {
                    if (tvList[2].contains("(退出了外部群)|(移出了群聊)|(邀请你加入了)|(修改群名为)|(此群为外部群)|(加入了外部群)".toRegex())) {
                        val interval =
                            System.currentTimeMillis() / 1000 - SPUtils.getInstance("noTipMessage")
                                .getLong(tvList[0], 0)
                        if (interval > 3600) {
                            LogUtils.i("发现无提示消息: $tvList")
                            log("发现无提示消息: $tvList")
                            if (AccessibilityUtil.performClick(item)) {
                                //进入聊天页 下一步 getChatMessageList
                            } else {
                                AccessibilityUtil.clickByNode(WeworkController.weworkService, item)
                            }
                            SPUtils.getInstance("noTipMessage")
                                .put(tvList[0], System.currentTimeMillis() / 1000)
                            return 1
                        } else {
                            LogUtils.v("发现无提示消息: $tvList 消息在 $interval 秒前已被查看")
                        }
                    }
                } else {
                    LogUtils.v("未发现无提示消息: ${tvList[1]}")
                    return -1
                }
            }
        }
        return 0
    }

    /**
     * 检查首页-聊天列表是否有不一致消息
     * @return -1当前列表不存在一周内消息 0未发现不一致消息 1发现不一致消息
     */
    private fun checkNoSyncMessage(list: AccessibilityNodeInfo): Int {
        list.refresh()
        val listBriefList = arrayListOf<List<CharSequence>>()
        val titleSet = hashSetOf<String>()
        for (i in 0 until list.childCount) {
            val item = list.getChild(i)
            val tvList = AccessibilityUtil.findAllOnceByClazz(item, Views.TextView)
                .mapNotNull { it.text?.toString() }.filter { !it.startsWith("＠") }
            listBriefList.add(tvList)
            //tvList title/time/content
            if (tvList.size == 3) {
                //只查看最近一周内的消息
                val title = tvList[0]
                if (title == "群聊" || !titleSet.add(title)) {
                    continue
                }
                if (tvList[1].isBlank() || tvList[1].contains("(刚刚)|(分钟前)|(上午)|(下午)|(昨天)|(星期)|(日程)|(会议)|(:)".toRegex())) {
                    val lastSyncMessage =
                        SPUtils.getInstance("lastSyncMessage").getString(title, null)
                            ?: continue
                    if (tvList[2].contains(lastSyncMessage.replace("\n", " ").trim(' ').trim())) {
                        continue
                    }
                    if (SPUtils.getInstance("noSyncMessage").getString(title) != lastSyncMessage) {
                        LogUtils.e("发现不一致消息: $tvList $lastSyncMessage")
                        error("发现不一致消息: $tvList $lastSyncMessage")
                        SPUtils.getInstance("noSyncMessage").put(title, lastSyncMessage)
                        if (AccessibilityUtil.performClick(item)) {
                            //进入聊天页 下一步 getChatMessageList
                        } else {
                            AccessibilityUtil.clickByNode(WeworkController.weworkService, item)
                        }
                        return 1
                    } else {
                        LogUtils.v("消息多次不一致: $tvList")
                    }
                } else {
                    LogUtils.v("未发现不一致消息: ${tvList[1]}")
                    return -1
                }
            }
        }
        return 0
    }

    /**
     * 解析消息列表里的一条消息
     */
    fun parseChatMessageItem(
        node: AccessibilityNodeInfo,
        titleList: ArrayList<String>,
        roomType: Int,
        imageCheck: Boolean = false,
        lastParse: Boolean = false
    ): WeworkMessageBean.SubMessageBean {
        val message: WeworkMessageBean.SubMessageBean
        val nameList = arrayListOf<String>()
        val itemMessageList = arrayListOf<WeworkMessageBean.ItemMessageBean>()
        LogUtils.v("开始解析一条消息...")
        //消息头(在消息主体上方 如时间信息)
        val linearLayoutItem =
            AccessibilityUtil.findOnceByClazz(node, Views.LinearLayout, limitDepth = 1)
        if (linearLayoutItem != null) {
            val sb = StringBuilder("消息头: ")
            val tvList = AccessibilityUtil.findAllOnceByClazz(linearLayoutItem, Views.TextView)
            for (item in tvList.filter { it.text != null && !it.text.isNullOrBlank() }) {
                val text = item.text.toString()
                val itemMessage = WeworkMessageBean.ItemMessageBean(0, text)
                sb.append(text).append("\t")
                itemMessageList.add(itemMessage)
            }
            LogUtils.v(sb.toString())
        }
        //消息主体
        val relativeLayoutItem =
            AccessibilityUtil.findOnceByClazz(node, Views.RelativeLayout, limitDepth = 1)
        if (relativeLayoutItem != null && relativeLayoutItem.childCount >= 2) {
            if (Views.ImageView.equals(relativeLayoutItem.getChild(0).className)) {
                LogUtils.v("头像在左边 本条消息发送者为其他联系人")
                nameList.addAll(WeworkTextUtil.getNameList(node))
                var textType = WeworkMessageBean.TEXT_TYPE_UNKNOWN
                val relativeLayoutContent =
                    AccessibilityUtil.findOnceByClazz(
                        relativeLayoutItem,
                        Views.RelativeLayout,
                        limitDepth = 2
                    )
                if (relativeLayoutContent != null) {
                    textType = WeworkTextUtil.getTextType(relativeLayoutContent)
                    LogUtils.v("textType: $textType")
                    val tvList =
                        AccessibilityUtil.findAllOnceByClazz(relativeLayoutContent, Views.TextView)
                    for (item in tvList.filter { it.text != null && !it.text.isNullOrBlank() }) {
                        val text = item.text.toString()
                        LogUtils.v(text)
                        if (text !in stopWords) {
                            val itemMessage = WeworkMessageBean.ItemMessageBean(2, text)
                            itemMessageList.add(itemMessage)
                        }
                    }
                }
                if (textType == WeworkMessageBean.TEXT_TYPE_LINK) {
                    val tempList = itemMessageList.filter { it.feature != 0 }
                    if (lastParse && tempList.size == 2 && tempList[0].text.contains("邀请你加入群聊")
                        && SPUtils.getInstance("groupInvite").getInt(tempList[1].text, 0) == 0
                    ) {
                        LogUtils.d("邀请你加入群聊: ${tempList[1].text}")
                        AccessibilityUtil.performClickWithSon(relativeLayoutContent)
                        if (AccessibilityExtraUtil.loadingPage("JsWebActivity")) {
                            val tvButton = AccessibilityUtil.findOneByText(
                                getRoot(),
                                "我知道了",
                                "加入群聊",
                                "你已接受过此邀请，无法再次加入",
                                "二维码已失效，无法加入群聊",
                                "你已接受邀请",
                                exact = true
                            )
                            val text = tvButton?.text?.toString()
                            if (text == "我知道了" || text?.startsWith("你已接受") == true || text?.startsWith(
                                    "二维码已失效"
                                ) == true
                            ) {
                                backPress()
                                SPUtils.getInstance("groupInvite").put(tempList[1].text, 1)
                                error("加入群聊失败: ${tempList[1].text}")
                            } else if (text == "加入群聊") {
                                AccessibilityUtil.performClick(tvButton)
                                SPUtils.getInstance("groupInvite").put(tempList[1].text, 1)
                                LogUtils.d("加入群聊: ${tempList[1].text}")
                                log("加入群聊: ${tempList[1].text}")
                                //该房间的信息读取任务会被强制中断并跳转到新房间且返回无效
                            } else {
                                LogUtils.e("加入群聊异常: ${tempList[1].text}")
                                error("加入群聊异常: ${tempList[1].text}")
                                backPress()
                            }
                        }
                    }
                }
                message = WeworkMessageBean.SubMessageBean(0, textType, itemMessageList, nameList)
                //图片类型特殊处理
                if (imageCheck && Constant.pushImage && textType == WeworkMessageBean.TEXT_TYPE_IMAGE) {
                    if (Constant.enableMediaProject) {
                        AccessibilityUtil.performClickWithSon(relativeLayoutContent)
                        AccessibilityExtraUtil.loadingPage(
                            "ShowImageController",
                            timeout = Constant.CHANGE_PAGE_INTERVAL
                        )
                        //图片双重校验
                        var image = 0
                        var image2 = 0
                        var byte: ByteArray? = null
                        do {
                            val bitmap = GetScreenShotActivity.startCapture()
                            byte = ImageUtils.bitmap2Bytes(bitmap)
                            LogUtils.v("bitmap byte: ${byte.size}")
                            image = byte.size
                            sleep(Constant.POP_WINDOW_INTERVAL / 2)
                            val bitmap2 = GetScreenShotActivity.startCapture()
                            val byte2 = ImageUtils.bitmap2Bytes(bitmap2)
                            LogUtils.v("bitmap2 byte: ${byte2.size}")
                            image2 = byte2.size
                            if (image != image2) {
                                LogUtils.e("图片双重校验失败")
                            }
                        } while (image != image2)
                        if (byte != null) {
                            if (titleList.isNotEmpty()) {
                                val lastImage = SPUtils.getInstance("lastImage")
                                    .getInt(titleList.joinToString())
                                if (lastImage == byte.size) {
                                    message.imageRepeat = true
                                } else {
                                    message.imageSize = byte.size
                                    message.image = byte
                                }
                            }
                        }
                        var retry = 3
                        while (retry-- > 0 && WeworkController.weworkService.currentClass == "com.tencent.wework.msg.controller.ShowImageController") {
                            AccessibilityUtil.performXYClick(
                                WeworkController.weworkService,
                                ScreenUtils.getScreenWidth() / 2F,
                                BarUtils.getStatusBarHeight() * 2F
                            )
                            sleep(Constant.POP_WINDOW_INTERVAL)
                        }
                    } else {
                        MultiFileObserver.saveSet.clear()
                        LogUtils.v("点击图片类型")
                        AccessibilityUtil.performClickWithSon(relativeLayoutContent)
                        AccessibilityExtraUtil.loadingPage(
                            "ShowImageController",
                            timeout = Constant.CHANGE_PAGE_INTERVAL
                        )
                        LogUtils.v("发现图片类型 查看图片检查有无新图片产生")
                        sleep(Constant.CHANGE_PAGE_INTERVAL)
                        if (MultiFileObserver.createSet.isNotEmpty()) {
                            LogUtils.d("正在下载图片...")
                            var downloading = true
                            val startTime = System.currentTimeMillis()
                            var currentTime = startTime
                            while (currentTime - startTime < Constant.LONG_INTERVAL) {
                                if (MultiFileObserver.createSet.size == MultiFileObserver.finishSet.size) {
                                    LogUtils.d("下载图片完成: ${MultiFileObserver.createSet.size}")
                                    downloading = false
                                    try {
                                        for (path in MultiFileObserver.finishSet) {
                                            val rawFileLength = File(path).length()
                                            if (rawFileLength < 20 * 1000) {
                                                LogUtils.d("原始文件大小<20k: $rawFileLength $path")
                                                log("原始文件大小<20k: $rawFileLength $path")
                                                continue
                                            }
                                            val df = SimpleDateFormat("MMdd_HHmmss")
                                            val targetPath = "${
                                                Utils.getApp().getExternalFilesDir("copy")
                                            }/${df.format(Date())}/${File(path).name}.png"
                                            if (FileUtils.copy(path, targetPath)) {
                                                LogUtils.d("复制图片完成: $rawFileLength $targetPath ")
                                                log("复制图片完成: $rawFileLength $targetPath")
                                                MultiFileObserver.saveSet.add(targetPath)
                                            } else {
                                                LogUtils.e("复制图片失败 请检查权限: $targetPath")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        LogUtils.e("接收图片出错", e)
                                        error("接收图片出错: ${e.message}")
                                    }
                                    break
                                }
                                sleep(Constant.POP_WINDOW_INTERVAL / 5)
                                currentTime = System.currentTimeMillis()
                            }
                            if (downloading) {
                                LogUtils.e("下载图片失败")
                            }
                        } else {
                            LogUtils.d("该图片已下载 忽略")
                        }
                        var retry = 3
                        while (retry-- > 0 && WeworkController.weworkService.currentClass == "com.tencent.wework.msg.controller.ShowImageController") {
                            AccessibilityUtil.performXYClick(
                                WeworkController.weworkService,
                                ScreenUtils.getScreenWidth() / 2F,
                                BarUtils.getStatusBarHeight() * 2F
                            )
                            sleep(Constant.POP_WINDOW_INTERVAL)
                        }
                    }
                }
            } else if (Views.ImageView.equals(relativeLayoutItem.getChild(1).className)) {
                LogUtils.v("头像在右边 本条消息发送者为自己")
                var textType = WeworkMessageBean.TEXT_TYPE_UNKNOWN
                val subLayout = relativeLayoutItem.getChild(0)
                if (subLayout.childCount > 0) {
                    textType = WeworkTextUtil.getTextType(subLayout)
                    LogUtils.v("textType: $textType")
                    val tvList = AccessibilityUtil.findAllOnceByClazz(
                        subLayout.getChild(subLayout.childCount - 1),
                        Views.TextView
                    )
                    for (item in tvList.filter { it.text != null && !it.text.isNullOrBlank() }) {
                        val text = item.text.toString()
                        LogUtils.v(text)
                        if (text !in stopWords) {
                            val itemMessage = WeworkMessageBean.ItemMessageBean(2, text)
                            itemMessageList.add(itemMessage)
                        }
                    }
                }
                if (textType == WeworkMessageBean.TEXT_TYPE_LINK && itemMessageList.size == 1
                    && itemMessageList[0].text.matches("[0-9]+%".toRegex())
                ) {
                    textType = WeworkMessageBean.TEXT_TYPE_IMAGE
                    itemMessageList.clear()
                }
                message = WeworkMessageBean.SubMessageBean(1, textType, itemMessageList, nameList)
            } else {
                // 没有头像的消息（撤销消息、其他可能的系统消息）
                val tvList = AccessibilityUtil.findAllOnceByClazz(node, Views.TextView)
                for (item in tvList.filter { it.text != null && !it.text.isNullOrBlank() }) {
                    val text = item.text.toString()
                    LogUtils.v(text)
                    val itemMessage = WeworkMessageBean.ItemMessageBean(1, text)
                    itemMessageList.add(itemMessage)
                }
                message = WeworkMessageBean.SubMessageBean(2, 0, itemMessageList, nameList)
                LogUtils.e("消息解析异常 未知异常")
            }
        } else {
            // 没有头像的消息（撤销消息、其他可能的系统消息）
            val sb = StringBuilder("未发现头像 本条消息发送者未知")
            val tvList = AccessibilityUtil.findAllOnceByClazz(node, Views.TextView)
            for (item in tvList.filter { it.text != null && !it.text.isNullOrBlank() }) {
                val text = item.text.toString()
                sb.append(text).append("/t")
                val itemMessage = WeworkMessageBean.ItemMessageBean(0, text)
                if (itemMessageList.count { it.feature == 0 && it.text == text } == 0) {
                    itemMessageList.add(itemMessage)
                }
            }
            LogUtils.v(sb.toString())
            message = WeworkMessageBean.SubMessageBean(2, 0, itemMessageList, nameList)
        }
        return message
    }

    /**
     * 有新消息时停止 or 不在消息页时停止 or 停止读循环时停止
     * @return true 停止 false 继续
     */
    private fun hasNewMessage(): Boolean {
        //如果有新消息则停止
        val list = AccessibilityUtil.findAllOnceByText(getRoot(), "消息", exact = true)
        var isSelect = false
        for (item in list) {
            val childCount = item.parent?.parent?.parent?.childCount
            if (childCount == 4 || childCount == 5) {
                if (item.parent != null && item.parent.childCount > 1) {
                    LogUtils.d("有新消息时停止")
                    return true
                }
                if (!item.isSelected) {
                    LogUtils.d("不在消息页时停止")
                    return true
                } else {
                    isSelect = true
                }
                if (!mainLoopRunning) {
                    LogUtils.d("停止读循环时停止")
                    return true
                }
            }
        }
        if (!isSelect) {
            LogUtils.d("不在消息页时停止")
            return true
        }
        return false
    }

}