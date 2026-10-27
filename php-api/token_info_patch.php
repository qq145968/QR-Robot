<?php
// Token 方式获取用户信息（APP端用）
if ($method === 'POST' && $action === 'info') {
    $uid = (int)req('user_id', 0);
    $tok = trim(req('token', ''));
    if ($uid > 0 && $tok !== '') {
        $u = $db->fetchOne("SELECT * FROM users WHERE id = ? AND token = ? LIMIT 1", [$uid, $tok]);
        if ($u) {
            echo json_encode([
                'ok' => true,
                'user' => [
                    'id'          => (int)$u['id'],
                    'username'    => $u['username'],
                    'email'       => $u['email'] ?? '',
                    'phone'       => $u['phone'] ?? '',
                    'avatar'      => !empty($u['avatar']) ? $u['avatar'] : strtoupper(substr($u['username'], 0, 1)),
                    'avatar_url'  => !empty($u['avatar']) ? (strpos($u['avatar'], 'http') === 0 ? $u['avatar'] : getBaseUrl() . $u['avatar']) : '',
                    'status'      => (int)$u['status'],
                    'source'      => $u['register_source'] ?? 'app',
                    'login_count' => (int)($u['login_count'] ?? 0),
                    'last_login'  => $u['last_login'] ?? '',
                    'created_at'  => $u['created_at'] ?? '',
                    'updated_at'  => $u['updated_at'] ?? '',
                ],
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }
    echo json_encode(['ok' => false, 'msg' => '用户信息不存在'], JSON_UNESCAPED_UNICODE);
    exit;
}
