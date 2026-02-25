package com.cyberaware.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

class SpamDetector private constructor(private val context: Context) {

    companion object {
        private const val MODEL_NAME = "cyberaware_model.tflite" // আপনার নতুন মডেল
        private const val VOCAB_NAME = "vocab.txt"
        private const val MAX_LEN = 128

        @Volatile
        private var instance: SpamDetector? = null

        fun getInstance(context: Context): SpamDetector {
            return instance ?: synchronized(this) {
                instance ?: SpamDetector(context.applicationContext).also { instance = it }
            }
        }
    }

    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = HashMap()
    private var isLoaded = false

    private val executorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        executorService.execute { initializeInterpreterSync() }
    }

    private fun initializeInterpreterSync() {
        try {
            vocab = loadVocab()
            val options = Interpreter.Options()
            try {
                options.addDelegate(FlexDelegate())
            } catch (e: Exception) {
                Log.e("CyberAware_AI", "Flex Delegate Error: ${e.message}")
            }
            val modelFile = loadModelFile()
            interpreter = Interpreter(modelFile, options)
            isLoaded = true
            Log.d("CyberAware_AI", "🚀 Manual AI Engine Ready. Vocab Size: ${vocab.size}")
        } catch (e: Exception) {
            Log.e("CyberAware_AI", "❌ Critical Init Error: ${e.message}")
            isLoaded = false
        }
    }

    fun classify(text: String, callback: (Int, FloatArray) -> Unit) {
        executorService.execute {
            if (!isLoaded || interpreter == null) {
                initializeInterpreterSync()
            }

            if (!isLoaded || interpreter == null) {
                mainHandler.post { callback(-1, FloatArray(4)) }
                return@execute
            }

            try {
                // ১. Tokenization
                val (inputIds, attentionMask) = prepareInputWordPiece(text)

                // ২. ByteBuffer তৈরি (int32 format)
                val idsBuffer = ByteBuffer.allocateDirect(4 * MAX_LEN).order(ByteOrder.nativeOrder())
                val maskBuffer = ByteBuffer.allocateDirect(4 * MAX_LEN).order(ByteOrder.nativeOrder())

                for (i in 0 until MAX_LEN) {
                    idsBuffer.putInt(inputIds[i])
                    maskBuffer.putInt(attentionMask[i])
                }

                // 🔥 CRITICAL: Order Matter! ChatGPT-র ইনস্ট্রাকশন অনুযায়ী mask আগে, ids পরে।
                val inputs = arrayOf(maskBuffer, idsBuffer)

                // ৩. Output Buffer (Float32)
                val outputBuffer = Array(1) { FloatArray(4) }
                val outputs = mapOf(0 to outputBuffer)

                // ৪. Inference Run
                interpreter?.runForMultipleInputsOutputs(inputs, outputs)

                val rawLogits = outputBuffer[0]

                // ৫. Softmax Calculation
                var maxLogit = rawLogits[0]
                for (logit in rawLogits) {
                    if (logit > maxLogit) maxLogit = logit
                }

                var sumExp = 0.0f
                val expValues = FloatArray(rawLogits.size)
                for (i in rawLogits.indices) {
                    expValues[i] = Math.exp((rawLogits[i] - maxLogit).toDouble()).toFloat()
                    sumExp += expValues[i]
                }

                val probabilities = FloatArray(rawLogits.size)
                for (i in rawLogits.indices) {
                    probabilities[i] = expValues[i] / sumExp
                }

                var maxIndex = 0
                var maxProb = probabilities[0]
                for (i in 1 until probabilities.size) {
                    if (probabilities[i] > maxProb) {
                        maxProb = probabilities[i]
                        maxIndex = i
                    }
                }

                Log.d("CyberAware_AI", "Pure AI -> Safe: ${probabilities[0]*100}%, Promo: ${probabilities[1]*100}%, Spam: ${probabilities[2]*100}%, Danger: ${probabilities[3]*100}%")

                mainHandler.post { callback(maxIndex, probabilities) }

            } catch (e: Exception) {
                Log.e("CyberAware_AI", "❌ Prediction Error: ${e.message}")
                mainHandler.post { callback(-1, FloatArray(4)) }
            }
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val file = File(context.filesDir, MODEL_NAME)
        if (!file.exists()) throw Exception("Model file not found")
        val fileInputStream = FileInputStream(file)
        return fileInputStream.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    }

    // 🔥 FIX: `vocab.txt` এখন `filesDir` থেকে লোড হবে, `assets` থেকে নয় (কারণ আপনি লাইভ সার্ভার থেকে নামান)
    private fun loadVocab(): Map<String, Int> {
        val vocabMap = HashMap<String, Int>()
        try {
            val file = File(context.filesDir, VOCAB_NAME)
            if (!file.exists()) throw Exception("Vocab file not found in filesDir")
            val reader = BufferedReader(InputStreamReader(FileInputStream(file)))
            var index = 0
            reader.forEachLine { line ->
                if (line.isNotEmpty()) {
                    vocabMap[line.trim()] = index
                    index++
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CyberAware_AI", "Vocab Load Error: ${e.message}")
        }
        return vocabMap
    }

    // ==================================================
    // 🔥 100% HUGGINGFACE COMPATIBLE CASED WORDPIECE TOKENIZER 🔥
    // ==================================================
    private fun prepareInputWordPiece(text: String): Pair<IntArray, IntArray> {
        // Punctuation আলাদা করা কিন্তু lowercase না করা (কারণ Cased model)
        val regex = Regex("([\\p{Punct}])")
        val spacedText = text.replace(regex, " $1 ")
        val words = spacedText.trim().split("\\s+".toRegex())

        val inputIds = IntArray(MAX_LEN) { vocab["[PAD]"] ?: 0 } // Default PAD
        val attentionMask = IntArray(MAX_LEN) { 0 }

        val clsId = vocab["[CLS]"] ?: 101
        val sepId = vocab["[SEP]"] ?: 102
        val unkId = vocab["[UNK]"] ?: 100

        var index = 0
        inputIds[index] = clsId
        attentionMask[index] = 1
        index++

        for (word in words) {
            if (word.isEmpty()) continue
            if (index >= MAX_LEN - 1) break // [SEP] এর জন্য জায়গা রাখা

            if (vocab.containsKey(word)) {
                inputIds[index] = vocab[word]!!
                attentionMask[index] = 1
                index++
                continue
            }

            // WordPiece Greedy Algorithm
            var start = 0
            var isUnk = false
            val subTokens = mutableListOf<Int>()

            while (start < word.length) {
                var end = word.length
                var matchId: Int? = null

                while (start < end) {
                    var subStr = word.substring(start, end)
                    if (start > 0) subStr = "##$subStr"

                    if (vocab.containsKey(subStr)) {
                        matchId = vocab[subStr]
                        break
                    }
                    end--
                }

                if (matchId == null) {
                    isUnk = true
                    break
                } else {
                    subTokens.add(matchId)
                    start = end
                }
            }

            if (isUnk) {
                if (index < MAX_LEN - 1) {
                    inputIds[index] = unkId
                    attentionMask[index] = 1
                    index++
                }
            } else {
                for (tokenId in subTokens) {
                    if (index >= MAX_LEN - 1) break
                    inputIds[index] = tokenId
                    attentionMask[index] = 1
                    index++
                }
            }
        }

        if (index < MAX_LEN) {
            inputIds[index] = sepId
            attentionMask[index] = 1
        }

        return Pair(inputIds, attentionMask)
    }
}