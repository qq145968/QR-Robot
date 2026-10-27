<?php
/**
 * APP 用户信息接口
 * GET /api/app_user.php?user_id=xxx&token=xxx
 * 返回: { ok, user: {...} }
 */
require_once __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, X-Requested-With');

$userId = (int)($_GET['user_id'] ?? $_POST['user_id'] ?? 0);
$token = trim($_GET['token'] ?? $_POST['token'] ?? '');

if ($userId <= 0 || empty($token)) {
    echo json_encode(['ok' => false, 'msg' => '参数不完整'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 验证 token
try {
    $user = $db->fetchOne("SELECT * FROM users WHERE id = ? AND token = ? LIMIT 1", [$userId, $token]);
    if (!$user) {
        echo json_encode(['ok' => false, 'msg' => '登录已过期', 'code' => 401], JSON_UNESCAPED_UNICODE);
        exit;
    }
} catch (Exception $e) {
    // token 列可能不存在，尝试兼容
    try {
        $col = $db->fetchOne("SHOW COLUMNS FROM `users` LIKE 'token'");
        if (empty($col)) {
            $db->query("ALTER TABLE `users` ADD COLUMN `token` VARCHAR(128) DEFAULT NULL COMMENT '会话Token' AFTER `status`");
        }
    } catch (Exception $_) {}
    echo json_encode(['ok' => false, 'msg' => '登录已过期', 'code' => 401], JSON_UNESCAPED_UNICODE);
    exit;
}

// 检查封禁是否过期
if ((int)$user['status'] === 0 && !empty($user['banned_until'])) {
    try {
        $expireRow = $db->fetchOne("SELECT TIMESTAMPDIFF(SECOND, NOW(), ?) as diff", [$user['banned_until']]);
        if ($expireRow && (int)$expireRow['diff'] <= 0) {
            $db->update('users', ['status' => 1, 'banned_at' => null, 'banned_until' => null], ['id' => $userId]);
            $user['status'] = 1;
        }
    } catch (Exception $_) {}
}

function toAbsoluteUrl($path) {
    if ($path === '' || $path === null) return '';
    if (strpos($path, 'http') === 0) return $path;
    $scheme = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on') ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'] ?? '';
    return $scheme . '://' . $host . (strpos($path, '/') === 0 ? $path : '/' . $path);
}

$avatarUrl = '';
if (!empty($user['avatar'])) {
    if (strpos($user['avatar'], 'http') === 0) {
        $avatarUrl = $user['avatar'];
    } else {
        $avatarUrl = toAbsoluteUrl($user['avatar']);
    }
}

echo json_encode([
    'ok' => true,
    'user' => [
        'id'          => (int)$user['id'],
        'username'    => $user['username'],
        'email'       => $user['email'] ?? '',
        'phone'       => $user['phone'] ?? '',
        'avatar'      => !empty($user['avatar']) ? $user['avatar'] : strtoupper(substr($user['username'], 0, 1)),
        'avatar_url'  => $avatarUrl,
        'status'      => (int)$user['status'],
        'source'      => $user['register_source'] ?? 'website',
        'login_count' => (int)($user['login_count'] ?? 0),
        'last_login'  => $user['last_login'] ?? '',
        'created_at'  => $user['created_at'] ?? '',
        'updated_at'  => $user['updated_at'] ?? '',
    ],
], JSON_UNESCAPED_UNICODE);
