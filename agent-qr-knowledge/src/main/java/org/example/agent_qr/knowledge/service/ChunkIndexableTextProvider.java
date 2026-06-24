package org.example.agent_qr.knowledge.service;

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
        List<Chunk> chunks = chunkMapper.selectList(null);
        return new ArrayList<>(chunks);
    }
}
