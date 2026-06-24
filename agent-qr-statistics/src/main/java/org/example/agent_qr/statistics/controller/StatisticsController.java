package org.example.agent_qr.statistics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.statistics.dto.DashboardVO;
import org.example.agent_qr.statistics.dto.FeedbackDTO;
import org.example.agent_qr.statistics.service.FeedbackService;
import org.example.agent_qr.statistics.service.StatisticsQueryService;
import org.example.agent_qr.user.entity.SysUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计查询控制器（P2 扩展：满意度反馈端点）。
 *
 * @author agent-qr
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;
    private final FeedbackService feedbackService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        log.info("收到仪表盘数据查询请求");
        DashboardVO dashboard = statisticsQueryService.getDashboard();
        return Result.success(dashboard);
    }

    /**
     * 提交满意度反馈（P2 新增）。
     *
     * @param messageId 消息 ID
     * @param dto       反馈请求
     * @return 操作结果
     */
    @PostMapping("/feedback/{messageId}")
    public Result<Void> submitFeedback(@PathVariable Long messageId,
                                        @RequestBody FeedbackDTO dto) {
        Long userId = getCurrentUserId();
        feedbackService.submitFeedback(messageId, dto.getFeedback(), dto.getReason(), userId);
        return Result.success("反馈提交成功");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }
        if (principal instanceof SysUser user) {
            return user.getId();
        }
        throw new RuntimeException("无法获取当前用户信息");
    }
}
