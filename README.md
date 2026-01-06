📢 Team Instructions for CyberAware Project:

সবাই নিচের ধাপগুলো ফলো করে প্রজেক্ট সেটআপ করো:

১. প্রজেক্ট নামানো: প্রথমে গিটহাব থেকে প্রজেক্ট ক্লোন করো:

create a project folder -> go to project folder -> right click and open with terminal -> copy and paste this line below-

git clone https://github.com/mahmudul20bd/Cyber-Aware.git

২. নিজের ব্রাঞ্চে যাওয়া: ফোল্ডারে ঢুকে চেক করো তুমি কোন ব্রাঞ্চে আছো। এরপর নিজের ব্রাঞ্চে সুইচ করো: git checkout তোমার_নাম (যেমন: git checkout Sharif)

৩. ডাটাবেস সেটআপ: প্রজেক্টের ফোল্ডারে দেখো একটা .sql ফাইল আছে। MySQL-এ cyberaware_db নামে একটা ডাটাবেস বানিয়ে সেখানে ওই ফাইলটা Import করো।

৪. সার্ভার কনফিগারেশন ও রান করা (IntelliJ IDEA): ⚠️ গুরুত্বপূর্ণ

IntelliJ IDEA তে প্রজেক্টের backend ফোল্ডারটি ওপেন করো।

backend\backend\src\main\java\com\cyberaware\backend\BackendApplication.java ফাইলে যাও।

এবার BackendApplication ফাইলটি বের করে Run বাটন চাপো। কনসোলে "Completed initialization in 1 ms" লেখা আসলে বুঝবে সার্ভার চলছে।

৪. আইপি অ্যাড্রেস সেটআপ: তোমার পিসির আইপি বের করো (cmd -> ipconfig)।

Go to Android Studio -> File -> Open AndroidApp folder -> go

app\src\main\java\com\cyberaware\app

 Constants.java ফাইলে BASE_URL এ তোমার আইপি বসাও। 

৫. কাজ শেষে পুশ করা: কাজ শেষ হলে মেইন ব্রাঞ্চে পুশ করবে না! শুধু নিজের ব্রাঞ্চে পুশ করবে:
 
 go to vs code -> open the Cyber Aware Folder -> open new terminal -> and then write these line

//Bash Command


git pull

git add .

git commit -m "ki kaj korso tar short description"

git push origin তোমার_নাম