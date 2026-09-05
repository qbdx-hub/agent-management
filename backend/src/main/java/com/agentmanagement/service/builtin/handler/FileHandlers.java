package com.agentmanagement.service.builtin.handler;

import com.agentmanagement.service.builtin.BuiltinToolResult;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.agentmanagement.service.builtin.handler.Params.str;

/**
 * 文件操作类内置工具 handler：全部在沙箱铁笼内（路径解析见 {@link #resolveSafe}）。
 * zip_unpack 带 zip-slip 防护与解压总量上限；delete 拒绝沙箱根本身。
 */
public final class FileHandlers {

    private FileHandlers() {
    }

    /** 复制/打包的单文件大小上限 10MB */
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    /** 解压总量上限 50MB（zip 炸弹防护） */
    private static final long MAX_UNZIP_TOTAL = 50L * 1024 * 1024;
    /** 递归遍历条目上限 */
    private static final int MAX_ENTRIES = 5000;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 路径解析（与 BuiltinToolServiceImpl.resolveSafe 同语义的唯一实现，主类委托到这里）：
     * 沙箱模式规范化后必须仍在沙箱根内，拒绝绝对路径与 .. 穿越；
     * 沙箱外模式（用户已授权）支持绝对路径。
     */
    public static Path resolveSafe(Path root, String rel, boolean outsideSandbox) {
        if (rel == null || rel.trim().isEmpty()) {
            throw new IllegalArgumentException("path 不能为空");
        }
        Path resolved = root.resolve(rel).normalize();
        if (!outsideSandbox && !resolved.startsWith(root)) {
            throw new IllegalArgumentException("路径越界，只允许访问会话沙箱内（可在聊天框开启「沙箱外运行」授权）: " + rel);
        }
        return resolved;
    }

    /** create_dir：递归建目录（mkdir -p 语义） */
    public static BuiltinToolResult createDir(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String rel = str(params.get("path"));
        Path dir = resolveSafe(root, rel, outsideSandbox);
        try {
            Files.createDirectories(dir);
            return BuiltinToolResult.ok("目录已创建: " + rel);
        } catch (IOException e) {
            return BuiltinToolResult.fail("目录创建失败: " + e.getMessage());
        }
    }

