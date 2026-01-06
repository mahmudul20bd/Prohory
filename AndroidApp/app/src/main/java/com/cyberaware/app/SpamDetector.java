package com.cyberaware.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Interpreter.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SpamDetector {

    private Interpreter tflite;
    private Map<String, Integer> vocab;
    private static final int MAX_LEN = 150;
    private String initializationError = null;

    public SpamDetector(Context context) {
        try {
            // ১. ভোকাবুলারি লোড
            vocab = loadVocab(context, "vocab.json"); // আপনার ফাইলের নাম

            // ২. মডেল লোড (Flex Delegate অপশন সহ)
            Interpreter.Options options = new Interpreter.Options();
            tflite = new Interpreter(loadModelFile(context, "cyberaware_model.tflite"), options);

        } catch (Exception e) {
            e.printStackTrace();
            initializationError = "Error Init: " + e.getMessage();
        }
    }

    public String classify(String text) {
        if (initializationError != null) return initializationError;
        if (tflite == null || vocab == null) return "Model Not Loaded";

        try {
            String ruleResult = checkRules(text);
            if (ruleResult != null) return ruleResult;

            Object[] inputs = { preprocess(text) };
            Map<Integer, Object> outputs = new HashMap<>();
            float[][] outputArray = new float[1][3];
            outputs.put(0, outputArray);

            tflite.runForMultipleInputsOutputs(inputs, outputs);

            float probSafe = outputArray[0][0];
            float probScam = outputArray[0][1];
            float probSus = outputArray[0][2];

            Log.d("CyberAwareAI", "AI Scores -> Safe: " + probSafe + " | Scam: " + probScam);

            if (probScam > probSafe && probScam > probSus) return "🚨 SCAM DETECTED! (AI)";
            if (probSus > probSafe && probSus > probScam) return "⚠️ SUSPICIOUS (AI)";

            return "✅ SAFE MESSAGE";

        } catch (Exception e) {
            e.printStackTrace();
            return "Run Error: " + e.getMessage();
        }
    }

    private float[][] preprocess(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        String cleanText = normalized.toLowerCase().replaceAll("[^a-zA-Z0-9\u0980-\u09FF]", " ");

        String[] words = cleanText.trim().split("\\s+");
        java.util.List<Integer> tokenList = new java.util.ArrayList<>();

        for (String word : words) {
            if (vocab.containsKey(word)) {
                tokenList.add(vocab.get(word));
            }
        }

        // Pre-Padding
        float[] sequence = new float[MAX_LEN];
        int dataLen = tokenList.size();
        int startPos = MAX_LEN - dataLen;
        if (startPos < 0) startPos = 0;

        for (int i = 0; i < dataLen && i < MAX_LEN; i++) {
            sequence[startPos + i] = (float) tokenList.get(i); // Float Cast
        }
        return new float[][]{sequence};
    }

    private String checkRules(String text) {
        String lowerText = text.toLowerCase();
        // Whitelist
        if (lowerText.contains("bkash.com") || lowerText.contains("gp.com")) return null;

        // Suspicious Links
        if (lowerText.contains("bit.ly") || lowerText.contains("cutt.ly") || lowerText.contains("tinyurl"))
            return "⚠️ SUSPICIOUS LINK DETECTED";

        return null;
    }

    // JSON Loader (Memory Efficient)
    private Map<String, Integer> loadVocab(Context context, String jsonFilename) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        InputStream is = context.getAssets().open(jsonFilename);
        android.util.JsonReader reader = new android.util.JsonReader(new java.io.InputStreamReader(is, "UTF-8"));
        reader.setLenient(true);
        try {
            parseJsonStream(reader, map);
        } finally {
            reader.close();
        }
        return map;
    }

    private void parseJsonStream(android.util.JsonReader reader, Map<String, Integer> map) throws IOException {
        try {
            if (reader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("word_index") || name.equals("vocab")) {
                        readWordMap(reader, map);
                    } else if (name.equals("config")) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            if (reader.nextName().equals("word_index")) readWordMap(reader, map);
                            else reader.skipValue();
                        }
                        reader.endObject();
                    } else if (name.equals("model_config")) { // HuggingFace format support
                        reader.skipValue();
                    } else {
                        // Flat format check
                        if (reader.peek() == android.util.JsonToken.NUMBER) {
                            map.put(name, reader.nextInt());
                        } else {
                            reader.skipValue();
                        }
                    }
                }
                reader.endObject();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void readWordMap(android.util.JsonReader reader, Map<String, Integer> map) throws IOException {
        if (reader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (reader.peek() == android.util.JsonToken.NUMBER) map.put(key, reader.nextInt());
                else reader.skipValue();
            }
            reader.endObject();
        } else {
            reader.skipValue();
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }
}