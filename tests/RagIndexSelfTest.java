package com.group_finity.mascot.chat;

import java.io.File;

/** Verifies that the shipped RAG index matches the shipped character corpus. */
public final class RagIndexSelfTest {
    private RagIndexSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        File baseDir = new File(args.length > 0 ? args[0] : "dist");
        File characterDir = new File(baseDir, "character");
        TFIDFIndex index = new TFIDFIndex(new File(characterDir, "rag_index.json"));
        if (!index.loadFromFile()) {
            throw new IllegalStateException("Unable to load RAG index");
        }
        String currentHash = TFIDFIndex.computeFileHash(characterDir);
        if (!currentHash.equals(index.getLastFileHash())) {
            throw new IllegalStateException("RAG index hash is stale");
        }
        System.out.println("RAG_INDEX_OK");
    }
}
