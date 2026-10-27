<?php
require_once __DIR__ . '/config.php';

$user_id = (int)($_POST['user_id'] ?? $_GET['user_id'] ?? 0);
$token = trim($_POST['token'] ?? $_GET['token'] ?? '');
$reason = trim($_POST['reason'] ?? $_GET['reason'] ?? '');
$device_info = trim($_POST['device_info'] ?? $_GET['device_info'] ?? '');

$authToken = '';
$headers = function_exists('getallheaders') ? (getallheaders() ?: []) : [];
foreach ($headers as $hk => $hv) {
    if (strcasecmp($hk, 'Authorization') === 0 && stripos($hv, 'Bearer ') === 0) {
        $authToken = trim(substr($hv, 7));
    }
}
if ($authToken === '') $authToken = $token;

if ($authToken !== '') {
    try {
        $u = $db->fetchOne("SELECT id, username, phone FROM users WHERE token = ? LIMIT 1", [$authToken]);
        if ($u) {
            $user_id = (int)$u['id'];
        }
    } catch (Exception $e) {}
}

if ($user_id <= 0) {
    echo json_encode(['success' => false, 'message' => '用户未登录']);
    exit;
}

// 检查是否已有待处理的注销申请
$existing = $db->fetchOne(
    "SELECT id, status, created_at FROM app_account_cancellations WHERE user_id = ? AND status = 'pending' ORDER BY id DESC LIMIT 1",
    [$user_id]
);
if ($existing) {
    echo json_encode(['success' => false, 'message' => '您已有待处理的注销申请，请勿重复提交']);
    exit;
}

// 获取用户信息
$user = $db->fetchOne("SELECT username, phone FROM users WHERE id = ? LIMIT 1", [$user_id]);
if (!$user) {
    echo json_encode(['success' => false, 'message' => '用户不存在']);
    exit;
}

// 判断登录方式
$loginType = 'account';
$displayAccount = $user['username'] ?? '';
if (!empty($user['phone']) && preg_match('/^1[3-9]\d{9}$/', $user['phone'])) {
    $loginType = 'phone';
    $displayAccount = $user['phone'];
}

$ip = $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['HTTP_X_REAL_IP'] ?? $_SERVER['REMOTE_ADDR'] ?? '';

$expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));

try {
    $db->insert('app_account_cancellations', [
        'user_id' => $user_id,
        'username' => $displayAccount,
        'login_type' => $loginType,
        'reason' => $reason ?: '用户主动注销',
        'device_info' => $device_info,
        'ip' => $ip,
        'expires_at' => $expiresAt
    ]);

    // 冻结账户
    $db->update('users', ['status' => 0], ['id' => $user_id]);

    echo json_encode([
        'success' => true,
        'message' => '注销申请已提交，账户已冻结，30天后将彻底删除数据',
        'expires_at' => $expiresAt
    ]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => '提交失败: ' . $e->getMessage()]);
}
