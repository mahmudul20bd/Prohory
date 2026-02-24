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
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

class SpamDetector private constructor(private val context: Context) {

    companion object {
        private const val MODEL_NAME = "cyberaware_model.tflite" // আপনার মডেল ফাইলের নাম
        private const val VOCAB_NAME = "vocab.txt"
        private const val MAX_LEN = 128 // মডেলের সিকোয়েন্স লেন্থ [Sequence Length]

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
    // ব্যাকগ্রাউন্ড প্রসেসিংয়ের জন্য এক্সিকিউটর
    private val executorService = Executors.newSingleThreadExecutor()
    // রেজাল্ট মেইন থ্রেডে পাঠানোর জন্য হ্যান্ডলার
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        executorService.execute {
            try {
                // ১. ভোকাবুলারি লোড
                vocab = loadVocab()
                Log.d("CyberAware_AI", "✅ Vocab Loaded. Size: ${vocab.size}")

                // ২. মডেল অপশন কনফিগারেশন
                val options = Interpreter.Options()

                // Flex Delegate যোগ করা (TFLite 2.16+ এ এটি গুরুত্বপূর্ণ)
                try {
                    val flexDelegate = FlexDelegate()
                    options.addDelegate(flexDelegate)
                    Log.d("CyberAware_AI", "✅ Flex Delegate Added Successfully")
                } catch (e: Exception) {
                    Log.e("CyberAware_AI", "⚠️ Flex Delegate Error (Check Dependencies): ${e.message}")
                }

                // ৩. মডেল লোড
                val modelFile = loadModelFile()
                interpreter = Interpreter(modelFile, options)
                isLoaded = true

                Log.d("CyberAware_AI", "🚀 AI Engine Ready & Loaded from filesDir")

                // ইনপুট শেপ চেক (ডিবাগিংয়ের জন্য)
                try {
                    val inputShape = interpreter?.getInputTensor(0)?.shape()
                    Log.d("CyberAware_AI", "🔎 Model Expects Input Shape: ${inputShape?.contentToString()}")
                } catch (e: Exception) {
                    Log.w("CyberAware_AI", "Could not read input tensor details")
                }

            } catch (e: Exception) {
                Log.e("CyberAware_AI", "❌ Critical Init Error: ${e.message}")
                e.printStackTrace()
                isLoaded = false
            }
        }
    }

    /**
     * টেক্সট ক্লাসিফাই করার ফাংশন
     * @param text যে টেক্সট চেক করতে হবে
     * @param callback ফলাফল পাওয়ার জন্য কলব্যাক (Label Index, Probability Array)
     */
    fun classify(text: String, callback: (Int, FloatArray) -> Unit) {
        if (!isLoaded || interpreter == null) {
            Log.e("CyberAware_AI", "❌ Engine not ready, re-initializing...")
            initializeInterpreter()
            // ফেইলসেফ রিটার্ন
            mainHandler.post { callback(-1, FloatArray(4)) }
            return
        }

        executorService.execute {
            try {
                // ১. ইনপুট প্রসেসিং (Tokenization + Padding)
                val inputData = prepareInput(text)

                // 🔥 CRITICAL FIX: মডেল [1, 128] শেপ চায়।
                // আমাদের কাছে আছে 1D IntArray [128]। এটাকে 2D Array তে কনভার্ট করতে হবে।

                val inputIds = Array(1) { inputData.first }       // Shape: [1, 128]
                val attentionMask = Array(1) { inputData.second } // Shape: [1, 128]

                // ২. ইনপুট অবজেক্ট তৈরি (Multiple Inputs: IDs + Mask)
                val inputs = arrayOf<Any>(inputIds, attentionMask)

                // ৩. আউটপুট বাফার তৈরি (Shape: [1, 4])
                // 0=Safe, 1=Promo, 2=Spam, 3=Danger
                val outputBuffer = Array(1) { FloatArray(4) }
                val outputs = mapOf(0 to outputBuffer)

                // ৪. ইনফারেন্স রান করা (Run Inference)
                interpreter?.runForMultipleInputsOutputs(inputs, outputs)

                // ৫. রেজাল্ট প্রসেসিং
                val probabilities = outputBuffer[0]

                // সবচেয়ে বেশি প্রোব্যাবিলিটি বের করা (ArgMax)
                var maxIndex = 0
                var maxProb = probabilities[0]
                for (i in 1 until probabilities.size) {
                    if (probabilities[i] > maxProb) {
                        maxProb = probabilities[i]
                        maxIndex = i
                    }
                }

                Log.d("CyberAware_AI", "✅ Result: Label=$maxIndex, Probs=${probabilities.contentToString()}")

                // ৬. মেইন থ্রেডে রেজাল্ট পাঠানো
                mainHandler.post {
                    callback(maxIndex, probabilities)
                }

            } catch (e: Exception) {
                Log.e("CyberAware_AI", "❌ Prediction Error: ${e.message}")
                e.printStackTrace()
                mainHandler.post {
                    callback(-1, FloatArray(4))
                }
            }
        }
    }

    // ==========================================
    // 🔥 PATH BUG FIX: Loading from filesDir instead of assets
    // ==========================================

    private fun loadModelFile(): MappedByteBuffer {
        val file = File(context.filesDir, MODEL_NAME)
        if (!file.exists()) {
            throw Exception("Model file not found in filesDir")
        }
        val fileInputStream = FileInputStream(file)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    }

    private fun loadVocab(): Map<String, Int> {
        val vocabMap = HashMap<String, Int>()
        try {
            val file = File(context.filesDir, VOCAB_NAME)
            if (!file.exists()) {
                throw Exception("Vocab file not found in filesDir")
            }
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
            Log.e("CyberAware_AI", "Error loading vocab: ${e.message}")
        }
        return vocabMap
    }

    private fun prepareInput(text: String): Pair<IntArray, IntArray> {
        val cleanedText = text.lowercase().trim()
        // Basic whitespace tokenizer (আপনার vocab অনুযায়ী)
        val words = cleanedText.split(Regex("\\s+"))

        val inputIds = IntArray(MAX_LEN)
        val attentionMask = IntArray(MAX_LEN)

        val clsId = vocab["[CLS]"] ?: 101
        val sepId = vocab["[SEP]"] ?: 102
        val unkId = vocab["[UNK]"] ?: 100

        var index = 0

        // [CLS] Start Token
        inputIds[index] = clsId
        attentionMask[index] = 1
        index++

        for (word in words) {
            if (index >= MAX_LEN - 1) break // [SEP] এর জন্য জায়গা রাখা

            val id = vocab[word] ?: unkId
            inputIds[index] = id
            attentionMask[index] = 1
            index++
        }

        // [SEP] End Token
        if (index < MAX_LEN) {
            inputIds[index] = sepId
            attentionMask[index] = 1
        }

        // বাকি অংশ 0 দিয়ে প্যাডিং করা থাকবে (Java/Kotlin IntArray default is 0)
        return Pair(inputIds, attentionMask)
    }
}