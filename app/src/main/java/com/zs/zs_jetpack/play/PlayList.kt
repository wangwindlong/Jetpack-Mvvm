package com.zs.zs_jetpack.play

import android.util.Log
import com.zs.base_library.BaseApp
import com.zs.base_library.common.getRandom
import com.zs.base_library.common.isListEmpty
import com.zs.base_library.common.toast
import com.zs.zs_jetpack.db.AppDataBase
import com.zs.zs_jetpack.play.bean.AudioBean
import com.zs.zs_jetpack.ui.play.history.HistoryAudioBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * des 播放列表
 * 关于历史和收藏.. 当历史和收藏列表需要改变时,数据库和内存中列表(手动更新)同时更新,UI与内存列表保持一致
 * 这样做的意图是避免每次操作历史/收藏列表时频繁读取数据库做数据同步
 * @author zs
 * @data 2020/6/25
 */
class PlayList private constructor() {

    /**
     * 单例创建PlayerManager
     */
    companion object {
        val instance: PlayList by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            PlayList()
        }
    }

    /**
     * 当前播放列表
     */
    private var currentAudioList = mutableListOf<AudioBean>()

    /**
     * 默认播放列表,本地资源
     */
    private var localList = mutableListOf<AudioBean>()

    /**
     * 默认播放列表,收藏
     */
    private var collectList = mutableListOf<AudioBean>()

    /**
     * 默认播放列表,历史
     */
    private var historyList = mutableListOf<AudioBean>()

    /**
     * 播放模式，默认为顺序播放
     */
    private var playMode = PlayMode.ORDER_PLAY_MODE

    /**
     * 播放列表，默认为本地播放
     */
    private var playListType = PlayListType.LOCAL_PLAY_LIST


    init {
        //通过io线程读取播放列表
        GlobalScope.launch(Dispatchers.IO) {
            Log.i("PlayList", "${System.currentTimeMillis()}")
            //读取三个播放列表
            localList = readLocalPlayList(BaseApp.getContext())
            historyList = readHistoryPlayList()
            collectList = readCollectPlayList()
            Log.i("PlayList", "localList:${localList.size}")
            Log.i("PlayList", "historyList:${historyList.size}")
            Log.i("PlayList", "localList:${historyList.size}")
            Log.i("PlayList", "historyList$historyList")

            switchPlayList(playListType)
        }
    }

    /**
     * 切换播放列表
     */
    private fun switchPlayList(playListType: Int) {
        when (playListType) {
            //本地列表
            PlayListType.LOCAL_PLAY_LIST -> {
                replacePlayList(localList)
            }
            //收藏列表
            PlayListType.COLLECT_PLAY_LIST -> {
                replacePlayList(collectList)
            }
            //历史列表
            PlayListType.HISTORY_PLAY_LIST -> {
                replacePlayList(historyList)
            }
        }
    }

    /**
     * 替换播放列表
     */
    private fun replacePlayList(list: MutableList<AudioBean>) {
        currentAudioList.apply {
            clear()
            addAll(list)
        }
    }

    /**
     * 当前正在播放的音频 默认为null
     */
    private var currentAudio: AudioBean? = null

    /**
     * 当前播放音频对象在对应播放列表的角标
     */
    private var currentIndex = 0

    /**
     * 当前正在播放的音频
     */
    fun currentAudio(): AudioBean? {
        return currentAudio
    }

    /**
     * 设置当前播放列表和currentIndex
     */
    fun setCurrentAudio(audioBean: AudioBean) {
        //模拟加入历史
        GlobalScope.launch(Dispatchers.IO) {
            AppDataBase.getInstance()
                .historyDao()
                .insertAudio(HistoryAudioBean.audio2History(audioBean))
        }
        //播放新音频时更新历史列表
        addRecord(audioBean)
        //每次切换都做播放列表更新
        switchPlayList(audioBean.playListType)
        //重置当前角标
        currentIndex = getIndexByAudio(audioBean)
    }

    /**
     * 增加历史记录
     */
    private fun addRecord(audioBean: AudioBean){
        //先将原纪录移除
        historyList.forEach {
            if (it.id == audioBean.id){
                historyList.remove(it)
                return@forEach
            }
        }
        //将新纪录加入到末尾
        historyList.add(audioBean)
    }

    /**
     * 第一次进入播放的音频，默认为播放列表的第一个
     */
    fun startAudio(): AudioBean? {
        if (!isListEmpty(currentAudioList)) {
            currentAudio = currentAudioList[0]
        }
        return currentAudio
    }

    /**
     * 下一个音频
     */
    fun nextAudio(): AudioBean? {
        if (!isListEmpty(currentAudioList)) {
            when (playMode) {
                //顺序
                PlayMode.ORDER_PLAY_MODE -> {
                    currentIndex = if (currentIndex < currentAudioList.size - 1) {
                        currentIndex + 1
                    } else {
                        0
                    }
                }
                //单曲(不做处理)
                PlayMode.SINGLE_PLAY_MODE -> {
                }
                //随机
                PlayMode.RANDOM_PLAY_MODE -> {
                    currentIndex = getRandom(0, currentAudioList.size - 1)
                }
            }
        }
        currentAudio = currentAudioList[currentIndex]
        return currentAudio
    }

    /**
     * 上一个音频
     */
    fun previousAudio(): AudioBean? {
        if (!isListEmpty(currentAudioList)) {
            when (playMode) {
                //顺序
                PlayMode.ORDER_PLAY_MODE -> {
                    currentIndex = if (currentIndex > 0) {
                        currentIndex - 1
                    } else {
                        currentAudioList.size - 1
                    }
                }
                //单曲(不做处理)
                PlayMode.SINGLE_PLAY_MODE -> {
                }
                //随机
                PlayMode.RANDOM_PLAY_MODE -> {
                    currentIndex = getRandom(0, currentAudioList.size - 1)
                }
            }
        }
        currentAudio = currentAudioList[currentIndex]
        return currentAudio
    }

    /**
     * 切换播放模式
     * 顺序播放点击 会切换至单曲
     * 单曲播放点击 会切换至随机
     * 随机播放点击 会切换至顺序
     */
    fun switchPlayMode(): Int {
        when (playMode) {
            PlayMode.ORDER_PLAY_MODE -> {
                toast("单曲循环")
                playMode = PlayMode.SINGLE_PLAY_MODE
            }
            PlayMode.SINGLE_PLAY_MODE -> {
                toast("随机播放")
                playMode = PlayMode.RANDOM_PLAY_MODE
            }
            PlayMode.RANDOM_PLAY_MODE -> {
                toast("列表循环")
                playMode = PlayMode.ORDER_PLAY_MODE
            }
        }
        return playMode
    }

    /**
     * 获取当前播放模式
     */
    fun getCurrentMode(): Int {
        return playMode
    }

    /**
     * 重置，将当前播放重置为null
     */
    fun clear() {
        currentAudio = null
    }

    /**
     * 通过currentAudio获取所在的index
     * 之所以没有全局开放一个index,是为了尽可能的降低 index 的操作权限
     */
    private fun getIndexByAudio(audioBean: AudioBean): Int {
        //设置当前正在播放的对象
        currentAudio = audioBean
        for (index in 0 until currentAudioList.size) {
            if (audioBean == currentAudioList[index]) {
                return index
            }
        }
        //默认返回0
        return 0
    }

    /**
     * 获取播放列表长度
     */
    fun getPlayListSize(): Int {
        return currentAudioList.size
    }

    /**
     * 获取播放列表
     */
    fun getPlayList(): MutableList<AudioBean> {
        return currentAudioList
    }

    /**
     * 更新历史列表
     */
    fun updateHistoryList(list: MutableList<AudioBean>) {
        historyList.clear()
        historyList.addAll(list)
    }

    /**
     * 更新收藏列表
     */
    fun updateCollectList(list: MutableList<AudioBean>) {
        collectList.clear()
        collectList.addAll(list)
    }

    class PlayMode {
        companion object {

            /**
             * 顺序播放-默认
             */
            const val ORDER_PLAY_MODE = 9

            /**
             * 单曲播放
             */
            const val SINGLE_PLAY_MODE = 99

            /**
             * 随机播放
             */
            const val RANDOM_PLAY_MODE = 999
        }
    }
}