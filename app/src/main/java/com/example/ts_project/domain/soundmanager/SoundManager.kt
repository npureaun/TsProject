package com.example.ts_project.domain.soundmanager

import android.media.MediaPlayer
import android.media.SoundPool
import android.view.View
import com.example.ts_project.MainActivity
import com.example.ts_project.R
import com.example.ts_project.gamedata.GameStatus

class SoundManager(
    private val uiCallbacks: MainActivity
) {
    private var selectData: Int = 0
    private val totalCount = 11
    private var lastPlayTime = 0L  // 마지막 재생 시작 시점(ms)
    private var soundPoolTungSahur: SoundPool? = null
    private var soundPoolAttack: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    var mediaPlayer: MediaPlayer? = null
    private val minDurationMs = 5000L
    private val playingStreams = mutableListOf<Int>()

    init {
        // API 21 이상부터 SoundPool.Builder 사용 권장
        soundPoolTungSahur = SoundPool.Builder()
            .setMaxStreams(1)  // 동시에 재생할 수 있는 최대 사운드 수
            .build()

        soundPoolAttack = SoundPool.Builder()
            .setMaxStreams(10)
            .build()
        // 사운드 리소스 로드 (동기적 로드 아님, 로드 완료 후 재생 가능)
        soundMap["tung_sahur"] = soundPoolTungSahur!!.load(uiCallbacks, R.raw.tung_sahur, 1)
        soundMap["user_attack"] = soundPoolAttack!!.load(uiCallbacks, R.raw.user_attack, 1)
        soundMap["com_attack"] = soundPoolAttack!!.load(uiCallbacks, R.raw.com_attack, 1)
        soundMap["user_critical"] = soundPoolAttack!!.load(uiCallbacks, R.raw.user_critical, 1)
        soundMap["com_critical"] = soundPoolAttack!!.load(uiCallbacks, R.raw.com_critical, 1)
    }

    fun refreshSound() {
        lastPlayTime = 1000
    }

    fun lockButtonAndPlayAudio(isNewGame: Boolean = false) {
        if (GameStatus.isGamePaused) return

        GameStatus.isLock = true
        uiCallbacks.button.isEnabled = false
        uiCallbacks.countDownAnimator?.cancel()

        if (isNewGame) {
            selectData()
        }
        // 랜덤 이미지와 오디오 설정
        val imageResId = uiCallbacks.resources.getIdentifier("com_$selectData", "drawable", uiCallbacks.packageName)
        val audioResId = uiCallbacks.resources.getIdentifier("com_$selectData", "raw", uiCallbacks.packageName)

        uiCallbacks.imageView.setImageResource(imageResId)

        uiCallbacks.buttonCover.translationX = 0f
        uiCallbacks.buttonCover.visibility = View.VISIBLE

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(uiCallbacks, audioResId)
        mediaPlayer?.setOnCompletionListener {
            GameStatus.isLock = false
            mediaPlayer?.release()
            mediaPlayer = null
            refreshSound()
            uiCallbacks.gameManager.startButtonEnablePeriod()
        }
        stopAllSounds()
        mediaPlayer?.start()
    }

    fun selectData() {
        selectData = (0 until totalCount).random()
    }

    fun stopAllSounds() {
        soundPoolTungSahur?.autoPause()
        soundPoolAttack?.autoPause()

        // playingStreams.forEach { streamId ->
        //     soundPoolTungSahur.stop(streamId)
        //     soundPoolAttack.stop(streamId)
        // }
        playingStreams.clear()
    }

    fun deleteAllSounds() {
        soundPoolTungSahur?.release()
        soundPoolAttack?.release()

        soundPoolTungSahur = null
        soundPoolAttack = null
    }

    fun playSoundTungSahur() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPlayTime < minDurationMs) {
            // 아직 최소 재생 간격이 지나지 않음 -> 재생 무시
            return
        }
        soundMap["tung_sahur"]?.let { soundId ->
            soundPoolTungSahur?.play(soundId, 1f, 1f, 1, 0, 1f)
                .also { playingStreams.add(it!!) }
            lastPlayTime = currentTime
        }
    }

    fun playSoundAttack(isUser: Boolean) {
        soundMap[if(isUser)"user_attack" else "com_attack"]?.let { soundId ->
            soundPoolAttack?.play(soundId, 1f, 1f, 1, 0, 1f)
                .also { playingStreams.add(it!!) }
        }
    }

    fun playSoundCritical(isUser: Boolean) {
        if(isUser){
            soundMap["user_critical"]?.let { soundId ->
                soundPoolAttack?.play(soundId, 0.8f, 0.8f, 1, 0, 1f)
                    .also { playingStreams.add(it!!) }
            }
        }
        else{
            soundMap["com_critical"]?.let { soundId ->
                soundPoolAttack?.play(soundId, 0.3f, 0.3f, 1, 0, 1f)
                    .also { playingStreams.add(it!!) }
            }
        }
    }
}

