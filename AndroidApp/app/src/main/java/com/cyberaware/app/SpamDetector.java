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

    private Interpreter tflite;
    private Map<String, Integer> vocab;
    private static final int MAX_LEN = 150;
    private String initError = null;

    public SpamDetector(Context context) {
        try {
            vocab = loadVocab(context, "vocab.json");
            tflite = new Interpreter(loadModelFile(context, "cyberaware_model.tflite"));
        } catch (Exception e) {
            initError = e.getMessage();
        }
    }

    public String classify(String text) {
        if (initError != null) return "ERROR_INIT";
        if (text == null || text.isEmpty()) return "SAFE";

        // 🔥 RULE ENGINE (HIGHEST PRIORITY)
        String ruleResult = ruleCheck(text);
        if (ruleResult != null) return ruleResult;

        try {
            float[][] input = preprocess(text);
            float[][] output = new float[1][3];

            tflite.run(input, output);

            float safe = output[0][0];
            float scam = output[0][1];
            float suspicious = output[0][2];

            Log.d("AI", "Safe=" + safe + " Scam=" + scam + " Susp=" + suspicious);

            if (scam > safe && scam > suspicious) return "SCAM";
            if (suspicious > safe && suspicious > scam) return "SUSPICIOUS";

            return "SAFE";

        } catch (Exception e) {
            return "ERROR_RUN";
        }
    }

    // ================= RULE ENGINE =================
    private String ruleCheck(String text) {
        String t = text.toLowerCase();

        if (t.contains("bit.ly") || t.contains("cutt.ly") || t.contains("tinyurl"))
            return "SUSPICIOUS";

        if (t.contains("otp") || t.contains("verify") || t.contains("account blocked"))
            return "SCAM";

        if (t.contains("bkash.com") || t.contains("gp.com"))
            return null;

        return null;
    }

    // ================= PREPROCESS =================
    private float[][] preprocess(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\u0980-\u09FF]", " ");

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

    // ================= LOADERS =================
    private Map<String, Integer> loadVocab(Context ctx, String file) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        InputStream is = ctx.getAssets().open(file);
        android.util.JsonReader reader =
                new android.util.JsonReader(new java.io.InputStreamReader(is));

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
        return channel.map(FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(), fd.getDeclaredLength());
    }
}
