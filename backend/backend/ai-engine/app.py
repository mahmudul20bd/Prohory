import os
os.environ["TF_USE_LEGACY_KERAS"] = "1"

from flask import Flask, request, jsonify
import tensorflow as tf
import numpy as np
from transformers import TFDistilBertModel, BertTokenizer
import requests

# ================= CONFIG =================
MODEL_WEIGHTS = "model_weights.h5"
VOCAB_PATH = "vocab.txt"
TOKENIZER_CONFIG = "tokenizer_config.json"

MAX_LEN = 128
LABELS = ["SAFE", "PROMO", "SPAM", "DANGER"]

# 🔥 ======== PASTE YOUR LINKS HERE ========
MODEL_URL = "https://drive.google.com/uc?export=download&id=1q1xQh_ur5ZTGMQK6H8yzYgjfjeMKwVLH"
VOCAB_URL = "https://drive.google.com/uc?export=download&id=1E02IYMuLY5vrAI61vuXErEF-k1gcQA4-"
TOKENIZER_URL = "https://drive.google.com/uc?export=download&id=1cWJj83KxSPkzyd8m-vmbRRIl15U6tfiT"


app = Flask(__name__)

# ================= DOWNLOAD SYSTEM =================
def download_file(url, filename):
    print(f"⬇️ Downloading {filename}...")
    response = requests.get(url, stream=True)
    response.raise_for_status()
    with open(filename, "wb") as f:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)
    print(f"✅ {filename} downloaded.")

def ensure_files():
    if not os.path.exists(MODEL_WEIGHTS):
        download_file(MODEL_URL, MODEL_WEIGHTS)

    if not os.path.exists(VOCAB_PATH):
        download_file(VOCAB_URL, VOCAB_PATH)

    if not os.path.exists(TOKENIZER_CONFIG):
        download_file(TOKENIZER_URL, TOKENIZER_CONFIG)

# Ensure required files exist
ensure_files()

# ================= TOKENIZER =================
try:
    tokenizer = BertTokenizer(
        vocab_file=VOCAB_PATH,
        do_lower_case=False
    )
    print("✅ Tokenizer loaded.")
except Exception as e:
    print(f"❌ Tokenizer Error: {e}")

# ================= MODEL BUILD =================
def build_model():
    input_ids = tf.keras.Input(shape=(MAX_LEN,), dtype=tf.int32, name="input_ids")
    attention_mask = tf.keras.Input(shape=(MAX_LEN,), dtype=tf.int32, name="attention_mask")

    bert = TFDistilBertModel.from_pretrained(
    "distilbert-base-multilingual-cased",
    cache_dir="./hf_cache"
)


    outputs = bert(input_ids=input_ids, attention_mask=attention_mask)

    x = outputs.last_hidden_state[:, 0, :]
    x = tf.keras.layers.Dropout(0.3)(x)
    x = tf.keras.layers.Dense(256, activation='relu')(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    logits = tf.keras.layers.Dense(4)(x)

    model = tf.keras.Model(
        inputs={"input_ids": input_ids, "attention_mask": attention_mask},
        outputs=logits
    )

    return model

print("⏳ Building model...")
model = build_model()

# Build graph once
_ = model({
    "input_ids": tf.zeros((1, MAX_LEN), dtype=tf.int32),
    "attention_mask": tf.zeros((1, MAX_LEN), dtype=tf.int32),
})

print("⏳ Loading weights...")
try:
    model.load_weights(MODEL_WEIGHTS, by_name=True, skip_mismatch=True)
    print("✅ Model weights loaded successfully!")
except Exception as e:
    print(f"❌ Weight loading failed: {e}")

# ================= API =================
@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json(force=True)
        text = data.get("message", "").strip()

        if not text:
            return jsonify({"error": "Empty message"}), 400

        inputs = tokenizer(
            text,
            max_length=MAX_LEN,
            padding="max_length",
            truncation=True,
            return_tensors="tf"
        )

        logits = model({
            "input_ids": inputs["input_ids"],
            "attention_mask": inputs["attention_mask"]
        })

        probs = tf.nn.softmax(logits, axis=-1).numpy()[0]
        idx = int(np.argmax(probs))
        confidence_score = float(probs[idx] * 100)
        predicted_label = LABELS[idx]

        return jsonify({
            "result": predicted_label,
            "confidence": f"{confidence_score:.2f}"
        })

    except Exception as e:
        print(f"❌ Prediction Error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/health")
def health():
    return {"status": "ok"}

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    print(f"🚀 AI Engine running on port {port}")
    app.run(host="0.0.0.0", port=port)
