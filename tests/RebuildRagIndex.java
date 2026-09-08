package com.group_finity.mascot.chat;

import java.io.File;
import java.util.List;

/** Rebuilds the TF-IDF RAG index for a character directory without calling any AI API. */
public final class RebuildRagIndex {
    private RebuildRagIndex() {
    }

    public static void main(String[] args) throws Exception {
        File baseDir = new File(args.length > 0 ? args[0] : ".");
        File characterDir = new File(baseDir, "character");
        File indexFile = new File(characterDir, "rag_index.json");

        ChunkSplitter splitter = new ChunkSplitter();
        List<ChunkSplitter.Chunk> chunks = splitter.splitAll(characterDir);
        String hash = TFIDFIndex.computeFileHash(characterDir);

        TFIDFIndex index = new TFIDFIndex(indexFile);
        index.buildIndex(chunks, hash);
        index.saveToFile();

        System.out.println("RAG_REBUILT chunks=" + chunks.size() + " file=" + indexFile.getAbsolutePath());
    }
}
