package org.yameida.worktool.service

import com.blankj.utilcode.util.*
import org.yameida.worktool.Demo
import org.yameida.worktool.annotation.RequestMapping
import org.yameida.worktool.model.ExecCallbackBean
import org.yameida.worktool.model.WeworkMessageBean

/**
 * 企业微信客服端反转
 * 被服务端远程调用的服务Controller类
 */
object WeworkController {

    lateinit var weworkService: WeworkService
    var enableLoopRunning = false
    var mainLoopRunning = false

    /**
     * 交互通知
     * @see WeworkMessageBean.TYPE_CONSOLE_TOAST
     * @param message#errorCode 失败错误码
     * @param message#errorReason 失败原因
     */
    @RequestMapping
    fun consoleToast(message: ExecCallbackBean): Boolean {
        LogUtils.d("consoleToast(): ${message.errorCode} ${message.errorReason}")
        return WeworkInteractionImpl.consoleToast(message, message.errorCode, message.errorReason)
    }

    /**
     * 停止所有任务并返回首页待命
     * @see WeworkMessageBean.STOP_AND_GO_HOME
     */
    @RequestMapping
    fun stopAndGoHome() {
        LogUtils.d("stopAndGoHome()")
        enableLoopRunning = false
        mainLoopRunning = false
        goHome()
    }

    /**
     * 回到首页等待接收新消息
     * @see WeworkMessageBean.LOOP_RECEIVE_NEW_MESSAGE
     */
    @RequestMapping
    fun loopReceiveNewMessage() {
        LogUtils.d("loopReceiveNewMessage() enableLoopRunning: $enableLoopRunning")
        WeworkLoopImpl.mainLoop()
    }