    /** delete_path：删除文件/目录（递归），拒绝沙箱根本身 */
    public static BuiltinToolResult deletePath(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String rel = str(params.get("path"));
        Path target = resolveSafe(root, rel, outsideSandbox);
        if (target.equals(root.normalize())) {
            return BuiltinToolResult.fail("不能删除沙箱根目录本身");
        }
        if (!Files.exists(target)) {
            return BuiltinToolResult.fail("路径不存在: " + rel);
        }
        try {
            if (Files.isRegularFile(target)) {
                Files.delete(target);
                return BuiltinToolResult.ok("文件已删除: " + rel);
            }
            final int[] count = {0};
            Files.walkFileTree(target, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return BuiltinToolResult.ok("目录已删除: " + rel + "（含 " + count[0] + " 个文件）");
        } catch (IOException e) {
            return BuiltinToolResult.fail("删除失败: " + e.getMessage());
        }
    }

    /** move_path：移动/重命名（源与目标都过铁笼） */
    public static BuiltinToolResult movePath(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String from = str(params.get("from"));
        String to = str(params.get("to"));
        Path src = resolveSafe(root, from, outsideSandbox);
        Path dst = resolveSafe(root, to, outsideSandbox);
        if (!Files.exists(src)) {
            return BuiltinToolResult.fail("源路径不存在: " + from);
        }
        if (Files.exists(dst)) {
            return BuiltinToolResult.fail("目标已存在: " + to + "（请先改名或删除）");
        }
        try {
            Files.createDirectories(dst.getParent() != null ? dst.getParent() : root);
            Files.move(src, dst);
            return BuiltinToolResult.ok("已移动: " + from + " → " + to);
        } catch (IOException e) {
            return BuiltinToolResult.fail("移动失败: " + e.getMessage());
        }
    }

    /** copy_path：复制文件/目录（递归，单文件 10MB 上限） */
    public static BuiltinToolResult copyPath(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String from = str(params.get("from"));
        String to = str(params.get("to"));
        Path src = resolveSafe(root, from, outsideSandbox);
        Path dst = resolveSafe(root, to, outsideSandbox);
        if (!Files.exists(src)) {
            return BuiltinToolResult.fail("源路径不存在: " + from);
        }
        try {
            final int[] count = {0};
            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.size(file) > MAX_FILE_BYTES) {
                        throw new IOException("文件超过 10MB 上限: " + file.getFileName());
                    }
                    Path rel = src.relativize(file);
                    Files.createDirectories(dst.resolve(rel).getParent() != null
                            ? dst.resolve(rel).getParent() : dst);
                    Files.copy(file, dst.resolve(rel));
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(src)) {
                        Files.createDirectories(dst.resolve(src.relativize(dir)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return BuiltinToolResult.ok("已复制: " + from + " → " + to + "（" + count[0] + " 个文件）");
        } catch (IOException e) {
            return BuiltinToolResult.fail("复制失败: " + e.getMessage());
        }
    }

    /** file_info：大小/行数/修改时间，图片附加尺寸 */
    public static BuiltinToolResult fileInfo(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String rel = str(params.get("path"));
        Path file = resolveSafe(root, rel, outsideSandbox);
        if (!Files.exists(file)) {
            return BuiltinToolResult.fail("路径不存在: " + rel);
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("路径: ").append(rel).append('\n');
            sb.append("类型: ").append(Files.isDirectory(file) ? "目录" : "文件").append('\n');
            sb.append("大小: ").append(Files.size(file)).append(" 字节\n");
            sb.append("修改时间: ").append(Files.getLastModifiedTime(file).toInstant()
                    .atZone(ZoneId.systemDefault()).format(TS)).append('\n');
            if (Files.isRegularFile(file)) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".gif") || name.endsWith(".bmp")) {
                    java.awt.image.BufferedImage img = ImageIO.read(file.toFile());
                    if (img != null) {
                        sb.append("图片尺寸: ").append(img.getWidth()).append(" x ").append(img.getHeight()).append('\n');
                    }
                }
                if (Files.size(file) < 2 * 1024 * 1024) {
                    int lines = countLines(file);
                    if (lines >= 0) {
                        sb.append("行数: ").append(lines).append('\n');
                    }
                }
            }
            return BuiltinToolResult.ok(sb.toString().trim());
        } catch (IOException e) {
            return BuiltinToolResult.fail("读取信息失败: " + e.getMessage());
        }
    }

    /** 行数统计；含 NUL 视为二进制返回 -1 */
    private static int countLines(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int lines = 0, n, total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total <= 8192) {
                    for (int i = 0; i < n; i++) {
                        if (buf[i] == 0) {
                            return -1;
                        }
                    }
                }
                for (int i = 0; i < n; i++) {
                    if (buf[i] == '\n') {
                        lines++;
                    }
                }
            }
            return lines + 1;
        }
    }

    /** zip_pack：把文件/目录打包成 zip */
    public static BuiltinToolResult zipPack(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String srcRel = str(params.get("source"));
        String zipRel = str(params.get("zip_path"));
        Path src = resolveSafe(root, srcRel, outsideSandbox);
        Path zip = resolveSafe(root, zipRel, outsideSandbox);
        if (!Files.exists(src)) {
            return BuiltinToolResult.fail("源路径不存在: " + srcRel);
        }
        final Path srcRoot = src;
        try (OutputStream fos = Files.newOutputStream(zip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            final int[] count = {0};
            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (count[0] >= MAX_ENTRIES) {
                        throw new IOException("条目数超过 " + MAX_ENTRIES + " 上限");
                    }
                    if (Files.size(file) > MAX_FILE_BYTES) {
                        throw new IOException("文件超过 10MB 上限: " + file.getFileName());
                    }
                    String entryName = srcRoot.relativize(file).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }
            });
            return BuiltinToolResult.ok("已打包 " + count[0] + " 个文件 → " + zipRel
                    + "（" + Files.size(zip) + " 字节）");
        } catch (IOException e) {
            try {
                Files.deleteIfExists(zip);
            } catch (IOException ignore) {
                // 清理失败不影响错误返回
            }
            return BuiltinToolResult.fail("打包失败: " + e.getMessage());
        }
    }

    /** zip_unpack：解压（zip-slip 防护 + 总量 50MB 上限） */
    public static BuiltinToolResult zipUnpack(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String zipRel = str(params.get("zip_path"));
        String destRel = str(params.get("dest"));
        Path zip = resolveSafe(root, zipRel, outsideSandbox);
        if (!Files.exists(zip) || Files.isDirectory(zip)) {
            return BuiltinToolResult.fail("zip 文件不存在: " + zipRel);
        }
        Path dest = destRel.isEmpty() ? root : resolveSafe(root, destRel, outsideSandbox);
        try (InputStream in = Files.newInputStream(zip);
             ZipInputStream zis = new ZipInputStream(in)) {
            int count = 0;
            long total = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (count++ >= MAX_ENTRIES) {
                    return BuiltinToolResult.fail("条目数超过 " + MAX_ENTRIES + " 上限，已中止");
                }
                // zip-slip 防护：entry 名规范化后必须仍在目标目录内
                Path target = dest.resolve(entry.getName()).normalize();
                if (!target.startsWith(dest.normalize())) {
                    return BuiltinToolResult.fail("检测到非法压缩路径（zip-slip）: " + entry.getName() + "，已中止");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent() != null ? target.getParent() : dest);
                long written = 0;
                try (OutputStream os = Files.newOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        written += n;
                        total += n;
                        if (total > MAX_UNZIP_TOTAL) {
                            return BuiltinToolResult.fail("解压总量超过 50MB 上限，已中止（疑似 zip 炸弹）");
                        }
                        os.write(buf, 0, n);
                    }
                }
            }
            return BuiltinToolResult.ok("已解压 " + count + " 个条目 → " + (destRel.isEmpty() ? "沙箱根" : destRel));
        } catch (IOException e) {
            return BuiltinToolResult.fail("解压失败: " + e.getMessage());
        }
    }
}
