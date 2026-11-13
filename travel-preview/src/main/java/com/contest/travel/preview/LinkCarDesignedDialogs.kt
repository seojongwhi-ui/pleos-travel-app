package com.contest.travel.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val dialogInk = Color(0xFF191A1B)
private val dialogSub = Color(0xFFA4A8AE)
private val dialogBlue = Color(0xFF5263E7)

@Composable
private fun DesignDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier, color = Color.White, shape = RoundedCornerShape(48.dp),
            shadowElevation = 18.dp) {
            Column(Modifier.padding(40.dp), content = content)
        }
    }
}

@Composable
internal fun DesignedSettingsDialog(s: WireSession) {
    DesignDialog({ s.showSettings = false }, Modifier.width(950.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("설정", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                color = dialogInk)
            TextButton(onClick = { s.showSettings = false }) { Text("닫기") }
        }
        Spacer(Modifier.height(20.dp))
        listOf(
            Triple("메시지 읽어 주기", "다른 차의 말을 음성으로 읽어 줘요", 0),
            Triple("노크 받기", "노크가 오면 10초 안에 문을 열 수 있어요", 1),
            Triple("길에 남긴 음성 알림", "300m 전에 미리 알려 줘요", 2),
            Triple("음악 신청 받기", "다른 차가 신청한 곡을 함께 들어요", 3),
            Triple("내 점수 공개", "끄면 순위에 이름 대신 점수만 보여요", 4)
        ).forEach { (title, detail, index) ->
            Row(Modifier.fillMaxWidth().height(104.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = dialogInk)
                    Text(detail, style = MaterialTheme.typography.labelSmall, color = dialogSub)
                }
                Switch(checked = s.settingsToggles[index], onCheckedChange = { checked ->
                    s.settingsToggles = s.settingsToggles.toMutableList().also { it[index] = checked }
                }, colors = SwitchDefaults.colors(checkedTrackColor = dialogBlue))
            }
            HorizontalDivider(color = Color(0xFFEDEEF2))
        }
        Spacer(Modifier.height(20.dp))
        Text("바꾼 설정은 이번 동행에만 적용돼요", Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall, color = dialogSub, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun DesignedEndDialog(s: WireSession) {
    DesignDialog({ s.showEnd = false }, Modifier.width(800.dp)) {
        Box(Modifier.size(88.dp).align(Alignment.CenterHorizontally)
            .background(Color(0xFFFF7777), CircleShape), contentAlignment = Alignment.Center) {
            Text("↩", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("동행을 끝낼까요?", Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium, color = dialogInk,
            textAlign = TextAlign.Center)
        Text("다른 차들은 계속 함께 가요\n지금까지의 기록은 내 여행 기록에 남아요",
            Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium,
            color = dialogSub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { s.showEnd = false }, Modifier.weight(1f).height(96.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDEEF2),
                    contentColor = dialogInk)) { Text("계속 함께 가기") }
            Button(onClick = { s.showEnd = false; s.open(WireScreen.ARRIVED) },
                Modifier.weight(1f).height(96.dp),
                colors = ButtonDefaults.buttonColors(containerColor = dialogInk)) {
                Text("동행 종료")
            }
        }
    }
}

@Composable
internal fun DesignedHistoryDialog(s: WireSession) {
    DesignDialog({ s.showHistory = false }, Modifier.width(920.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("이전 대화", Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium, color = dialogInk)
            TextButton(onClick = { s.showHistory = false }) { Text("닫기") }
        }
        Spacer(Modifier.height(20.dp))
        listOf(
            Triple("다연 차 · 3분 전", "우리 휴게소 들렀다 가자!", "제안 · 좋아 3/3"),
            Triple("문준 차 · 8분 전", "이 노래 누가 신청했어? 좋다", "음성"),
            Triple("다연 차 · 12분 전", "마지막에 오는 사람 음료수 좀 사오기~", "길에 남긴 음성"),
            Triple("나 · 20분 전", "다들 출발했어?", "모두에게")
        ).forEach { (who, message, label) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(who, style = MaterialTheme.typography.labelSmall, color = dialogSub)
                Text(message, style = MaterialTheme.typography.titleMedium, color = dialogInk)
                Text(label, style = MaterialTheme.typography.labelSmall, color = dialogBlue)
            }
            HorizontalDivider(color = Color(0xFFEDEEF2))
        }
    }
}
