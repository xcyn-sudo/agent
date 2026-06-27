package org.example.agent_qr.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.agent_qr.common.rag.IndexableText;
import org.example.agent_qr.common.rag.IndexableTextProvider;
import org.example.agent_qr.knowledge.entity.Chunk;
import org.example.agent_qr.knowledge.mapper.ChunkMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link IndexableTextProvider} 实现 — 封装 ChunkMapper 查询。
 * <p>
 * 将 MyBatis Mapper 查询结果适配为 {@link IndexableText} 接口，
 * 供 rag 模块的 BM25Retriever 通过接口注入使用，避免循环依赖。
 * 查询时自动过滤已软删除的记录。
 * </p>
 *
 * @author agent-qr
 */
@Component
@RequiredArgsConstructor
public class ChunkIndexableTextProvider implements IndexableTextProvider {

    private final ChunkMapper chunkMapper;

    @Override
    public List<IndexableText> findAllIndexable() {
        // 过滤软删除记录：仅加载未删除的切片
        LambdaQueryWrapper<Chunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Chunk::getDeleted, 0);
        List<Chunk> chunks = chunkMapper.selectList(wrapper);
        return new ArrayList<>(chunks);
    }
}
