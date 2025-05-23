package com.example.ts_project

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.ts_project.domain.gamemanager.GameManager
import com.example.ts_project.domain.soundmanager.SoundManager
import com.example.ts_project.gamedata.DamageType
import com.example.ts_project.gamedata.GameStatus
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {
    lateinit var button: Button
    var countDownAnimator: ValueAnimator? = null
    lateinit var buttonCover: View
    lateinit var imageView : ImageView

    private lateinit var statusTextView: TextView
    private lateinit var hpTextView :TextView
    private lateinit var damageText:TextView
    var buttonEnableJob: Job? = null


    lateinit var gameManager:GameManager
    lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameManager=GameManager(this)
        soundManager=SoundManager(this)
        startSetUp()

        soundManager.lockButtonAndPlayAudio()
    }

    private fun startSetUp(){
        setUpid()
        //startButtonEnablePeriod()
        updateStatus()
        updateComHP()
        gameManager.userData.originHP=gameManager.userData.playerHP
        soundManager.selectData()
        button.setOnClickListener {
            if(!GameStatus.isGamePaused) {
                soundManager.playSoundTungSahur()
                gameManager.userAction()
            }
        }
    }

    private fun setUpid(){
        button = findViewById(R.id.button)
        buttonCover = findViewById(R.id.buttonCover)
        imageView=findViewById(R.id.imageView)
        statusTextView = findViewById(R.id.statusTextView)
        hpTextView= findViewById(R.id.hpTextView)
        damageText = findViewById(R.id.damageText)
    }

    fun updateStatus() {
        var critical:String
        critical=if(gameManager.userData.criticalRate>=100)"MAX" else gameManager.userData.criticalRate.toString()

        statusTextView.text = "Lv: ${gameManager.userData.lvCnt}\n" +
            "HP: ${gameManager.userData.playerHP}\n" +
            "ATK: ${gameManager.userData.playerATK}\n"+
            "CRITICAL: "+ critical

        statusTextView.setTextColor(Color.CYAN)
    }

    fun updateComHP(){
        hpTextView.text="HP: ${gameManager.comData.comHP}"
    }

    // 5초 카운트다운 애니메이터 (오른쪽->왼쪽 색 변화)
    fun animateButtonCoverFor5Seconds() {
        button.post {
            val buttonWidth = buttonCover.width.toFloat()
            countDownAnimator?.cancel()

            countDownAnimator = ValueAnimator.ofFloat(-buttonWidth,0f).apply {
                duration = gameManager.delayTime*2 // 5초
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    buttonCover.translationX = value
                }
                start()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        gameManager.onStart()
    }


    override fun onStop() {
        super.onStop()
        gameManager.sleepGame()
    }


    fun showDamageTextAnimation(text: String, damageType:DamageType) {
        val damageTextView = TextView(this).apply {
            textSize = 50f
            this.text = text
            if(damageType==DamageType.USER_CRITICAL){
                setTextColor(Color.MAGENTA)
                textSize=60f
            }else if(damageType==DamageType.USER){
                setTextColor(Color.CYAN)
            }else if(damageType==DamageType.COM_CRITICAL){
                setTextColor(Color.YELLOW)
                textSize=60f
            }else setTextColor(Color.RED)
            typeface = Typeface.DEFAULT_BOLD
            alpha = 1f
            translationY = 0f
            visibility = View.VISIBLE
            id = View.generateViewId() // 제약 조건에 ID 필요

            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                verticalBias = 0.26f
            }
        }

        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        rootLayout.addView(damageTextView)

        damageTextView.animate()
            .translationYBy(-100f)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                rootLayout.removeView(damageTextView)
            }
            .start()
    }

    fun showUpgradeSelectionDialog() {
        GameStatus.isDialog=true
        val upgrades = arrayOf("HP HEAL", "ATK +(15+LV)%", "CRITICAL +5%")

        AlertDialog.Builder(this)
            .setTitle("강화 요소를 선택하세요")
            .setItems(upgrades) { dialog, which ->
                when (which) {
                    0 -> gameManager.increaseHp()
                    1 -> gameManager.increaseAtk()
                    2 -> gameManager.increaseCritical()
                }
                dialog.dismiss()
                updateStatus()
                GameStatus.isDialog=false
                gameManager.resumeGame()
            }
            .setCancelable(false)
            .show()
    }

    fun reGameSelectionDialog() {
        GameStatus.isDialog=true
        val upgrades = arrayOf("yes", "no")

        AlertDialog.Builder(this)
            .setTitle("Re Game?")
            .setItems(upgrades) { dialog, which ->
                when (which) {
                    0 -> gameManager.playDataFormat()
                    1 -> finish()
                }
                dialog.dismiss()
                GameStatus.isDialog=false
                gameManager.resumeGame()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.mediaPlayer?.release()
        soundManager.mediaPlayer = null
        countDownAnimator?.cancel()
        soundManager.stopAllSounds()
        soundManager.deleteAllSounds()
    }
}
//
// abstract class MainActivity : AppCompatActivity(),UICallbacks {
//     private lateinit var button: Button
//     private var mediaPlayer: MediaPlayer? = null
//     private var countDownAnimator: ValueAnimator? = null
//     private var lastPlayTime = 0L  // 마지막 재생 시작 시점(ms)
//     private lateinit var buttonCover: View
//     private lateinit var imageView : ImageView
//     private val delayTime=5000L
//     private val minDurationMs=400L
//     private lateinit var statusTextView: TextView
//     private lateinit var hpTextView :TextView
//     private lateinit var damageText:TextView
//     private var buttonEnableJob: Job? = null
//
//     private var comData=ComData()
//
//     private var userData=UserData()
//
//     private var selectData:Int=0
//
//     private val totalCount=10
//
//     private var isGamePaused = false
//
//     private lateinit var gameManager:GameManager
//
//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)
//         setContentView(R.layout.activity_main)
//
//         gameManager=GameManager(this)
//         startSetUp()
//
//         lockButtonAndPlayAudio()
//     }
//
//     private fun startSetUp(){
//         setUpid()
//         //startButtonEnablePeriod()
//         updateStatus()
//         updateComHP()
//         userData.originHP=userData.playerHP
//         selectData=(0 until totalCount).random()
//         button.setOnClickListener {
//             if(! isGamePaused) playSoundWithMinDuration(minDurationMs)  // 300ms 최소 재생시간 설정
//         }
//     }
//
//     private fun setUpid(){
//         button = findViewById(R.id.button)
//         buttonCover = findViewById(R.id.buttonCover)
//         imageView=findViewById(R.id.imageView)
//         statusTextView = findViewById(R.id.statusTextView)
//         hpTextView= findViewById(R.id.hpTextView)
//         damageText = findViewById(R.id.damageText)
//     }
//
//     private fun updateStatus() {
//         statusTextView.text = "Lv: ${userData.lvCnt}\n" +
//             "HP: ${userData.playerHP}\n" +
//             "ATK: ${userData.playerATK}"
//
//         statusTextView.setTextColor(Color.CYAN)
//     }
//
//     private fun updateComHP(){
//         hpTextView.text="HP: ${comData.comHP}"
//     }
//
//     private fun startButtonEnablePeriod(isNewGame:Boolean=false) {
//         buttonEnableJob?.cancel()
//         button.isEnabled = true
//         if(isGamePaused) {
//             return
//         }
//         buttonCover.translationX = button.width.toFloat()
//         buttonCover.visibility = View.VISIBLE
//         imageView.setImageResource(R.drawable.tung_sahur)
//
//
//         buttonEnableJob=lifecycleScope.launch {
//             if (!isNewGame) {
//                 animateButtonCoverFor5Seconds()
//                 delay(delayTime)
//                 lockButtonAndPlayAudio(false)
//                 comAction()
//             } else {
//                 delay(1L)
//                 lockButtonAndPlayAudio(true)
//             }
//         }
//     }
//
//     // 5초 카운트다운 애니메이터 (오른쪽->왼쪽 색 변화)
//     private fun animateButtonCoverFor5Seconds() {
//         button.post {
//             val buttonWidth = button.width.toFloat()
//             countDownAnimator?.cancel()
//
//             countDownAnimator = ValueAnimator.ofFloat(-buttonWidth,0f).apply {
//                 duration = delayTime*2 // 5초
//                 addUpdateListener { animation ->
//                     val value = animation.animatedValue as Float
//                     buttonCover.translationX = value
//                 }
//                 start()
//             }
//         }
//     }
//
//     // 버튼 잠금 & 음성 재생 시작
//     private fun lockButtonAndPlayAudio(isNewGame: Boolean=false) {
//         if(isGamePaused) return
//
//         button.isEnabled = false
//         countDownAnimator?.cancel()
//
//         if(isNewGame) {
//             selectData = (0 until totalCount).random()
//         }
//         // 랜덤 이미지와 오디오 설정
//         val imageResId = resources.getIdentifier("com_$selectData", "drawable", packageName)
//         val audioResId = resources.getIdentifier("com_$selectData", "raw", packageName)
//
//         imageView.setImageResource(imageResId)
//
//         buttonCover.translationX = 0f
//         buttonCover.visibility = View.VISIBLE
//
//         mediaPlayer?.release()
//         mediaPlayer = MediaPlayer.create(this, audioResId)
//         mediaPlayer?.setOnCompletionListener {
//             mediaPlayer?.release()
//             mediaPlayer = null
//             startButtonEnablePeriod()
//         }
//         mediaPlayer?.start()
//     }
//
//
//     override fun onStop() {
//         super.onStop()
//         finish()
//     }
//
//     private fun userAction() {
//         val isCritical = (1..100).random() <= 5  // 5% 확률
//         val baseDamage = userData.playerATK
//         val randomFactor = (90..110).random() / 100.0
//         var damage = (baseDamage * randomFactor).toLong()
//
//         if (isCritical) {
//             damage *= 2
//             showDamageTextAnimation("CRITICAL!!! $damage",DamageType.CRITICAL)
//         } else {
//             showDamageTextAnimation(damage.toString(),DamageType.USER)
//         }
//
//         comData.comHP -= damage
//
//         if (comData.comHP <= 0) {
//             newGame()
//             startButtonEnablePeriod(true)
//         }
//
//         updateComHP()
//     }
//
//
//     private fun newGame() {
//         pauseGame()
//         showUpgradeSelectionDialog()
//         val level = ++userData.lvCnt
//
//         // 컴퓨터 HP 기본값
//         var baseComHP = 1000 * level
//
//         // 컴퓨터 레벨 보정치: 누적 HP 증가 (예: 레벨당 3% 추가)
//         val comLevelBonus = (baseComHP * level * 0.03).toLong()
//         comData.comHP = baseComHP + comLevelBonus
//
//         // 유저 증가량
//         val hpBoost = (userData.originHP * (5..10).random() / 100.0).toLong()
//         val atkBoost = (userData.playerATK * (3..5).random() / 100.0).toLong()
//
//         // 유저 레벨 보정치: 레벨 * 2% 추가 HP
//         val levelHpBonus = (userData.originHP * level * 0.02).toLong()
//
//         userData.playerHP += hpBoost + levelHpBonus
//         userData.playerATK += atkBoost
//
//         updateStatus()
//         userData.originHP = userData.playerHP
//
//     }
//
//     private fun comAction() {
//         if (isGamePaused) return
//         val damagePercent = (15..30).random() / 100.0
//         val baseDamage = userData.playerATK * damagePercent*10
//
//         // 보정치: 레벨마다 5% 추가 데미지
//         val levelBonus = 1 + (userData.lvCnt * 0.01)
//         val totalDamage = (baseDamage * levelBonus).toInt()
//
//         userData.playerHP -= totalDamage
//         showDamageTextAnimation("$totalDamage",DamageType.COM)
//         updateStatus()
//         if(userData.playerHP<=0){
//             playDataFormat()
//             showDamageTextAnimation("RE GAME",DamageType.CRITICAL)
//         }
//     }
//
//     private fun playDataFormat(){
//         userData= UserData()
//         comData= ComData()
//         updateStatus()
//         updateComHP()
//     }
//
//     private fun showDamageTextAnimation(text: String, damageType:DamageType) {
//         val damageTextView = TextView(this).apply {
//             this.text = text
//             if(damageType==DamageType.CRITICAL){
//                 setTextColor(Color.RED)
//             }else if(damageType==DamageType.USER){
//                 setTextColor(Color.CYAN)
//             }else{
//                 setTextColor(Color.WHITE)
//             }
//             textSize = 50f
//             typeface = Typeface.DEFAULT_BOLD
//             alpha = 1f
//             translationY = 0f
//             visibility = View.VISIBLE
//             id = View.generateViewId() // 제약 조건에 ID 필요
//
//             layoutParams = ConstraintLayout.LayoutParams(
//                 ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                 ConstraintLayout.LayoutParams.WRAP_CONTENT
//             ).apply {
//                 topToTop = ConstraintLayout.LayoutParams.PARENT_ID
//                 bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
//                 startToStart = ConstraintLayout.LayoutParams.PARENT_ID
//                 endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//                 verticalBias = 0.26f
//             }
//         }
//
//         val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
//         rootLayout.addView(damageTextView)
//
//         damageTextView.animate()
//             .translationYBy(-100f)
//             .alpha(0f)
//             .setDuration(1000)
//             .withEndAction {
//                 rootLayout.removeView(damageTextView)
//             }
//             .start()
//     }
//
//
//
//     private fun playSoundWithMinDuration(minDurationMs: Long) {
//         userAction()
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
//
//
//     private fun playSound() {
//         if (mediaPlayer == null) {
//             mediaPlayer = MediaPlayer.create(this, R.raw.tung_sahur)
//             mediaPlayer?.setOnCompletionListener {
//                 mediaPlayer?.release()
//                 mediaPlayer = null
//             }
//         } else {
//             mediaPlayer?.apply {
//                 if (isPlaying) {
//                     stop()
//                     reset()
//                     setDataSource(this@MainActivity, Uri.parse("android.resource://${packageName}/${R.raw.tung_sahur}"))
//                     prepare()
//                 }
//             }
//         }
//         mediaPlayer?.start()
//     }
//
//     private fun showUpgradeSelectionDialog() {
//         val upgrades = arrayOf("HP +100", "ATK +10", "CRITICAL +5%")
//
//         AlertDialog.Builder(this)
//             .setTitle("강화 요소를 선택하세요")
//             .setItems(upgrades) { dialog, which ->
//                 when (which) {
//                     0 -> increaseHp(100)
//                     1 -> increaseAtk(10)
//                     2 -> increaseCritical(5)
//                 }
//                 dialog.dismiss()
//                 resumeGame()
//             }
//             .setCancelable(false)
//             .show()
//     }
//
//     override fun onDestroy() {
//         super.onDestroy()
//         mediaPlayer?.release()
//         mediaPlayer = null
//         countDownAnimator?.cancel()
//     }
//
//     fun pauseGame() {
//         isGamePaused = true
//         buttonEnableJob?.cancel()
//         countDownAnimator?.cancel()
//         mediaPlayer?.pause()
//         button.isEnabled = false
//     }
//
//     fun resumeGame() {
//         if (!isGamePaused) return
//         isGamePaused = false
//         mediaPlayer?.start()
//         lockButtonAndPlayAudio(true)
//     }
//
//
//     private fun increaseHp(amount: Int) {
//         // HP 증가 로직
//     }
//
//     private fun increaseAtk(amount: Int) {
//         // ATK 증가 로직
//     }
//
//     private fun increaseCritical(amount: Int) {
//         // CRITICAL 증가 로직
//     }
// }
