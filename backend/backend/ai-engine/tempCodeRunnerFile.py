import os
# 🔥 পুরোনো মডেল সাপোর্ট করার জন্য এই লাইনটি সবার আগে থাকতে হবে
os.environ["TF_USE_LEGACY_KERAS"] = "1"

from flask import Flask, request, jsonify
import tensorflow as tf
from transformers import AutoTokenizer, TFDistilBertModel # মডেল ক্লাস ইমপোর্ট
import numpy as np

app = Flask(__name__)

# --- CONFIGURATION ---
MODEL_PATH = "model.keras"
# আপনার মডেল ৪টি ক্লাস প্রেডিক্ট করে। সেগুলোর নাম এখানে দিন।
# সাধারণত: 0=Safe, 1=Phishing, 2=Malware, 3=Spam (এটি আপনার মডেল ট্রেনিং এর ওপর নির্ভর করে)
# আপাতত আমরা জেনেরিক নাম দিচ্ছি, আপনি রান করে কনসোলে চেক করবেন কোনটা কি।
LABELS = ["Safe", "Phishing/Scam", "Suspicious", "Malware"] 

print("⏳ Loading AI Model... (This might take a while)")

model = None
tokenizer = None

try:
    # ১. টোকেনাইজার লোড
    tokenizer = AutoTokenizer.from_pretrained("./")

    # ২. মডেল লোড (Custom Object সহ)
    # আপনার মডেলের ভেতরে 'TFDistilBertModel' লেয়ার আছে, তাই এটা বলে দিতে হয়
    model = tf.keras.models.load_model(
        MODEL_PATH, 
        custom_objects={"TFDistilBertModel": TFDistilBertModel}
    )
    
    print("✅ AI Model Loaded Successfully!")

except Exception as e:
    print(f"❌ Error loading model: {e}")

@app.route('/predict', methods=['POST'])
def predict():
    if model is None:
        return jsonify({'error': 'Model failed to load. Check server logs.'}), 500

    try:
        data = request.json
        message = data.get('message', '')

        if not message:
            return jsonify({'error': 'No message provided'}), 400

        # ৩. ইনপুট প্রসেসিং
        inputs = tokenizer(
            message, 
            return_tensors="tf", 
            padding="max_length", 
            truncation=True, 
            max_length=128
        )

        # ৪. প্রেডিকশন
        # ইনপুট ডিকশনারি আকারে পাস করতে হবে
        predictions = model.predict({
            'input_ids': inputs['input_ids'],
            'attention_mask': inputs['attention_mask']
        })

        # ৫. রেজাল্ট প্রসেসিং (Logits থেকে)
        logits = predictions['logits'] if 'logits' in predictions else predictions
        
        # Softmax অ্যাপ্লাই করে প্রবাবিলিটি বের করা
        probs = tf.nn.softmax(logits, axis=-1).numpy()[0]
        
        # সবচেয়ে বেশি স্কোর যার, সেই ক্লাসটি নেওয়া
        predicted_class_id = np.argmax(probs)
        confidence = float(probs[predicted_class_id])
        result_label = LABELS[predicted_class_id] if predicted_class_id < len(LABELS) else "Unknown"

        # স্ক্যাম ডিটেকশন লজিক (Safe বাদে বাকি সব স্ক্যাম)
        final_verdict = "Safe" if predicted_class_id == 0 else "SCAM"
        if predicted_class_id == 2: final_verdict = "SUSPICIOUS"

        print(f"🔍 Input: '{message[:20]}...' | Class: {predicted_class_id} ({result_label}) | Conf: {confidence:.2f}")

        return jsonify({
            'result': final_verdict,
            'details': result_label,
            'confidence': f"{confidence * 100:.2f}%"
        })

    except Exception as e:
        print(f"Prediction Error: {e}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    # পোর্ট 5000 এ রান হবে
    app.run(port=5000)