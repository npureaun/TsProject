package com.example.ts_project.gamedata.status

data class ComData(
    var comHP: Long = 1000,
    var backupHP:Long=1000,
    var originHP:Long=1000,
    var comATK: Long = 100,
    var criticalRate: Long = 5  // 기본 크리티컬 확률 5%
)
