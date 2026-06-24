package org.example.agent_qr.knowledge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;

/**
 * 文件存储服务，负责将上传的文件持久化到本地磁盘。
 * <p>
 * 文件按日期自动归档到子目录（yyyy/MM），
 * 文件名使用时间戳前缀保证唯一性。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 将上传文件存储到本地磁盘。
     * <p>
     * 按当前年月创建子目录，使用 "时间戳_原始文件名" 命名以避免冲突。
     * </p>
     *
     * @param file 上传的 MultipartFile
     * @return 文件的相对存储路径
     * @throws RuntimeException 如果文件存储失败
     */
    public String store(MultipartFile file) {
        try {
            // 将 uploadDir 解析为绝对路径，避免因 Tomcat 临时工作目录导致的路径漂移
            Path basePath = getBasePath();

            // 按日期创建子目录: basePath/yyyy/MM
            YearMonth yearMonth = YearMonth.now();
            String subDir = String.format("%04d/%02d", yearMonth.getYear(), yearMonth.getMonthValue());
            Path dirPath = basePath.resolve(subDir);

            // NIO 创建目录
            Files.createDirectories(dirPath);

            // 生成唯一文件名（原始文件名可能含中文，需要安全处理）
            String originalName = file.getOriginalFilename();
            String uniqueFileName = System.currentTimeMillis() + "_" +
                    (originalName != null ? originalName : "unknown");
            Path filePath = dirPath.resolve(uniqueFileName);

            // 防御性创建父目录（兼容 NIO Path 与 java.io.File 路径解析差异）
            File parentDir = filePath.toFile().getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    log.debug("防御性创建目录: {}", parentDir.getAbsolutePath());
                }
            }

            // 保存文件
            file.transferTo(filePath.toFile());

            String relativePath = subDir + "/" + uniqueFileName;
            log.info("文件已存储: {} -> {}", relativePath, filePath.toAbsolutePath());
            return relativePath;
        } catch (IOException e) {
            log.error("文件存储失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除指定路径的文件。
     *
     * @param filePath 文件的相对路径
     */
    public void delete(String filePath) {
        try {
            Path basePath = getBasePath();
            Path path = basePath.resolve(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("文件已删除: {}", filePath);
            } else {
                log.warn("文件不存在或已删除: {}", filePath);
            }
        } catch (IOException e) {
            log.error("文件删除失败: {} - {}", filePath, e.getMessage(), e);
        }
    }

    /**
     * 将配置的上传目录解析为规范化绝对路径。
     * <p>
     * 相对路径（如 ./uploads）以 JVM 工作目录（user.dir）为基准解析。
     * 若 user.dir 被嵌入式 Tomcat 错误指向临时工作目录，
     * 则自动向上搜索项目根目录（包含 pom.xml 或 .git 的目录）
     * 并以其为基准重新解析上传路径。
     * </p>
     *
     * @return 上传目录的绝对路径（始终位于项目根目录下）
     */
    private Path getBasePath() {
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
        }
        path = path.normalize();

        // 检测是否被解析到了 Tomcat 临时工作目录
        String pathStr = path.toString().replace('\\', '/');
        if (pathStr.contains("/tomcat.") || pathStr.contains("/Temp/")) {
            Path projectRoot = findProjectRoot();
            if (projectRoot != null) {
                // 从配置的相对路径中去掉 ./ 前缀，拼接到项目根目录
                String relativeUploadDir = uploadDir.replaceAll("^\\./", "");
                Path correctedPath = projectRoot.resolve(relativeUploadDir).normalize();
                log.warn("上传目录被解析到临时目录({})，已纠正至项目根目录: {}", path, correctedPath);
                return correctedPath;
            }
        }

        return path;
    }

    /**
     * 从当前类路径位置向上查找项目根目录。
     * <p>
     * 项目根目录的判定标准为同时包含 pom.xml 和 .git 的目录，
     * 回退为仅包含 pom.xml。
     * </p>
     *
     * @return 项目根目录路径，找不到时返回 null
     */
    private Path findProjectRoot() {
        try {
            // 从类文件所在目录开始向上查找
            Path current = Paths.get(getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(current)) {
                // jar 包场景：从 jar 所在目录开始
                current = current.getParent();
            }

            while (current != null && current.getParent() != null) {
                if (Files.exists(current.resolve("pom.xml"))
                        && Files.exists(current.resolve(".git"))) {
                    return current;
                }
                current = current.getParent();
            }

            // 回退：仅查找包含 pom.xml 的目录
            current = Paths.get(getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(current)) {
                current = current.getParent();
            }
            while (current != null && current.getParent() != null) {
                if (Files.exists(current.resolve("pom.xml"))) {
                    return current;
                }
                current = current.getParent();
            }
        } catch (Exception e) {
            log.debug("查找项目根目录失败: {}", e.getMessage());
        }
        return null;
    }
}
