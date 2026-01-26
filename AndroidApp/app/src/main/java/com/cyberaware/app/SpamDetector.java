package com.cyberaware.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpamDetector {

    private static SpamDetector instance; // Singleton Instance
    private Interpreter tflite;
    private Map<String, Integer> vocab;
    private static final int MAX_LEN = 150;
    private String initError = null;
    private boolean isLoaded = false;

    // Private Constructor
    private SpamDetector(Context context) {
        try {
            vocab = loadVocab(context, "vocab.json");
            tflite = new Interpreter(loadModelFile(context, "cyberaware_model.tflite"));
            isLoaded = true;
            Log.d("SpamDetector", "Model Loaded Successfully");
        } catch (Exception e) {
            initError = e.getMessage();
            Log.e("SpamDetector", "Model Load Failed: " + e.getMessage());
        }
    }

    // Singleton Method (Call this instead of new SpamDetector)
    public static synchronized SpamDetector getInstance(Context context) {
        if (instance == null) {
            instance = new SpamDetector(context.getApplicationContext());
        }
        return instance;
    }

    public String classify(String text) {
        if (!isLoaded || initError != null) return "AI_LOADING_ERROR";
        if (text == null || text.isEmpty()) return "SAFE";

        String ruleResult = ruleCheck(text);
        if (ruleResult != null) return ruleResult;

        try {
            float[][] input = preprocess(text);
            float[][] output = new float[1][3];
            tflite.run(input, output);

            float safe = output[0][0];
            float scam = output[0][1];
            float suspicious = output[0][2];

            if (scam > safe && scam > suspicious) return "SCAM";
            if (suspicious > safe && suspicious > scam) return "SUSPICIOUS";
            return "SAFE";
        } catch (Exception e) {
            return "ERROR_RUN";
        }
    }

    private String ruleCheck(String text) {
        String t = text.toLowerCase();
        if (t.contains("bit.ly") || t.contains("tinyurl")) return "SUSPICIOUS";
        if (t.contains("otp") || t.contains("verify")) return "SCAM";
        return null;
    }

    private float[][] preprocess(String text) {
        // ... (তোমার আগের preprocess কোড একই থাকবে) ...
        // সংক্ষেপে: Normalizer এবং vocab mapping লজিক এখানে রাখো
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", " ");

        String[] words = normalized.trim().split("\\s+");
        List<Integer> tokens = new ArrayList<>();
        for (String w : words) {
            if (vocab.containsKey(w)) tokens.add(vocab.get(w));
        }
        float[] seq = new float[MAX_LEN];
        int start = Math.max(0, MAX_LEN - tokens.size());
        for (int i = 0; i < tokens.size() && i < MAX_LEN; i++) {
            seq[start + i] = tokens.get(i);
        }
        return new float[][]{seq};
    }

    private Map<String, Integer> loadVocab(Context ctx, String file) throws IOException {
        // ... (তোমার আগের loadVocab কোড একই থাকবে) ...
        Map<String, Integer> map = new HashMap<>();
        InputStream is = ctx.getAssets().open(file);
        android.util.JsonReader reader = new android.util.JsonReader(new java.io.InputStreamReader(is));
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (reader.peek() == android.util.JsonToken.NUMBER) {
                map.put(key, reader.nextInt());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        return map;
    }

    private MappedByteBuffer loadModelFile(Context ctx, String path) throws IOException {
        AssetFileDescriptor fd = ctx.getAssets().openFd(path);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }
}