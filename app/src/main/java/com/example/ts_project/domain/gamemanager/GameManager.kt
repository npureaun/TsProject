package com.example.ts_project.domain.gamemanager

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.ts_project.MainActivity
import com.example.ts_project.R
import com.example.ts_project.gamedata.status.ComData
import com.example.ts_project.gamedata.DamageType
import com.example.ts_project.gamedata.GameStatus
import com.example.ts_project.gamedata.status.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameManager(
    private val uiCallbacks: MainActivity
) {
    var userData= UserData()
    var comData= ComData()
    var delayTime=4000L
    // 게임 상태
    fun pauseGame() {
        GameStatus.isGamePaused = true
        uiCallbacks.buttonEnableJob?.cancel()
        uiCallbacks.countDownAnimator?.cancel()
        uiCallbacks.button.isEnabled = false
    }

    fun sleepGame() {
        GameStatus.isGamePaused = true
        uiCallbacks.buttonEnableJob?.cancel()
        uiCallbacks.countDownAnimator?.cancel()
        uiCallbacks.soundManager.mediaPlayer?.pause()
        uiCallbacks.soundManager.stopAllSounds()
        uiCallbacks.button.isEnabled = false
    }

    fun resumeGame() {
        if (!GameStatus.isGamePaused) return
        GameStatus.isGamePaused = false
        uiCallbacks.soundManager.mediaPlayer?.start()
        uiCallbacks.updateComHP()
        uiCallbacks.soundManager.lockButtonAndPlayAudio(true)
    }

    fun onStart(){
        if (!GameStatus.isGamePaused) return
        if(GameStatus.isDialog)return
        GameStatus.isGamePaused = false
        uiCallbacks.soundManager.mediaPlayer?.start()
        if(GameStatus.isLock) uiCallbacks.soundManager.lockButtonAndPlayAudio()
        else{
            comData.comHP=comData.backupHP
            uiCallbacks.updateComHP()
            uiCallbacks.soundManager.refreshSound()
            startButtonEnablePeriod()
        }
    }

    fun increaseHp() {
        // val hpBoost = (userData.playerHP * (10) / 100.0).toLong()
        // userData.playerHP+=hpBoost
        // userData.originHP=userData.playerHP

        userData.playerHP=((userData.lvCnt*1000)/3)+1000
        userData.originHP=userData.playerHP
    }

    fun increaseAtk() {
        // ATK 증가 로직
        val atkBoost = (userData.playerATK * (15+userData.lvCnt) / 100.0).toLong()
        userData.playerATK+= atkBoost
    }

    fun increaseCritical() {
        // CRITICAL 증가 로직
        userData.criticalRate+=5
    }


    fun userAction() {
        val isCritical = (1..100).random() <= userData.criticalRate
        val baseDamage = userData.playerATK
        val randomFactor = (90..110).random() / 100.0
        var damage = (baseDamage * randomFactor).toLong()

        if (isCritical) {
            damage *= 2
            uiCallbacks.soundManager.playSoundCritical(true)
            uiCallbacks.showDamageTextAnimation("$damage",DamageType.USER_CRITICAL)
        } else {
            uiCallbacks.soundManager.playSoundAttack(true)
            uiCallbacks.showDamageTextAnimation(damage.toString(),DamageType.USER)
        }

        comData.comHP -= damage

        if (comData.comHP <= 0) {
            uiCallbacks.updateComHP()
            newGame()
            startButtonEnablePeriod(true)
            return
        }

        uiCallbacks.updateComHP()
    }


    private fun newGame() {
        pauseGame()
        uiCallbacks.showUpgradeSelectionDialog()
        val level = ++userData.lvCnt

        // 컴퓨터 HP 기본값 (0.15 지수 기울기 적용)
        val expFactor = 0.01 + (level / 1000.0)  // 레벨이 올라갈수록 살짝 증가
        val baseComHP = (comData.originHP * (80..100).random() / 100.0) * kotlin.math.exp(level * expFactor)


        var atkBoost = (comData.comATK * (3..5).random() / 100.0) * kotlin.math.exp(level * 0.02)

        // 컴퓨터 레벨 보정치: 누적 HP 증가
        val comLevelBonus = (baseComHP * ((7..10).random()) / 100.0).toLong()
        comData.comHP = baseComHP.toLong() + comLevelBonus
        comData.comATK += atkBoost.toLong() + 10

        if (level % 5 == 0L && comData.criticalRate < 100) {
            comData.criticalRate += 5
        }

        // 유저 HP / ATK 증가량 (0.15 지수 기울기 적용)
        val hpBoost = (userData.originHP * (10) / 100.0).toLong()
        atkBoost = (userData.playerATK * 10 / 100.0) * kotlin.math.exp(level * 0.02)

        if (level % 5 == 0L && userData.criticalRate < 100) {
            userData.criticalRate += 5
        }
        if(level>=30&&(level % 3 == 0L||level%10==0L)){
            atkBoost*=level
        }

        userData.playerHP += hpBoost.toLong()
        userData.playerATK += atkBoost.toLong()

        uiCallbacks.updateStatus()
        userData.originHP = userData.playerHP
        comData.originHP = comData.comHP
    }


    private suspend fun comAction() {
        if (GameStatus.isGamePaused) return

        comData.backupHP=comData.comHP
        val randomFactor = (80..120).random() / 100.0
        val baseDamage = (comData.comATK * randomFactor).toLong()

        val isCritical = (1..100).random() <= comData.criticalRate
        var damage = baseDamage

        delay(500)
        if (isCritical) {
            damage *= 2
            uiCallbacks.soundManager.playSoundCritical(false)
            uiCallbacks.showDamageTextAnimation("$damage", DamageType.COM_CRITICAL)
        } else {
            uiCallbacks.soundManager.playSoundAttack(false)
            uiCallbacks.showDamageTextAnimation("$damage", DamageType.COM)
        }

        userData.playerHP -= damage
        uiCallbacks.updateStatus()

        if (userData.playerHP <= 0) {
            pauseGame()
            uiCallbacks.reGameSelectionDialog()
            playDataFormat()
            uiCallbacks.showDamageTextAnimation("RE GAME", DamageType.COM_CRITICAL)
        }
    }


    fun playDataFormat(){
        userData= UserData()
        comData= ComData()
        uiCallbacks.updateStatus()
        uiCallbacks.updateComHP()
    }

    fun startButtonEnablePeriod(isNewGame:Boolean=false) {
        uiCallbacks.buttonEnableJob?.cancel()
        uiCallbacks.button.isEnabled = true
        if(GameStatus.isGamePaused) {
            return
        }
        uiCallbacks.buttonCover.translationX = uiCallbacks.button.width.toFloat()
        uiCallbacks.buttonCover.visibility = View.VISIBLE
        uiCallbacks.imageView.setImageResource(R.drawable.tung_sahur)


        uiCallbacks.buttonEnableJob=uiCallbacks.lifecycleScope.launch {
            if (!isNewGame) {
                uiCallbacks.animateButtonCoverFor5Seconds()
                delay(delayTime)
                uiCallbacks.soundManager.lockButtonAndPlayAudio(false)
                comAction()
            } else {
                delay(1L)
                uiCallbacks.soundManager.lockButtonAndPlayAudio(true)
            }
        }
    }
}
