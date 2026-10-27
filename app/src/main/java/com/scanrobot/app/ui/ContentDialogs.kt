package com.scanrobot.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scanrobot.app.util.QRCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactInfo(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val org: String = "",
    val title: String = ""
) {
    fun toVCard(): String {
        return buildString {
            append("BEGIN:VCARD\nVERSION:3.0\n")
            if (name.isNotEmpty()) append("FN:$name\n")
            if (org.isNotEmpty()) append("ORG:$org\n")
            if (title.isNotEmpty()) append("TITLE:$title\n")
            if (phone.isNotEmpty()) append("TEL:$phone\n")
            if (email.isNotEmpty()) append("EMAIL:$email\n")
            append("END:VCARD")
        }
    }
}

data class FormField(
    val name: String = "",
    val type: String = "text",
    val required: Boolean = false
)

data class LocationInfo(
    val latitude: String = "",
    val longitude: String = "",
    val address: String = ""
) {
    fun toGeoUri(): String {
        return if (latitude.isNotEmpty() && longitude.isNotEmpty()) {
            "geo:$latitude,$longitude" + if (address.isNotEmpty()) "?q=$address" else ""
        } else ""
    }
}

@Composable
fun LinkInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    InputDialogBase(
        title = "链接",
        onDismiss = onDismiss,
        onConfirm = {
            val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            onConfirm(finalUrl)
        },
        confirmEnabled = url.isNotBlank()
    ) {
        Text("输入网页/跳转链接，扫码后自动唤起浏览器", fontSize = 13.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(12.dp))
        Text("链接地址", fontSize = 13.sp, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://example.com", fontSize = 14.sp, color = Color(0xFFBBBBBB)) },
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1677FF),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
fun ContactInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (ContactInfo) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    InputDialogBase(
        title = "联系方式",
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(ContactInfo(name, phone, email, org, title))
        },
        confirmEnabled = name.isNotBlank() || phone.isNotBlank()
    ) {
        Text("录入个人信息，生成电子名片二维码", fontSize = 13.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(12.dp))
        ContactField("姓名", name) { name = it }
        ContactField("电话", phone, KeyboardType.Phone) { phone = it }
        ContactField("邮箱", email, KeyboardType.Email) { email = it }
        ContactField("公司/组织", org) { org = it }
        ContactField("职位", title) { title = it }
    }
}

@Composable
private fun ContactField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Text(label, fontSize = 13.sp, color = Color(0xFF333333))
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1677FF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun LocationInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocationInfo) -> Unit
) {
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    InputDialogBase(
        title = "地理位置",
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(LocationInfo(lat, lng, address))
        },
        confirmEnabled = lat.isNotBlank() && lng.isNotBlank()
    ) {
        Text("标记经纬度位置信息，扫码后唤起地图导航", fontSize = 13.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    Text("纬度", fontSize = 13.sp, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("39.9042", fontSize = 14.sp, color = Color(0xFFBBBBBB)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1677FF),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    Text("经度", fontSize = 13.sp, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = lng,
                        onValueChange = { lng = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("116.4074", fontSize = 14.sp, color = Color(0xFFBBBBBB)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1677FF),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("地址描述（选填）", fontSize = 13.sp, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
            singleLine = true,
            placeholder = { Text("北京市天安门广场", fontSize = 14.sp, color = Color(0xFFBBBBBB)) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1677FF),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
fun FormEditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<FormField>) -> Unit
) {
    var fields by remember { mutableStateOf(listOf(FormField())) }

    InputDialogBase(
        title = "表单",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(fields.filter { it.name.isNotBlank() }) },
        confirmEnabled = fields.any { it.name.isNotBlank() }
    ) {
        Text("自定义编辑表单字段，扫码后可填写提交", fontSize = 13.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(12.dp))
        fields.forEachIndexed { index, field ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = field.name,
                    onValueChange = { newName ->
                        fields = fields.toMutableList().apply { this[index] = this[index].copy(name = newName) }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
                    singleLine = true,
                    placeholder = { Text("字段名称", fontSize = 14.sp, color = Color(0xFFBBBBBB)) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1677FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (field.required) "必填" else "选填",
                    fontSize = 12.sp,
                    color = if (field.required) Color(0xFF1677FF) else Color(0xFF999999),
                    modifier = Modifier.clickable {
                        fields = fields.toMutableList().apply { this[index] = this[index].copy(required = !field.required) }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除",
                    tint = Color(0xFF999999),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            fields = fields.filterIndexed { i, _ -> i != index }
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { fields = fields + FormField() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("+ 添加字段", fontSize = 14.sp)
        }
    }
}

@Composable
fun FilePickerSheet(
    mimeType: String,
    title: String,
    onDismiss: () -> Unit,
    onFileSelected: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "未知文件"
            onFileSelected(uri, fileName)
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.height(16.dp))
                Text("请选择本地文件上传，生成后扫码可查看", fontSize = 13.sp, color = Color(0xFF999999), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { launcher.launch(mimeType) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))
                ) {
                    Text("选择文件", fontSize = 15.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消", fontSize = 14.sp, color = Color(0xFF999999))
                }
            }
        }
    }
}

@Composable
fun QRResultDialog(
    bitmap: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("二维码已生成", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.height(20.dp))
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "生成的二维码",
                        modifier = Modifier.size(260.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("生成失败", color = Color(0xFF999999))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("容错等级：30%（H）", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1677FF),
                            containerColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("分享", fontSize = 14.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("关闭", fontSize = 14.sp, color = Color(0xFF999999))
                }
            }
        }
    }
}

@Composable
fun PreviewDialog(
    content: String,
    type: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("扫码预览效果", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.height(16.dp))
                Text("内容类型：$type", fontSize = 13.sp, color = Color(0xFF999999))
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFF5F6F8), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(content, fontSize = 14.sp, color = Color(0xFF333333))
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("关闭", fontSize = 14.sp, color = Color(0xFF1677FF))
                }
            }
        }
    }
}

