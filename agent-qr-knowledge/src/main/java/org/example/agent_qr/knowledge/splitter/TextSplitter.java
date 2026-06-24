package org.example.agent_qr.knowledge.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片器，将长文本按语义边界切割为适合向量化的短片段。
 * <p>
 * 核心策略：先按空行（段落）分割，对长段落使用滑动窗口裁剪，
 * 在断点附近优先选择自然断句位置，最后合并过短的片段。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class TextSplitter {

    /** 每个切片的最大字符数 */
    @Value("${rag.chunk-size:500}")
    private int chunkSize;

    /** 相邻切片之间的重叠字符数 */
    @Value("${rag.chunk-overlap:50}")
    private int chunkOverlap;

    /** 过短切片阈值，小于此长度的切片将合并到前一个切片 */
    private static final int SHORT_CHUNK_THRESHOLD = 100;

    /**
     * 将文本分割为切片列表。
     *
     * @param text 原始文本
     * @return 切片列表
     */
    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        // 1. 按空行分割为段落
        String[] paragraphs = text.split("\n\n");
        List<String> chunks = new ArrayList<>();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > chunkSize) {
                // 长段落使用滑动窗口切片
                chunks.addAll(splitLongText(trimmed));
            } else {
                chunks.add(trimmed);
            }
        }

        // 2. 合并过短片段
        chunks = mergeShortChunks(chunks);

        log.debug("文本切片完成: 总字符数={}, 切片数={}", text.length(), chunks.size());
        return chunks;
    }

    /**
     * 使用滑动窗口对长文本进一步切片。
     * <p>
     * 窗口大小 = chunkSize，步长 = chunkSize - chunkOverlap。
     * 在窗口末尾附近寻找自然断点以获得更语义化的切片。
     * </p>
     *
     * @param text 长文本
     * @return 切片列表
     */
    List<String> splitLongText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            // 如果还没到文本末尾，在 end 附近寻找更好的断点
            if (end < length) {
                end = findBreakPoint(text, end);
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 已到达文本末尾，退出循环
            if (end >= length) {
                break;
            }

            // 下一次起始位置 = 当前结束位置 - 重叠量
            // 同时保证 start 严格递增，防止死循环
            start = Math.max(start + 1, end - chunkOverlap);
        }

        return chunks;
    }

    /**
     * 在目标位置附近寻找最自然的语义断点。
     * <p>
     * 在 {@code [end-50, end]} 范围内搜索断点，优先级如下：
     * 句号 &gt; 换行 &gt; 感叹号 &gt; 问号 &gt; 分号 &gt; 中文分号 &gt; 逗号 &gt; 空格。
     * 若找不到任何断点，返回原 end 值。
     * </p>
     *
     * @param text 文本
     * @param end  目标结束位置
     * @return 最优断点位置
     */
    int findBreakPoint(String text, int end) {
        int searchStart = Math.max(end - 50, 0);

        // 按优先级查找断点
        char[] breakChars = {'。', '\n', '！', '？', ';', '；', '，', ' '};
        for (char ch : breakChars) {
            int pos = text.lastIndexOf(ch, end);
            if (pos >= searchStart) {
                // 对于换行和空格，断点在之后
                return (ch == '\n' || ch == ' ') ? pos + 1 : pos + 1;
            }
        }

        return end;
    }

    /**
     * 合并过短的切片到前一个切片中。
     * <p>
     * 长度小于 100 字符的切片将被合并到它前面的切片末尾。
     * 如果第一个切片就过短，则保留原样。
     * </p>
     *
     * @param chunks 原始切片列表
     * @return 合并后的切片列表
     */
    List<String> mergeShortChunks(List<String> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<String> result = new ArrayList<>();
        result.add(chunks.get(0));

        for (int i = 1; i < chunks.size(); i++) {
            String current = chunks.get(i);
            if (current.length() < SHORT_CHUNK_THRESHOLD && !result.isEmpty()) {
                // 合并到前一条
                int lastIndex = result.size() - 1;
                result.set(lastIndex, result.get(lastIndex) + "\n" + current);
            } else {
                result.add(current);
            }
        }

        return result;
    }
}
