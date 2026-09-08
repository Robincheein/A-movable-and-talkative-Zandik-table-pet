package com.group_finity.mascot.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * TF-IDF 向量索引 + 余弦相似度检索。
 * 纯 Java 实现，使用字符级 bigram+unigram 特征。
 * 索引保存时仅存储 chunk 文本（向量在加载时重建），文件大小可控。
 */
public class TFIDFIndex {
    private static final Logger log = Logger.getLogger(TFIDFIndex.class.getName());

    private List<ChunkSplitter.Chunk> chunks;
    private Map<String, Integer> vocabulary;
    private List<Map<Integer, Double>> tfidfVectors;
    private Map<Integer, Double> idfValues;
    private final File indexFile;
    private String lastFileHash;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public TFIDFIndex(File indexFile) {
        this.indexFile = indexFile;
    }

    public void buildIndex(List<ChunkSplitter.Chunk> chunks, String fileHash) {
        this.chunks = chunks;
        this.lastFileHash = fileHash;
        log.info("构建 TF-IDF 索引，共 " + chunks.size() + " 个 chunk...");
        buildVocabulary(chunks);
        computeIDF(chunks);
        tfidfVectors = new ArrayList<>();
        for (ChunkSplitter.Chunk chunk : chunks) {
            tfidfVectors.add(computeTFIDF(chunk.text));
        }
        log.info("TF-IDF 索引完成，词汇量: " + vocabulary.size());
    }

    private void buildVocabulary(List<ChunkSplitter.Chunk> chunks) {
        vocabulary = new LinkedHashMap<>();
        int idx = 0;
        for (ChunkSplitter.Chunk chunk : chunks) {
            for (String term : extractTerms(chunk.text)) {
                if (!vocabulary.containsKey(term)) {
                    vocabulary.put(term, idx++);
                }
            }
        }
    }

