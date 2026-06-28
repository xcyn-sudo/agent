package org.example.agent_qr.common.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS 读写分离 AOP 切面（P3）。
 * <p>
 * 拦截所有标注 {@link Transactional @Transactional} 的方法，
 * 从 {@code readOnly} 属性读取读写标志并设置到
 * {@link ReadWriteRoutingDataSource} 的 ThreadLocal 中，
 * 实现自动数据源路由。
 * </p>
 *
 * <p><b>执行顺序：</b></p>
 * <ul>
 *   <li>{@code @Order(-1)} 确保在 Spring 事务拦截器之前执行</li>
 *   <li>{@code finally} 块清理 ThreadLocal，防止线程池污染</li>
 * </ul>
 *
 * <p><b>路由逻辑：</b></p>
 * <ul>
 *   <li>{@code @Transactional(readOnly = true)} → 读库</li>
 *   <li>{@code @Transactional(readOnly = false)} → 写库</li>
 *   <li>{@code @Transactional}（默认 readOnly=false）→ 写库</li>
 * </ul>
 *
 * @see ReadWriteRoutingDataSource
 */
@Aspect
@Component
@Order(-1)
@Slf4j
public class ReadWriteDataSourceAspect {

    /**
     * 环绕通知：根据 {@link Transactional#readOnly()} 设置数据源路由标志。
     *
     * @param pjp          切点连接点
     * @param transactional 事务注解（由 Spring 自动绑定）
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("@annotation(transactional)")
    public Object routeDataSource(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        boolean readOnly = transactional.readOnly();
        log.debug("事务只读标志: readOnly={}, method={}", readOnly, pjp.getSignature());
        ReadWriteRoutingDataSource.setReadOnly(readOnly);
        try {
            return pjp.proceed();
        } finally {
            // ★ 必须清理，防止 ThreadLocal 泄漏污染线程池中的线程
            ReadWriteRoutingDataSource.clear();
        }
    }
}