// class SoundManager(
//     private val uiCallbacks: MainActivity
// ) {
//     private var selectData:Int=0
//     private val totalCount=11
//     private var lastPlayTime = 0L  // 마지막 재생 시작 시점(ms)
//     var mediaPlayer: MediaPlayer? = null
//     var mediaPlayerAttack: MediaPlayer? = null
//
//     fun lockButtonAndPlayAudio(isNewGame: Boolean=false) {
//         if(GameStatus.isGamePaused)return
//
//         uiCallbacks.button.isEnabled = false
//         uiCallbacks.countDownAnimator?.cancel()
//
//         if(isNewGame) {
//             selectData()
//         }
//         // 랜덤 이미지와 오디오 설정
//         val imageResId = uiCallbacks.resources.getIdentifier("com_$selectData", "drawable", uiCallbacks.packageName)
//         val audioResId = uiCallbacks.resources.getIdentifier("com_$selectData", "raw", uiCallbacks.packageName)
//
//         uiCallbacks.imageView.setImageResource(imageResId)
//
//         uiCallbacks.buttonCover.translationX = 0f
//         uiCallbacks.buttonCover.visibility = View.VISIBLE
//
//         mediaPlayer?.release()
//         mediaPlayer = MediaPlayer.create(uiCallbacks, audioResId)
//         mediaPlayer?.setOnCompletionListener {
//             mediaPlayer?.release()
//             mediaPlayer = null
//             uiCallbacks.gameManager.startButtonEnablePeriod()
//         }
//         mediaPlayer?.start()
//     }
//
//     fun selectData(){
//         selectData = (0 until totalCount).random()
//     }
//
//     fun playSoundWithMinDuration(minDurationMs: Long) {
//         //uiCallbacks.gameManager.userAction()
//         val currentTime = System.currentTimeMillis()
//
//         // 마지막 재생 시작 후 경과 시간 계산
//         val elapsed = currentTime - lastPlayTime
//
//         if (elapsed >= minDurationMs || mediaPlayer == null || mediaPlayer?.isPlaying == false) {
//             // 최소 재생 시간 지났거나, 재생 중이 아니면 재생 시작
//             playSound()
//             lastPlayTime = currentTime
//         } else {
//             // 0.3초 안 됐으면 무시하거나 대기 - 여기선 무시
//             // 필요하면 핸들러 등으로 지연 재생 구현 가능
//         }
//     }
//
//     fun playSoundWithAttack() {
//         uiCallbacks.gameManager.userAction()
//         val currentTime = System.currentTimeMillis()
//         val minDurationMs=10
//         // 마지막 재생 시작 후 경과 시간 계산
//         val elapsed = currentTime - lastPlayTime
//
//         if (elapsed >= minDurationMs || mediaPlayer == null || mediaPlayer?.isPlaying == false) {
//             // 최소 재생 시간 지났거나, 재생 중이 아니면 재생 시작
//             playSoundAttack()
//             lastPlayTime = currentTime
//         } else {
//             // 0.3초 안 됐으면 무시하거나 대기 - 여기선 무시
//             // 필요하면 핸들러 등으로 지연 재생 구현 가능
//         }
//     }
//
//     private fun playSoundAttack() {
//         if (mediaPlayerAttack == null) {
//             mediaPlayerAttack = MediaPlayer.create(uiCallbacks, R.raw.attack)
//             mediaPlayerAttack?.setOnCompletionListener {
//                 mediaPlayerAttack?.release()
//                 mediaPlayerAttack = null
//             }
//         } else {
//             mediaPlayerAttack?.apply {
//                 if (isPlaying) {
//                     stop()
//                     reset()
//                     setDataSource(uiCallbacks, Uri.parse("android.resource://${uiCallbacks.packageName}/${R.raw.attack}"))
//                     prepare()
//                 }
//             }
//         }
//         mediaPlayerAttack?.start()
//     }
//
//     private fun playSound() {
//         if (mediaPlayer == null) {
//             mediaPlayer = MediaPlayer.create(uiCallbacks, R.raw.tung_sahur)
//             mediaPlayer?.setOnCompletionListener {
//                 mediaPlayer?.release()
//                 mediaPlayer = null
//             }
//         } else {
//             mediaPlayer?.apply {
//                 if (isPlaying) {
//                     stop()
//                     reset()
//                     setDataSource(uiCallbacks, Uri.parse("android.resource://${uiCallbacks.packageName}/${R.raw.tung_sahur}"))
//                     prepare()
//                 }
//             }
//         }
//         mediaPlayer?.start()
//     }
// }