    private Set<String> extractTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        String cleaned = text.replaceAll("\\s+", "");
        for (int i = 0; i < cleaned.length() - 1; i++) {
            terms.add(cleaned.substring(i, i + 2));
        }
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isIdeographic(c) || Character.isLetter(c)) {
                terms.add(String.valueOf(c));
            }
        }
        return terms;
    }

    private void computeIDF(List<ChunkSplitter.Chunk> chunks) {
        idfValues = new HashMap<>();
        int N = chunks.size();
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (ChunkSplitter.Chunk chunk : chunks) {
            for (String term : extractTerms(chunk.text))
                documentFrequency.put(term, documentFrequency.getOrDefault(term, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : vocabulary.entrySet()) {
            String term = entry.getKey();
            int idx = entry.getValue();
            int df = documentFrequency.getOrDefault(term, 0);
            idfValues.put(idx, Math.log((1.0 + N) / (1.0 + df)) + 1.0);
        }
    }

    private Map<Integer, Double> computeTFIDF(String text) {
        Map<Integer, Double> vector = new HashMap<>();
        Set<String> terms = extractTerms(text);
        if (terms.isEmpty()) return vector;

        Map<String, Integer> termFreq = new HashMap<>();
        for (String term : terms) {
            termFreq.put(term, termFreq.getOrDefault(term, 0) + 1);
        }
        int maxFreq = 1;
        for (int v : termFreq.values()) maxFreq = Math.max(maxFreq, v);

        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            Integer idx = vocabulary.get(entry.getKey());
            if (idx == null) continue;
            Double idf = idfValues.get(idx);
            if (idf == null) continue;
            double tf = 0.5 + 0.5 * ((double) entry.getValue() / maxFreq);
            vector.put(idx, tf * idf);
        }
        return vector;
    }

    public List<SearchResult> search(String query, int topK, double minSimilarity) {
        return search(query, topK, minSimilarity, chunk -> true);
    }

    public List<SearchResult> search(String query, int topK, double minSimilarity,
                                    java.util.function.Predicate<ChunkSplitter.Chunk> filter) {
        if (tfidfVectors == null || chunks == null) return Collections.emptyList();
        Map<Integer, Double> queryVector = computeTFIDF(query);
        if (queryVector.isEmpty()) return Collections.emptyList();

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < tfidfVectors.size(); i++) {
            if (!filter.test(chunks.get(i))) continue;
            double sim = cosineSimilarity(queryVector, tfidfVectors.get(i));
            if (sim >= minSimilarity) {
                results.add(new SearchResult(chunks.get(i), sim));
            }
        }
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        List<SearchResult> selected = new ArrayList<>();
        Map<String, Integer> perFile = new HashMap<>();
        for (SearchResult result : results) {
            if (selected.size() >= Math.max(0, topK)) break;
            int count = perFile.getOrDefault(result.chunk.sourceFile, 0);
            if (count >= 2) continue;
            selected.add(result);
            perFile.put(result.chunk.sourceFile, count + 1);
        }
        return selected;
    }

    private double cosineSimilarity(Map<Integer, Double> v1, Map<Integer, Double> v2) {
        if (v1.isEmpty() || v2.isEmpty()) return 0.0;
        double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (Map.Entry<Integer, Double> e : v1.entrySet()) {
            double val1 = e.getValue();
            norm1 += val1 * val1;
            Double val2 = v2.get(e.getKey());
            if (val2 != null) dot += val1 * val2;
        }
        for (double val : v2.values()) norm2 += val * val;
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /** 保存：仅保存 chunk 文本（加载时重建向量，避免膨胀） */
    public void saveToFile() throws IOException {
        IndexData data = new IndexData();
        data.lastFileHash = this.lastFileHash;
        data.chunkTexts = new ArrayList<>();
        data.chunkSources = new ArrayList<>();
        for (ChunkSplitter.Chunk chunk : chunks) {
            data.chunkTexts.add(chunk.text);
            data.chunkSources.add(chunk.sourceFile);
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(indexFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }
        log.info("RAG 索引已保存: " + (indexFile.length() / 1024) + " KB, "
                + data.chunkTexts.size() + " chunks");
    }

    /** 加载：从 JSON 读取 chunks，重建 TF-IDF 向量 */
    public boolean loadFromFile() {
        if (!indexFile.exists()) return false;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(indexFile), StandardCharsets.UTF_8)) {
            IndexData data = gson.fromJson(reader, IndexData.class);
            this.lastFileHash = data.lastFileHash;

            this.chunks = new ArrayList<>();
            for (int i = 0; i < data.chunkTexts.size(); i++) {
                chunks.add(new ChunkSplitter.Chunk(
                        "chunk_" + i, data.chunkSources.get(i), data.chunkTexts.get(i)));
            }

            buildVocabulary(chunks);
            computeIDF(chunks);
            this.tfidfVectors = new ArrayList<>();
            for (ChunkSplitter.Chunk chunk : chunks) {
                tfidfVectors.add(computeTFIDF(chunk.text));
            }

            log.info("RAG 索引加载完成: " + chunks.size() + " chunks, "
                    + vocabulary.size() + " terms");
            return true;
        } catch (IOException e) {
            log.warning("RAG 索引加载失败: " + e.getMessage());
            return false;
        }
    }

    public static String computeFileHash(File characterDir) {
        List<File> files = new ArrayList<>();
        collectAllFiles(characterDir, files);
        files.removeIf(file -> !file.getName().toLowerCase().endsWith(".txt"));
        files.sort(Comparator.comparing(file ->
                characterDir.toPath().relativize(file.toPath()).toString()));

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (File file : files) {
                String relativePath = characterDir.toPath().relativize(file.toPath())
                        .toString().replace(File.separatorChar, '/');
                digest.update(relativePath.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                try (InputStream input = new FileInputStream(file)) {
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        digest.update(buffer, 0, read);
                    }
                }
                digest.update((byte) 0);
            }
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest()) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法计算角色资料指纹", e);
        }
    }

    private static void collectAllFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if ("memory".equalsIgnoreCase(file.getName())) continue;
                collectAllFiles(file, result);
            }
            else result.add(file);
        }
    }

    public String getLastFileHash() { return lastFileHash; }
    public boolean isIndexReady() { return chunks != null && !chunks.isEmpty(); }

    public static class SearchResult {
        public final ChunkSplitter.Chunk chunk;
        public final double similarity;
        public SearchResult(ChunkSplitter.Chunk chunk, double similarity) {
            this.chunk = chunk; this.similarity = similarity;
        }
    }

    /** JSON 序列化结构 — 只存文本，不存向量 */
    private static class IndexData {
        String lastFileHash;
        List<String> chunkTexts;
        List<String> chunkSources;
    }
}
