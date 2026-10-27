package com.scanrobot.app

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.scanrobot.app.ui.DetailScreen
import com.scanrobot.app.ui.HomeScreen
import com.scanrobot.app.ui.ScannerScreen
import com.scanrobot.app.ui.theme.ScanRobotTheme
import com.scanrobot.app.viewmodel.ScanViewModel
import com.scanrobot.app.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("scan_robot_prefs", Context.MODE_PRIVATE)
        val privacyAccepted = sharedPrefs.getBoolean("privacy_accepted", false)

        // Check for crash log
        val crashPrefs = getSharedPreferences("crash_log", Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)

        setContent {
            ScanRobotTheme {
                var showPrivacyDialog by remember { mutableStateOf(!privacyAccepted) }
                var isPrivacyAccepted by remember { mutableStateOf(privacyAccepted) }
                val currentScreen by viewModel.currentScreen.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var lastBackPress by remember { mutableLongStateOf(0L) }
                var showCrashDialog by remember { mutableStateOf(lastCrash != null) }

                // 隐私弹窗
                if (showPrivacyDialog) {
                    PrivacyDialog(
                        onAccept = {
                            sharedPrefs.edit().putBoolean("privacy_accepted", true).apply()
                            isPrivacyAccepted = true
                            showPrivacyDialog = false
                        },
                        onReject = {
                            finish()
                        }
                    )
                }

                // Show crash dialog if there was a crash
                if (showCrashDialog && lastCrash != null && isPrivacyAccepted) {
                    AlertDialog(
                        onDismissRequest = {
                            crashPrefs.edit().clear().commit()
                            showCrashDialog = false
                        },
                        title = { Text("应用崩溃日志") },
                        text = {
                            LazyColumn {
                                items(lastCrash.split("\n")) { line ->
                                    Text(
                                        text = line,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                crashPrefs.edit().clear().commit()
                                showCrashDialog = false
                            }) {
                                Text("知道了")
                            }
                        }
                    )
                }

                BackHandler(enabled = isPrivacyAccepted) {
                    when (currentScreen) {
                        is Screen.Scanner, is Screen.Detail -> {
                            viewModel.goBack()
                        }
                        else -> {
                            val now = System.currentTimeMillis()
                            if (now - lastBackPress < 2000) {
                                finish()
                            } else {
                                lastBackPress = now
                                viewModel.showToast("再按一次退出应用")
                            }
                        }
                    }
                }

                LaunchedEffect(toastMessage) {
                    val msg = toastMessage
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearToast()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isPrivacyAccepted) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> HomeScreen(
                                viewModel = viewModel
                            )
                            is Screen.Scanner -> ScannerScreen(viewModel)
                            is Screen.Detail -> DetailScreen(viewModel, screen.batchId)
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) { snackbarData ->
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xF0312D4E),
                            shadowElevation = 12.dp,
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .padding(bottom = 64.dp, start = 32.dp, end = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5CC),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = snackbarData.visuals.message,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyDialog(
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    var showPrivacyText by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.9f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题
                    Text(
                        text = "服务协议和隐私政策",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 提示内容
                    Text(
                        text = buildAnnotatedString {
                            append("欢迎使用扫码机器人！\n\n")
                            append("为了更好地保障您的个人权益，在使用产品前，请您认真阅读并充分理解")
                            withStyle(SpanStyle(color = Color(0xFF1677FF))) {
                                append("《用户服务协议》")
                            }
                            append("和")
                            withStyle(SpanStyle(color = Color(0xFF1677FF))) {
                                append("《隐私政策》")
                            }
                            append("的各条款，包括但不限于：为了向您提供账号注册、扫码记录、消息推送等功能，我们会收集、使用和存储必要的个人信息。\n\n")
                            append("您可点击链接查看完整协议内容。如您同意，请点击\"同意并继续\"开始使用我们的产品和服务。")
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 链接区域
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showPrivacyText = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "点击查看完整协议内容 >",
                            fontSize = 13.sp,
                            color = Color(0xFF1677FF)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF999999)
                            )
                        ) {
                            Text(
                                text = "不同意退出",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onAccept,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1677FF),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "同意并继续",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // 隐私政策全文弹窗
    if (showPrivacyText) {
        Dialog(
            onDismissRequest = { showPrivacyText = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 标题栏
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "隐私政策",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        TextButton(
                            onClick = { showPrivacyText = false },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text("关闭", color = Color(0xFF1677FF))
                        }
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color(0xFFEEEEEE))
                    )

                    // 协议内容
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        PrivacyPolicyContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "扫码机器人隐私政策",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "更新日期：2026年8月22日",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        PrivacySection(
            title = "一、我们如何收集和使用您的个人信息",
            content = """
                我们仅会出于本政策所述的以下目的，收集和使用您的个人信息：

                1. 账号注册与登录
                当您注册、登录账号时，我们会收集您的账号信息（用户名/手机号、密码），用于身份验证和账号安全。

                2. 扫码功能
                当您使用扫码功能时，我们会收集您扫描的二维码内容、扫描时间、设备信息，用于生成扫码记录和统计。

                3. 消息推送
                我们会向您推送系统公告、消息通知，您可以在设置中关闭非必要通知。

                4. 应用安全
                我们会收集设备标识、日志信息，用于保障应用安全运行和故障排查。
            """.trimIndent()
        )

        PrivacySection(
            title = "二、我们如何使用权限",
            content = """
                为了向您提供服务，我们会在必要时申请以下权限：

                1. 相机权限：用于扫描二维码，您可以拒绝后手动输入二维码内容。
                2. 存储权限：用于保存扫码记录、下载更新包。
                3. 网络权限：用于与服务器通信、同步数据。

                您可以在系统设置中随时关闭这些权限，关闭后可能影响部分功能使用。
            """.trimIndent()
        )

        PrivacySection(
            title = "三、我们如何存储和保护您的信息",
            content = """
                1. 我们会在本应用服务所必需的期限内保留您的个人信息。
                2. 我们使用加密技术（HTTPS/SSL）传输和存储您的敏感信息。
                3. 我们不会向第三方共享、转让您的个人信息，除非获得您的明确同意。
                4. 如发生个人信息安全事件，我们将按照法律法规要求及时告知您。
            """.trimIndent()
        )

        PrivacySection(
            title = "四、您的权利",
            content = """
                您对您的个人信息享有以下权利：

                1. 查询、更正您的个人信息
                2. 删除您的个人信息
                3. 撤回同意授权
                4. 注销账号

                您可以通过应用内\"我的\"页面相关功能或联系客服行使以上权利。
            """.trimIndent()
        )

        PrivacySection(
            title = "五、未成年人保护",
            content = """
                我们非常重视未成年人的个人信息保护。如果您是未满18周岁的未成年人，请在监护人陪同下阅读本政策，并在取得监护人同意后使用我们的服务。
            """.trimIndent()
        )

        PrivacySection(
            title = "六、联系我们",
            content = """
                如果您对本隐私政策有任何疑问、意见或建议，可通过以下方式联系我们：

                客服邮箱：s***@scanrobot.com
                客服热线：400-xxx-xxxx

                我们会在收到您的反馈后15个工作日内予以回复。
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "感谢您的阅读！",
            fontSize = 13.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = content,
            fontSize = 13.sp,
            color = Color(0xFF666666),
            lineHeight = 20.sp
        )
    }
}
