package org.example.agent_qr.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * 数据指纹工具类。
 * <p>
 * 提供 MD5 哈希和记录指纹计算方法，用于去重检测（DeduplicationRule）
 * 和 ETL 管线（Chunk.recordHash 写入），确保哈希算法在全局一致。
 * </p>
 *
 * @author agent-qr
 */
public final class FingerprintUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FingerprintUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 计算字符串的 MD5 哈希（32 位十六进制小写）。
     *
     * @param input 输入字符串
     * @return MD5 哈希值
     */
    public static String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 是 Java 标准算法，不会抛出此异常
            return Integer.toHexString(input.hashCode());
        }
    }

    /**
     * 计算数据记录的指纹（JSON 序列化后取 MD5）。
     * <p>
     * 使用 Jackson 将 Map 序列化为 JSON 字符串，然后计算 MD5。
     * 注意：JSON 序列化需要稳定的 key 顺序，否则同一记录可能产生不同指纹。
     * Jackson 默认按 key 的自然顺序序列化 LinkedHashMap/TreeMap，
     * 对于 HashMap 则顺序不确定。调用方应确保传入的 Map 有可预测的 key 顺序。
     * </p>
     *
     * @param record 数据记录（字段名 → 字段值）
     * @return 32 位 MD5 指纹
     */
    public static String computeRecordFingerprint(Map<String, Object> record) {
        if (record == null || record.isEmpty()) {
            return "d41d8cd98f00b204e9800998ecf8427e"; // 空字符串的 MD5
        }
        try {
            String json = MAPPER.writeValueAsString(record);
            return md5Hash(json);
        } catch (Exception e) {
            // 序列化失败时回退到 record 的 toString 哈希
            return md5Hash(record.toString());
        }
    }
}
