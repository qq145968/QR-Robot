<!-- 注销管理 -->
<?php
$cancellations = [];
try {
    $cancellations = $db->fetchAll("SELECT * FROM app_account_cancellations ORDER BY created_at DESC LIMIT 100");
} catch (Exception $e) {}

// 处理操作
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['cancel_action'])) {
    $cancelId = (int)($_POST['cancel_id'] ?? 0);
    $action = $_POST['cancel_action'];
    $remark = trim($_POST['admin_remark'] ?? '');

    if ($cancelId > 0) {
        $cancel = $db->fetchOne("SELECT * FROM app_account_cancellations WHERE id = ?", [$cancelId]);
        if ($cancel) {
            if ($action === 'approve') {
                $db->update('app_account_cancellations', [
                    'status' => 'approved',
                    'admin_remark' => $remark,
                    'processed_at' => date('Y-m-d H:i:s')
                ], ['id' => $cancelId]);
                // 彻底删除用户数据
                $db->query("DELETE FROM users WHERE id = ?", [$cancel['user_id']]);
                $db->query("DELETE FROM app_message_reads WHERE user_id = ?", [$cancel['user_id']]);
                echo '<script>showToast("已批准注销，用户数据已彻底删除", "success");</script>';
            } elseif ($action === 'reject') {
                $db->update('app_account_cancellations', [
                    'status' => 'rejected',
                    'admin_remark' => $remark,
                    'processed_at' => date('Y-m-d H:i:s')
                ], ['id' => $cancelId]);
                // 恢复用户状态
                $db->update('users', ['status' => 1], ['id' => $cancel['user_id']]);
                echo '<script>showToast("已驳回注销申请，用户已恢复", "success");</script>';
            }
        }
    }
    // 重新查询
    $cancellations = $db->fetchAll("SELECT * FROM app_account_cancellations ORDER BY created_at DESC LIMIT 100");
}

$statusLabels = [
    'pending' => '<span style="color:#fbbf24;font-weight:600;">待处理</span>',
    'approved' => '<span style="color:#ef4444;font-weight:600;">已批准</span>',
    'rejected' => '<span style="color:#22c55e;font-weight:600;">已驳回</span>',
    'cancelled' => '<span style="color:#6b7280;">已取消</span>'
];
?>

<div class="tab-content <?= $tab === 'cancellation' ? 'active' : '' ?>" data-content="cancellation">
    <div class="card" style="background:#1a1a22;border:1px solid rgba(255,255,255,0.06);border-radius:12px;padding:24px;margin-bottom:20px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;">
            <div>
                <h3 style="color:#fafafa;font-size:16px;margin:0;">🗑️ 账户注销申请管理</h3>
                <p style="color:#80808a;font-size:13px;margin:4px 0 0;">用户提交注销申请后账户立即冻结，30天后彻底删除。管理员可在此批准或驳回。</p>
            </div>
            <div style="display:flex;gap:12px;">
                <div style="background:rgba(251,191,36,0.1);border:1px solid rgba(251,191,36,0.2);border-radius:8px;padding:8px 16px;">
                    <span style="color:#fbbf24;font-size:20px;font-weight:700;">
                        <?php echo count(array_filter($cancellations, fn($c) => $c['status'] === 'pending')); ?>
                    </span>
                    <span style="color:#80808a;font-size:12px;">待处理</span>
                </div>
                <div style="background:rgba(239,68,68,0.1);border:1px solid rgba(239,68,68,0.2);border-radius:8px;padding:8px 16px;">
                    <span style="color:#ef4444;font-size:20px;font-weight:700;">
                        <?php echo count(array_filter($cancellations, fn($c) => $c['status'] === 'approved')); ?>
                    </span>
                    <span style="color:#80808a;font-size:12px;">已批准</span>
                </div>
            </div>
        </div>

        <table class="data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>用户账号</th>
                    <th>登录方式</th>
                    <th>注销原因</th>
                    <th>设备信息</th>
                    <th>IP地址</th>
                    <th>状态</th>
                    <th>申请时间</th>
                    <th>到期删除时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($cancellations)): ?>
                <tr><td colspan="10" class="empty">暂无注销申请记录</td></tr>
                <?php else: foreach ($cancellations as $c): ?>
                <tr>
                    <td><?php echo $c['id']; ?></td>
                    <td style="color:#fafafa;font-weight:500;"><?php echo htmlspecialchars($c['username'] ?? '-'); ?></td>
                    <td><?php echo $c['login_type'] === 'phone' ? '📱 手机号' : '👤 账号'; ?></td>
                    <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;"><?php echo htmlspecialchars($c['reason'] ?? '-'); ?></td>
                    <td style="max-width:150px;overflow:hidden;text-overflow:ellipsis;font-size:12px;"><?php echo htmlspecialchars($c['device_info'] ?? '-'); ?></td>
                    <td style="font-size:12px;"><?php echo htmlspecialchars($c['ip'] ?? '-'); ?></td>
                    <td><?php echo $statusLabels[$c['status']] ?? $c['status']; ?></td>
                    <td style="font-size:12px;"><?php echo $c['created_at']; ?></td>
                    <td style="font-size:12px;"><?php echo $c['expires_at'] ?? '-'; ?></td>
                    <td>
                        <?php if ($c['status'] === 'pending'): ?>
                        <form method="POST" style="display:inline;" onsubmit="return confirmForm(this, '确定批准此注销申请？用户数据将被彻底删除！', {type:'danger',title:'批准注销',confirmText:'确认批准'})">
                            <input type="hidden" name="cancel_action" value="approve">
                            <input type="hidden" name="cancel_id" value="<?php echo $c['id']; ?>">
                            <button type="submit" style="background:rgba(239,68,68,0.15);color:#ef4444;border:1px solid rgba(239,68,68,0.3);padding:4px 12px;border-radius:6px;cursor:pointer;font-size:12px;">批准</button>
                        </form>
                        <form method="POST" style="display:inline;" onsubmit="return confirmForm(this, '确定驳回此注销申请？用户将恢复正常使用。', {type:'success',title:'驳回注销',confirmText:'确认驳回'})">
                            <input type="hidden" name="cancel_action" value="reject">
                            <input type="hidden" name="cancel_id" value="<?php echo $c['id']; ?>">
                            <button type="submit" style="background:rgba(34,197,94,0.15);color:#22c55e;border:1px solid rgba(34,197,94,0.3);padding:4px 12px;border-radius:6px;cursor:pointer;font-size:12px;">驳回</button>
                        </form>
                        <?php else: ?>
                        <span style="color:#6b7280;font-size:12px;">
                            <?php if (!empty($c['admin_remark'])): ?>
                            <?php echo htmlspecialchars($c['admin_remark']); ?>
                            <?php else: ?>
                            已处理
                            <?php endif; ?>
                        </span>
                        <?php endif; ?>
                    </td>
                </tr>
                <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
</div>
