package org.example.agent_qr.rag.retriever;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.example.agent_qr.common.rag.IndexableText;
import org.example.agent_qr.common.rag.IndexableTextProvider;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * BM25 关键词检索器 — 基于 Lucene 内存索引。
 * <p>
 * 使用 StandardAnalyzer 进行分词，在内存中构建倒排索引，
 * 支持 BM25 评分检索和增量更新。
 * P3 可升级为 SmartChineseAnalyzer 以获得更好的中文分词效果。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class BM25Retriever {

    @Autowired
    private IndexableTextProvider indexableTextProvider;

    private final ByteBuffersDirectory directory = new ByteBuffersDirectory();
    private final Analyzer analyzer = new StandardAnalyzer();
    private volatile DirectoryReader reader;
    private volatile IndexSearcher searcher;

    /**
     * 启动时从 MySQL 加载全量索引文本构建 Lucene 内存索引。
     */
    @PostConstruct
    public void buildIndex() {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteAll();
                List<IndexableText> texts = indexableTextProvider.findAllIndexable();
                for (IndexableText text : texts) {
                    if (text.getContent() != null && !text.getContent().isBlank()) {
                        addToIndexInternal(writer, text);
                    }
                }
                writer.commit();
            }
            refreshReader();
            log.info("BM25 索引构建完成，共索引 {} 条切片", reader != null ? reader.numDocs() : 0);
        } catch (Exception e) {
            log.error("BM25 索引构建失败", e);
        }
    }

    /**
     * 关键词检索。
     *
     * @param query 查询关键词
     * @param topK  返回的最大结果数
     * @return 检索结果列表
     */
    public List<RetrievedDocument> keywordSearch(String query, int topK) {
        List<RetrievedDocument> results = new ArrayList<>();
        if (searcher == null) {
            return results;
        }

        try {
            QueryParser parser = new QueryParser("content", analyzer);
            Query luceneQuery = parser.parse(QueryParser.escape(query));
            TopDocs topDocs = searcher.search(luceneQuery, topK);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                RetrievedDocument rd = new RetrievedDocument();
                rd.setDocumentId(doc.get("chunkId"));
                rd.setContent(doc.get("content"));
                rd.setDocumentTitle(doc.get("title"));
                rd.setSimilarity((double) scoreDoc.score);
                results.add(rd);
            }
            log.debug("BM25 检索: query={}, 返回 {} 条结果", query, results.size());
        } catch (Exception e) {
            log.error("BM25 检索失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 增量添加文本到索引。
     */
    public void addToIndex(IndexableText text) {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                addToIndexInternal(writer, text);
                writer.commit();
            }
            refreshReader();
        } catch (Exception e) {
            log.error("BM25 索引添加失败: chunkId={}", text.getId(), e);
        }
    }

    /**
     * 从索引中删除切片。
     */
    public void removeFromIndex(Long chunkId) {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteDocuments(new Term("chunkId", chunkId.toString()));
                writer.commit();
            }
            refreshReader();
        } catch (Exception e) {
            log.error("BM25 索引删除失败: chunkId={}", chunkId, e);
        }
    }

    private void addToIndexInternal(IndexWriter writer, IndexableText text) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("chunkId", text.getId().toString(), Field.Store.YES));
        doc.add(new TextField("content", text.getContent(), Field.Store.YES));
        doc.add(new StoredField("title", "chunk-" + text.getChunkIndex()));
        writer.addDocument(doc);
    }

    private void refreshReader() {
        try {
            DirectoryReader newReader = DirectoryReader.open(directory);
            DirectoryReader oldReader = this.reader;
            this.reader = newReader;
            this.searcher = new IndexSearcher(newReader);
            if (oldReader != null) {
                oldReader.close();
            }
        } catch (Exception e) {
            log.error("BM25 索引 Reader 刷新失败", e);
        }
    }
}
