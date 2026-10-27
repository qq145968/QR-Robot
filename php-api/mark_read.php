<?php
require_once __DIR__ . '/config.php';

$user_id = (int)($_POST['user_id'] ?? $_GET['user_id'] ?? 0);
$token = trim($_POST['token'] ?? $_GET['token'] ?? '');
$message_ids = $_POST['message_ids'] ?? $_GET['message_ids'] ?? '';

$authToken = '';
$headers = function_exists('getallheaders') ? (getallheaders() ?: []) : [];
foreach ($headers as $hk => $hv) {
    if (strcasecmp($hk, 'Authorization') === 0 && stripos($hv, 'Bearer ') === 0) {
        $authToken = trim(substr($hv, 7));
    }
}
if ($authToken === '') $authToken = $token;

if ($user_id <= 0 && $authToken !== '') {
    try {
        $u = $db->fetchOne("SELECT id FROM users WHERE token = ? LIMIT 1", [$authToken]);
        if ($u) $user_id = (int)$u['id'];
    } catch (Exception $e) {}
}

if ($user_id <= 0) {
    echo json_encode(['success' => false, 'message' => '用户未登录']);
    exit;
}

if (empty($message_ids)) {
    echo json_encode(['success' => false, 'message' => '缺少消息ID']);
    exit;
}

$ids = array_filter(array_map('intval', explode(',', $message_ids)), fn($x) => $x > 0);
if (empty($ids)) {
    echo json_encode(['success' => false, 'message' => '消息ID无效']);
    exit;
}

$marked = 0;
foreach ($ids as $mid) {
    try {
        $exists = $db->fetchOne("SELECT id FROM app_message_reads WHERE user_id = ? AND message_id = ?", [$user_id, $mid]);
        if (!$exists) {
            $db->insert('app_message_reads', ['user_id' => $user_id, 'message_id' => $mid]);
            $marked++;
        }
    } catch (Exception $e) {
        try {
            $db->insert('app_message_reads', ['user_id' => $user_id, 'message_id' => $mid]);
            $marked++;
        } catch (Exception $_) {}
    }
}

echo json_encode(['success' => true, 'message' => 'ok', 'marked' => $marked]);