    /**
     * 在房间内发送消息
     * @see WeworkMessageBean.SEND_MESSAGE
     * @param message#titleList 房间名称
     * @param message#receivedContent 回复内容
     * @param message#at 要at的昵称
     * @see WeworkMessageBean.TEXT_TYPE
     */
    @RequestMapping
    fun sendMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("sendMessage(): ${message.titleList} ${message.receivedContent} ${message.at} ${message.atList?.joinToString()}")
        return WeworkOperationImpl.sendMessage(message, message.titleList, message.receivedContent, message.at, message.atList)
    }

    /**
     * 在房间内指定回复消息
     * @see WeworkMessageBean.REPLY_MESSAGE
     * @param message#titleList 房间名称
     * @param message#receivedName 原始消息的发送者姓名
     * @param message#originalContent 原始消息的内容
     * @param message#textType 原始消息的消息类型
     * @param message#receivedContent 回复内容
     * @see WeworkMessageBean.TEXT_TYPE
     */
    @RequestMapping
    fun replyMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("replyMessage(): ${message.receivedName} ${message.originalContent} ${message.textType} ${message.receivedContent}")
        return WeworkOperationImpl.replyMessage(
            message,
            message.titleList,
            message.receivedName,
            message.originalContent,
            message.textType,
            message.receivedContent
        )
    }

    /**
     * 在房间内转发消息
     * @see WeworkMessageBean.RELAY_MESSAGE
     * @param message#titleList 房间名称
     * @param message#receivedName 原始消息的发送者姓名
     * @param message#originalContent 原始消息的内容
     * @param message#textType 原始消息的消息类型
     * @param message#nameList 待转发姓名列表
     * @param message#extraText 附加留言 选填
     * @see WeworkMessageBean.TEXT_TYPE
     */
    @RequestMapping
    fun relayMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("relayMessage(): ${message.titleList} ${message.receivedName} ${message.originalContent} ${message.textType} ${message.nameList} ${message.extraText}")
        return WeworkOperationImpl.relayMessage(
            message,
            message.titleList,
            message.receivedName,
            message.originalContent,
            message.textType,
            message.nameList,
            message.extraText
        )
    }

    /**
     * 初始化群设置
     * 群名称、群公告、拉人、踢人、群备注、群模板
     * @see WeworkMessageBean.INIT_GROUP
     * @param message#groupName 修改群名称
     * @param message#selectList 添加群成员名称列表 选填
     * @param message#groupAnnouncement 修改群公告 选填
     * @param message#groupRemark 修改群备注 选填
     * @param message#groupTemplate 修改群模板 选填
     */
    @RequestMapping
    fun initGroup(message: WeworkMessageBean): Boolean {
        LogUtils.d("initGroup(): ${message.groupName} ${message.selectList} ${message.groupAnnouncement} ${message.groupRemark} ${message.groupTemplate}")
        return WeworkOperationImpl.initGroup(
            message,
            message.groupName,
            message.selectList,
            message.groupAnnouncement,
            message.groupRemark,
            message.groupTemplate
        )
    }

    /**
     * 机器人接口测试
     * @see WeworkMessageBean.ROBOT_CONTROLLER_TEST
     */
    @RequestMapping
    fun test(message: WeworkMessageBean? = null) {
        LogUtils.d(message)
        Demo.test(true)
    }

    /**
     * 进入群聊并修改群配置
     * 群名称、群公告、拉人、踢人、群备注、群模板
     * @see WeworkMessageBean.UPDATE_GROUP
     * @param message#groupName 待修改的群
     * @param message#newGroupName 修改群名 选填
     * @param message#newGroupAnnouncement 修改群公告 选填
     * @param message#groupRemark 修改群备注 选填
     * @param message#groupTemplate 修改群模板 选填
     * @param message#selectList 添加群成员名称列表/拉人 选填
     * @param message#showMessageHistory 拉人是否附带历史记录 选填
     * @param message#removeList 移除群成员名称列表/踢人 选填
     */
    @RequestMapping
    fun updateGroup(message: WeworkMessageBean): Boolean {
        LogUtils.d("updateGroup(): ${message.groupName} ${message.newGroupName} ${message.newGroupAnnouncement} ${message.selectList} ${message.showMessageHistory} ${message.removeList} ${message.groupRemark} ${message.groupTemplate}")
        return WeworkOperationImpl.updateGroup(
            message,
            message.groupName,
            message.newGroupName,
            message.newGroupAnnouncement,
            message.groupRemark,
            message.groupTemplate,
            message.selectList,
            message.showMessageHistory,
            message.removeList
        )
    }

    /**
     * 解散群聊
     * @see WeworkMessageBean.DISMISS_GROUP
     * @param message#groupName 待解散的群
     */
    @RequestMapping
    fun dismissGroup(message: WeworkMessageBean): Boolean {
        LogUtils.d("dismissGroup(): ${message.groupName}")
        return WeworkOperationImpl.dismissGroup(
            message,
            message.groupName
        )
    }

    /**
     * 从外部群添加好友
     * @see WeworkMessageBean.ADD_FRIEND_BY_GROUP
     * @param message#groupName 外部群
     * @param message#friend 待添加用户
     */
    @RequestMapping
    fun addFriendByGroup(message: WeworkMessageBean): Boolean {
        LogUtils.d("addFriendByGroup(): ${message.groupName} ${message.friend}")
        return WeworkOperationImpl.addFriendByGroup(
            message,
            message.groupName,
            message.friend
        )
    }

    /**
     * 给群成员添加备注
     * @see WeworkMessageBean.MODIFY_GROUP_MEMBER_INFO
     * @param message#groupName 外部群
     * @param message#friend 待添加用户
     */
    @RequestMapping
    fun modifyGroupMemberInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("modifyGroupMemberInfo(): ${message.groupName} ${message.friend}")
        return WeworkOperationImpl.modifyGroupMemberInfo(
            message,
            message.groupName,
            message.friend
        )
    }

    /**
     * 添加待办
     * @see WeworkMessageBean.ADD_NEED_DEAL
     * @param message#titleList 内部用户昵称列表
     * @param message#receivedContent 回复内容
     */
    @RequestMapping
    fun addNeedDeal(message: WeworkMessageBean): Boolean {
        LogUtils.d("addNeedDeal(): ${message.titleList} ${message.receivedContent}")
        return WeworkOperationImpl.addNeedDeal(
            message,
            message.titleList,
            message.receivedContent
        )
    }

    /**
     * 打卡
     * @see WeworkMessageBean.CLOCK_IN
     */
    @RequestMapping
    fun clockIn(message: WeworkMessageBean): Boolean {
        LogUtils.d("clockIn(): ")
        return WeworkOperationImpl.clockIn(message)
    }

    /**
     * 切换企业
     * @see WeworkMessageBean.SWITCH_CORP
     * @param message#objectName 企业名称
     */
    @RequestMapping
    fun switchCorp(message: WeworkMessageBean): Boolean {
        LogUtils.d("switchCorp(): ${message.objectName}")
        return WeworkOperationImpl.switchCorp(message, message.objectName)
    }

    /**
     * 推送微盘图片
     * @see WeworkMessageBean.PUSH_MICRO_DISK_IMAGE
     * @param message#titleList 待发送姓名列表
     * @param message#objectName 图片名称
     * @param message#extraText 附加留言 可选
     */
    @RequestMapping
    fun pushMicroDiskImage(message: WeworkMessageBean): Boolean {
        LogUtils.d("pushMicroDiskImage(): ${message.titleList} ${message.objectName} ${message.extraText}")
        return WeworkOperationImpl.pushMicroDiskImage(
            message,
            message.titleList,
            message.objectName,
            message.extraText
        )
    }

    /**
     * 推送微盘文件
     * @see WeworkMessageBean.PUSH_MICRO_DISK_FILE
     * @param message#titleList 待发送姓名列表
     * @param message#objectName 文件名称
     * @param message#extraText 附加留言 可选
     */
    @RequestMapping
    fun pushMicroDiskFile(message: WeworkMessageBean): Boolean {
        LogUtils.d("pushMicroDiskFile(): ${message.titleList} ${message.objectName} ${message.extraText}")
        return WeworkOperationImpl.pushMicroDiskFile(
            message,
            message.titleList,
            message.objectName,
            message.extraText
        )
    }

    /**
     * 推送小程序
     * @see WeworkMessageBean.PUSH_MICROPROGRAM
     * @param message#titleList 待发送姓名列表
     * @param message#objectName 小程序名称
     * @param message#receivedContent 小程序描述
     * @param message#originalContent 小程序链接地址
     * @param message#fileUrl 图片地址
     * @param message#extraText 附加留言 可选
     */
    @RequestMapping
    fun pushMicroprogram(message: WeworkMessageBean): Boolean {
        LogUtils.d("pushMicroprogram(): ${message.titleList} ${message.objectName} ${message.receivedContent} ${message.originalContent} ${message.fileUrl} ${message.extraText}")
        return WeworkOperationImpl.pushMicroprogram(
            message,
            message.titleList,
            message.objectName,
            message.receivedContent,
            message.originalContent,
            message.fileUrl,
            message.extraText
        )
    }

    /**
     * 推送腾讯文档
     * @see WeworkMessageBean.PUSH_OFFICE
     * TODO 自己的文档分享时可选择权限级别
     * @param message#titleList 待发送姓名列表
     * @param message#objectName 腾讯文档名称
     * @param message#extraText 附加留言 可选
     */
    @RequestMapping
    fun pushOffice(message: WeworkMessageBean): Boolean {
        LogUtils.d("pushOffice(): ${message.titleList} ${message.objectName} ${message.extraText}")
        return WeworkOperationImpl.pushOffice(
            message,
            message.titleList,
            message.objectName,
            message.extraText
        )
    }

    /**
     * 推送文件(网络图片视频和文件等)
     * @see WeworkMessageBean.PUSH_FILE
     * @param message#titleList 待发送姓名列表
     * @param message#objectName 文件名称
     * @param message#fileUrl 文件网络地址
     * @param message#fileBase64 文件Base64
     * @param message#fileType 文件类型
     * @param message#extraText 附加留言 可选
     */
    @RequestMapping
    fun pushFile(message: WeworkMessageBean): Boolean {
        LogUtils.d("pushFile(): ${message.titleList} ${message.objectName} ${message.fileUrl} ${message.fileBase64?.substring(0, 100)} ${message.fileType} ${message.extraText}")
        return WeworkOperationImpl.pushFile(
            message,
            message.titleList,
            message.objectName,
            message.fileUrl,
            message.fileBase64,
            message.fileType,
            message.extraText,
            message.maxRetryCount
        )
    }

    /**
     * 推送链接
     * @see WeworkMessageBean.PUSH_LINK
     * @param message#titleList 待发送姓名列表
     * @param message#objectName 文章标题
     * @param message#receivedContent 文章副标题
     * @param message#originalContent 文章链接地址
     * @param message#fileUrl 图片地址
     * @param message#extraText 附加留言 可选
     */
    @RequestMapping
    fun pushLink(message: WeworkMessageBean): Boolean {
        LogUtils.d("pushLink(): ${message.titleList} ${message.objectName} ${message.receivedContent} ${message.originalContent} ${message.fileUrl} ${message.extraText}")
        return WeworkOperationImpl.pushLink(
            message,
            message.titleList,
            message.objectName,
            message.receivedContent,
            message.originalContent,
            message.fileUrl,
            message.extraText
        )
    }

    /**
     * 撤回消息
     * @see WeworkMessageBean.RECALL_MESSAGE
     * @param message#titleList 房间名称
     * @param message#originalContent 原始消息的内容
     * @param message#textType 原始消息的消息类型
     * @see WeworkMessageBean.TEXT_TYPE
     */
    @RequestMapping
    fun recallMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("recallMessage(): ${message.titleList} ${message.originalContent} ${message.textType}")
        return WeworkOperationImpl.recallMessage(
            message,
            message.titleList,
            message.originalContent,
            message.textType
        )
    }

    /**
     * 批量转发
     * @see WeworkMessageBean.RELAY_MULTI_MESSAGE
     * @param message#titleList 房间名称
     * @param message#messageList 消息列表
     * @param message#nameList 待转发姓名列表
     * @param message#extraText 附加留言 选填
     * @see WeworkMessageBean.TEXT_TYPE
     */
    @RequestMapping
    fun relayMultiMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("relayMultiMessage(): ${message.titleList} ${message.messageList} ${message.nameList} ${message.extraText}")
        return WeworkOperationImpl.relayMultiMessage(
            message,
            message.titleList,
            message.messageList,
            message.nameList,
            message.extraText
        )
    }

    /**
     * 合并转发
     * @see WeworkMessageBean.RELAY_MERGE_MESSAGE
     * @param message#titleList 房间名称
     * @param message#messageList 消息列表
     * @param message#nameList 待转发姓名列表
     * @param message#extraText 附加留言 选填
     * @see WeworkMessageBean.TEXT_TYPE
     */
    @RequestMapping
    fun relayMergeMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("relayMergeMessage(): ${message.titleList} ${message.messageList} ${message.nameList} ${message.extraText}")
        return WeworkOperationImpl.relayMergeMessage(
            message,
            message.titleList,
            message.messageList,
            message.nameList,
            message.extraText
        )
    }

    /**
     * 批量发送
     * @see WeworkMessageBean.SEND_MULTI_MESSAGE
     * @param message#weworkMessageList 消息列表
     * @param message#nameList 待转发姓名列表
     * @param message#extraText 附加留言 选填
     */
    @RequestMapping
    fun sendMultiMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("sendMultiMessage(): ${message.weworkMessageList} ${message.nameList} ${message.extraText}")
        return WeworkOperationImpl.sendMultiMessage(
            message,
            message.weworkMessageList,
            message.nameList,
            message.extraText
        )
    }

    /**
     * 合并发送
     * @see WeworkMessageBean.SEND_MERGE_MESSAGE
     * @param message#weworkMessageList 消息列表
     * @param message#nameList 待转发姓名列表
     * @param message#extraText 附加留言 选填
     */
    @RequestMapping
    fun sendMergeMessage(message: WeworkMessageBean): Boolean {
        LogUtils.d("sendMergeMessage(): ${message.weworkMessageList} ${message.nameList} ${message.extraText}")
        return WeworkOperationImpl.sendMergeMessage(
            message,
            message.weworkMessageList,
            message.nameList,
            message.extraText
        )
    }

    /**
     * 按手机号添加好友
     * @see WeworkMessageBean.ADD_FRIEND_BY_PHONE
     * @param message#friend 待添加用户
     */
    @RequestMapping
    fun addFriendByPhone(message: WeworkMessageBean): Boolean {
        LogUtils.d("addFriendByPhone(): ${message.friend}")
        return WeworkOperationImpl.addFriendByPhone(message, message.friend)
    }

    /**
     * 展示群信息
     * @see WeworkMessageBean.SHOW_GROUP_INFO
     * @param message#titleList 待查询群名
     * @param message#receivedName 原始消息的发送者姓名
     * @param message#originalContent 原始消息的内容
     * @param message#textType 原始消息的消息类型
     */
    @RequestMapping
    fun showGroupInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("showGroupInfo(): ${message.titleList} ${message.receivedName} ${message.originalContent} ${message.textType}")
        return WeworkOperationImpl.showGroupInfo(
            message,
            message.titleList,
            message.receivedName,
            message.originalContent,
            message.textType
        )
    }

    /**
     * 扫一扫
     * @see WeworkMessageBean.SCAN_QR_CODE
     * @param message#fileUrl 图片地址
     * @param message#fileBase64 文件Base64
     */
    @RequestMapping
    fun scanQrCode(message: WeworkMessageBean): Boolean {
        LogUtils.d("scanQrCode(): ${message.fileUrl} ${message.fileBase64?.substring(0, 100)}")
        return WeworkOperationImpl.scanQrCode(
            message,
            message.fileUrl,
            message.fileBase64
        )
    }

    /**
     * 删除联系人
     * @see WeworkMessageBean.DELETE_CONTACT
     * @param message#friend 待删除用户
     */
    @RequestMapping
    fun deleteContact(message: WeworkMessageBean): Boolean {
        LogUtils.d("deleteContact(): ${message.friend}")
        return WeworkOperationImpl.deleteContact(message, message.friend)
    }

    /**
     * 获取群信息
     * @see WeworkMessageBean.GET_GROUP_INFO
     * @param message#selectList 群名列表 为空时去群管理页查询并返回群聊页
     */
    @RequestMapping
    fun getGroupInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("getGroupInfo(): ${message.selectList}")
        return WeworkGetImpl.getGroupInfo(message, message.selectList)
    }

    /**
     * 获取好友信息
     * @see WeworkMessageBean.GET_FRIEND_INFO
     * @param message#selectList 好友名列表
     */
    @RequestMapping
    fun getFriendInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("getFriendInfo(): ${message.selectList}")
        return WeworkGetImpl.getFriendInfo(message, message.selectList)
    }

    /**
     * 获取全部好友信息
     * @see WeworkMessageBean.GET_ALL_FRIEND_INFO
     */
    @RequestMapping
    fun getAllFriendInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("getAllFriendInfo():")
        return WeworkGetImpl.getAllFriendInfo(message)
    }

    /**
     * 获取全部群信息
     * @see WeworkMessageBean.GET_ALL_GROUP_INFO
     */
    @RequestMapping
    fun getAllGroupInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("getAllGroupInfo():")
        return WeworkGetImpl.getAllGroupInfo(message)
    }

    /**
     * 获取本地文件
     * @see WeworkMessageBean.GET_LOCAL_FILE
     * @param message#fileUrl 文件地址
     */
    @RequestMapping
    fun getLocalFile(message: WeworkMessageBean): Boolean {
        LogUtils.d("getLocalFile(): ${message.fileUrl}")
        return WeworkGetImpl.getLocalFile(message, message.fileUrl)
    }

    /**
     * 获取我的信息
     * @see WeworkMessageBean.GET_MY_INFO
     */
    @RequestMapping
    fun getMyInfo(message: WeworkMessageBean): Boolean {
        LogUtils.d("getMyInfo():")
        return WeworkGetImpl.getMyInfo(message)
    }

    /**
     * 获取最近聊天列表
     * @see WeworkMessageBean.GET_RECENT_LIST
     */
    @RequestMapping
    fun getRecentList(message: WeworkMessageBean): Boolean {
        LogUtils.d("getRecentList():")
        return WeworkGetImpl.getRecentList(message)
    }

    /**
     * 获取企业列表
     * @see WeworkMessageBean.GET_CORP_LIST
     */
    @RequestMapping
    fun getCorpList(message: WeworkMessageBean): Boolean {
        LogUtils.d("getCorpList():")
        return WeworkGetImpl.getCorpList(message)
    }

}