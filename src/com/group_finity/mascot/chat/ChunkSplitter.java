package com.group_finity.mascot.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器。
 * 将 character/ 下的所有 .txt 文件按段落/语义边界分块，
 * 每个 chunk 200-500 字，用于 RAG 检索。
 */
public class ChunkSplitter {

    /** 目标 chunk 大小（字符数） */
    private static final int TARGET_CHUNK_SIZE = 300;
    private static final int MIN_CHUNK_SIZE = 100;
    private static final int MAX_CHUNK_SIZE = 600;

    /**
     * 分块结果。
     */
    public static class Chunk {
        public final String id;
        public final String sourceFile;
        public final String text;

        public Chunk(String id, String sourceFile, String text) {
            this.id = id;
            this.sourceFile = sourceFile;
            this.text = text;
        }

        @Override
        public String toString() {
            return "[" + sourceFile + "] " + text.substring(0, Math.min(50, text.length())) + "...";
        }
    }

    /**
     * 从文件夹加载所有 .txt 文件并分块。
     */
    public List<Chunk> splitAll(File characterDir) {
        List<Chunk> allChunks = new ArrayList<>();
        if (!characterDir.exists() || !characterDir.isDirectory()) {
            return allChunks;
        }

        List<File> txtFiles = new ArrayList<>();
        collectTxtFiles(characterDir, txtFiles);

        int chunkCounter = 0;
        for (File file : txtFiles) {
            try {
                String content = readFile(file);
                String relativePath = getRelativePath(characterDir, file);
                List<Chunk> fileChunks = splitContent(content, relativePath, chunkCounter);
                // Every web fragment retains title/source/revision even far from the file header.
                if (relativePath.startsWith("sources/web/") || relativePath.startsWith("analysis/imported/")) {
                    int end = content.indexOf("\n\n");
                    String header = end > 0 ? content.substring(0, Math.min(end, 1200)) : "";
                    if (!header.isEmpty()) {
                        List<Chunk> attributed = new ArrayList<>();
                        for (Chunk chunk : fileChunks) {
                            attributed.add(new Chunk(chunk.id, chunk.sourceFile,
                                    chunk.text.startsWith(header) ? chunk.text : header + "\n\n" + chunk.text));
                        }
                        fileChunks = attributed;
                    }
                }
                allChunks.addAll(fileChunks);
                chunkCounter += fileChunks.size();
            } catch (IOException e) {
                System.err.println("读取文件失败: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }

        return allChunks;
    }

    /**
     * 将文本内容按段落分块。
     */
    private List<Chunk> splitContent(String text, String sourceFile, int startId) {
        List<Chunk> chunks = new ArrayList<>();

        // 先按双换行（段落）分割
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        int id = startId;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            // 如果段落本身就很长，先按句号/换行细分
            if (trimmed.length() > MAX_CHUNK_SIZE) {
                // 先保存当前 chunk
                if (currentChunk.length() >= MIN_CHUNK_SIZE) {
                    chunks.add(new Chunk("chunk_" + id, sourceFile, currentChunk.toString().trim()));
                    id++;
                    currentChunk = new StringBuilder();
                }
                // 长段落按句子拆分
                List<String> subParts = splitLongParagraph(trimmed);
                for (String sub : subParts) {
                    chunks.add(new Chunk("chunk_" + id, sourceFile, sub));
                    id++;
                }
                continue;
            }

            // 如果加入当前段落会超出最大长度，先保存当前 chunk
            if (currentChunk.length() + trimmed.length() > MAX_CHUNK_SIZE
                    && currentChunk.length() >= MIN_CHUNK_SIZE) {
                chunks.add(new Chunk("chunk_" + id, sourceFile, currentChunk.toString().trim()));
                id++;
                currentChunk = new StringBuilder();
            }

            // 加入当前段落
            if (currentChunk.length() > 0) {
                currentChunk.append("\n");
            }
            currentChunk.append(trimmed);
        }

        // 保存最后一个 chunk
        if (currentChunk.length() >= MIN_CHUNK_SIZE / 2) {
            chunks.add(new Chunk("chunk_" + id, sourceFile, currentChunk.toString().trim()));
        }

        return chunks;
    }

    /**
     * 将长段落按句号、换行等语义边界拆分成子句子。
     */
    private List<String> splitLongParagraph(String longText) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < longText.length(); i++) {
            char c = longText.charAt(i);
            sb.append(c);

            // 断句标记
            boolean isBreakPoint = (c == '。' || c == '！' || c == '？' || c == '\n'
                    || c == '…' || c == '；' || c == '，')
                    && sb.length() >= MIN_CHUNK_SIZE;

            if (isBreakPoint && sb.length() >= MIN_CHUNK_SIZE) {
                parts.add(sb.toString().trim());
                sb = new StringBuilder();
            }

            // 强制断句
            if (sb.length() >= MAX_CHUNK_SIZE) {
                parts.add(sb.toString().trim());
                sb = new StringBuilder();
            }
        }

        if (sb.length() > 0) {
            parts.add(sb.toString().trim());
        }

        return parts;
    }

    private void collectTxtFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        // 排除 rag_index.json 等非文本文件
        for (File file : files) {
            if (file.isDirectory()) {
                if ("memory".equalsIgnoreCase(file.getName())) continue;
                collectTxtFiles(file, result);
            } else if (file.getName().toLowerCase().endsWith(".txt")) {
                result.add(file);
            }
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String getRelativePath(File base, File file) {
        String basePath = base.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1).replace('\\', '/');
        }
        return file.getName();
    }
}
