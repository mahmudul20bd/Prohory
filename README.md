---

# 📢 Team Instructions for CyberAware Project

সবাই নিচের ধাপগুলো ফলো করে প্রজেক্ট সেটআপ করো:

---

## ⚠️ গুরুত্বপূর্ণ (Clone করার আগে একবার করতেই হবে)

এই প্রজেক্টে বড় `.tflite` AI model Git LFS দিয়ে রাখা আছে।

Clone করার আগে এই কমান্ড রান করো:

```bash
git lfs install
```

## ১️. প্রজেক্ট নামানো:

Create a project folder →
Go to project folder →
Right click → Open with Terminal →
তারপর নিচের লাইনটি paste করো:

```bash
git clone https://github.com/mahmudul20bd/Cyber-Aware.git
cd Cyber-Aware
```

## ২️. নিজের ব্রাঞ্চে যাওয়া:

ফোল্ডারে ঢুকে চেক করো তুমি কোন ব্রাঞ্চে আছো:

```bash
git branch
```

এরপর নিজের ব্রাঞ্চে সুইচ করো:

```bash
git checkout তোমার_নাম
```

উদাহরণ:

```bash
git checkout Sharif
```

---

## ৩️. ডাটাবেস সেটআপ (⚠️ শুধু Local Development এর জন্য)

⚠️ Production এ Database Railway-তে hosted আছে।
Local এ backend run করতে চাইলে এই ধাপ করো।

১. MySQL open করো
২. `cyberaware_db` নামে একটি database বানাও
৩. Project folder এ থাকা `.sql` file import করো

---

## ৪️. সার্ভার কনফিগারেশন ও রান করা (IntelliJ IDEA) – Optional

⚠️ গুরুত্বপূর্ণ: Backend already Render-এ hosted আছে।
Local এ run করতে চাইলে নিচের ধাপগুলো ফলো করো।

১. IntelliJ IDEA তে প্রজেক্টের `backend` ফোল্ডারটি ওপেন করো
২. নিচের ফাইলে যাও:

```
backend/backend/src/main/java/com/cyberaware/backend/BackendApplication.java
```

৩. `BackendApplication.java` ফাইলটি Run করো

Console এ যদি দেখো:

```
Started BackendApplication
```

তাহলে বুঝবে সার্ভার চলছে।

---

## ৫️. ⚠️ আইপি অ্যাড্রেস সেটআপ (আর প্রয়োজন নেই)

আগের মতো PC IP বসানোর দরকার নেই।

Backend এখন Render-এ hosted।
Android App already deployed backend URL ব্যবহার করে।

তাই `Constants.java` এ IP পরিবর্তন করার প্রয়োজন নেই।

---

## ৬️. Android App রান করা

১. Android Studio open করো
২. File → Open → `AndroidApp` folder select করো
৩. Gradle sync হতে দাও
৪. Emulator বা Physical device এ Run করো

---

## ৭️. AI Model সংক্রান্ত গুরুত্বপূর্ণ তথ্য

* Android `.tflite` model Git LFS দিয়ে stored
* Clone করার আগে অবশ্যই `git lfs install` করতে হবে
* না করলে model 1KB pointer file হিসেবে আসবে

---

## ৮️. কাজ শেষে Push করার নিয়ম

⚠️ কখনোই সরাসরি `main` branch এ push করবে না।

VS Code এ:

Open CyberAware Folder → Open New Terminal → লিখো:

```bash
git pull
git add .
git commit -m "কি কাজ করেছো তার সংক্ষিপ্ত বর্ণনা"
git push origin তোমার_নাম
```

---

# 🧠 Project Architecture

```
Android App
     ↓
Render Backend (Spring Boot)
     ↓
Cloud AI Engine (Flask + TensorFlow)
     ↓
Railway MySQL Database
```
