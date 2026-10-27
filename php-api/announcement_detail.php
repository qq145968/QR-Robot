<?php
/**
 * 公告详情 + 浏览量+1
 * GET: announcement_detail.php?id=1
 */
require_once __DIR__ . '/config.php';

$id = (int)($_GET['id'] ?? $_POST['id'] ?? 0);
if ($id <= 0) {
    echo json_encode(['success' => false, 'message' => '参数错误'], JSON_UNESCAPED_UNICODE);
    exit;
}

try {
    $row = $db->fetchOne("SELECT * FROM app_messages WHERE id = ? AND status IN ('sent','sending','1',1) LIMIT 1", [$id]);
    if (!$row) {
        echo json_encode(['success' => false, 'message' => '公告不存在'], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // 浏览量+1
    try {
        $db->query("UPDATE app_messages SET view_count = view_count + 1 WHERE id = ?", [$id]);
    } catch (Exception $_) {}

    echo json_encode([
        'success' => true,
        'data' => [
            'id'         => (int)$row['id'],
            'title'      => $row['title'] ?? '',
            'content'    => $row['content'] ?? '',
            'type'       => $row['type'] ?? $row['message_type'] ?? 'announcement',
            'view_count' => (int)($row['view_count'] ?? 0) + 1,
            'created_at' => $row['sent_at'] ?? $row['created_at'] ?? '',
        ]
    ], JSON_UNESCAPED_UNICODE);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