@Composable
private fun InputDialogBase(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF999999),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text("取消", fontSize = 15.sp)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        enabled = confirmEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))
                    ) {
                        Text("确定", fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

data class QRStyleConfig(
    val foregroundColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val useRoundDots: Boolean = false,
    val logoBitmap: android.graphics.Bitmap? = null
)

@Composable
fun StyleConfigDialog(
    content: String,
    onDismiss: () -> Unit,
    onConfirm: (QRStyleConfig) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var fgColor by remember { mutableStateOf(Color.Black) }
    var bgColor by remember { mutableStateOf(Color.White) }
    var useRoundDots by remember { mutableStateOf(false) }
    var logoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val logoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val input = context.contentResolver.openInputStream(uri)
            logoBitmap = android.graphics.BitmapFactory.decodeStream(input)
        }
    }

    LaunchedEffect(fgColor, bgColor, useRoundDots, logoBitmap) {
        if (content.isNotBlank()) {
            val bmp = withContext(kotlinx.coroutines.Dispatchers.IO) {
                QRCodeGenerator.generateStyledQRCode(
                    content = content,
                    foregroundColor = fgColor.toArgb(),
                    backgroundColor = bgColor.toArgb(),
                    useRoundDots = useRoundDots,
                    logoBitmap = logoBitmap
                )
            }
            previewBitmap = bmp
        }
    }

    val fgColors = listOf(
        Color(0xFF000000) to "黑",
        Color(0xFF1677FF) to "蓝",
        Color(0xFF52C41A) to "绿",
        Color(0xFFFF4D4F) to "红",
        Color(0xFF722ED1) to "紫",
        Color(0xFFFA8C16) to "橙",
        Color(0xFF13C2C2) to "青",
        Color(0xFFEB2F96) to "粉"
    )
    val bgColors = listOf(
        Color(0xFFFFFFFF) to "白",
        Color(0xFFF6FFED) to "浅绿",
        Color(0xFFE6F7FF) to "浅蓝",
        Color(0xFFFFF1F6) to "浅粉",
        Color(0xFFFFF7E6) to "浅黄",
        Color(0xFFF9F0FF) to "浅紫",
        Color(0xFF000000) to "黑底",
        Color.Transparent to "透明"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("样式库", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(24.dp).clickable { onDismiss() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("预览", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFF5F6F8), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "预览二维码",
                                modifier = Modifier.size(180.dp)
                            )
                        } else {
                            Text("输入内容后预览", fontSize = 13.sp, color = Color(0xFF999999))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("前景色", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        fgColors.forEach { (color, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (fgColor == color) 3.dp else 0.dp,
                                        color = if (fgColor == color) Color(0xFF1677FF) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { fgColor = color }
                            ) {
                                if (fgColor == color) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.Black) Color.White else Color.Black,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("背景色", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bgColors.forEach { (color, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (bgColor == color) 3.dp else 0.dp,
                                        color = if (bgColor == color) Color(0xFF1677FF) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { bgColor = color }
                            ) {
                                if (bgColor == color) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.Black) Color.White else Color.Black,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("码点形状", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = !useRoundDots,
                            onClick = { useRoundDots = false },
                            label = { Text("方块", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1677FF),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = useRoundDots,
                            onClick = { useRoundDots = true },
                            label = { Text("圆点", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1677FF),
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Logo水印", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap!!.asImageBitmap(),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { logoBitmap = null },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("移除", fontSize = 13.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { logoLauncher.launch("image/*") },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("+ 上传Logo", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text("取消", fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(QRStyleConfig(fgColor, bgColor, useRoundDots, logoBitmap))
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))
                    ) {
                        Text("应用样式", fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
