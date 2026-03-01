package com.cyberaware.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ExternalScannerService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String PYTHON_AI_URL = "https://prohory-hybrid-model.onrender.com/api/v1/scan-ai";

    @Value("${google.safebrowsing.api.key:}")
    private String gsbApiKey;

    @Value("${virustotal.api.key:}")
    private String vtApiKey;

    @Value("${app.whois.api.key:}")
    private String whoisApiKey;

    @Value("${google.vision.api.key:}")
    private String visionApiKey;

    @Async("taskExecutor")
    public CompletableFuture<String> getAiDecision(String message) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_AI_URL, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("label")) {
                return CompletableFuture.completedFuture((String) response.getBody().get("label"));
            }
        } catch (Exception e) {
            System.err.println("❌ AI Service Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture("SAFE");
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> checkGoogleSafeBrowsing(String url) {
        if (gsbApiKey == null || gsbApiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            String gsbEndpoint = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + gsbApiKey;

            Map<String, Object> client = new HashMap<>();
            client.put("clientId", "cyberaware-app");
            client.put("clientVersion", "1.0");

            Map<String, Object> threatInfo = new HashMap<>();
            threatInfo.put("threatTypes", new String[]{"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"});
            threatInfo.put("platformTypes", new String[]{"ANY_PLATFORM"});
            threatInfo.put("threatEntryTypes", new String[]{"URL"});

            Map<String, String> urlEntry = new HashMap<>();
            urlEntry.put("url", url);
            threatInfo.put("threatEntries", new Object[]{urlEntry});

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("client", client);
            requestBody.put("threatInfo", threatInfo);

            ResponseEntity<Map> response = restTemplate.postForEntity(gsbEndpoint, requestBody, Map.class);

            boolean hasThreat = response.getBody() != null && response.getBody().containsKey("matches");
            return CompletableFuture.completedFuture(hasThreat);
        } catch (Exception e) {
            System.err.println("❌ GSB API Error: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> checkVirusTotal(String url) {
        if (vtApiKey == null || vtApiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            String encodedUrl = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String vtEndpoint = "https://www.virustotal.com/api/v3/urls/" + encodedUrl;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apikey", vtApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(vtEndpoint, org.springframework.http.HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
                Map<String, Object> stats = (Map<String, Object>) attributes.get("last_analysis_stats");

                int malicious = (int) stats.getOrDefault("malicious", 0);
                int suspicious = (int) stats.getOrDefault("suspicious", 0);

                if (malicious > 0 || suspicious > 1) {
                    System.out.println("🦠 VirusTotal detected threat for: " + url);
                    return CompletableFuture.completedFuture(true);
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            System.err.println("❌ VirusTotal API Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(false);
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> checkDeadLinkOrMaliciousFile(String url) {
        try {
            // URL থেকে প্যারামিটার বাদ দিয়ে শুধু Path-টুকু বের করা হচ্ছে (যেমন: /download/app.apk)
            java.net.URL parsedUrl = new java.net.URL(url);
            String path = parsedUrl.getPath().toLowerCase();

            boolean hasBadExtension = path.endsWith(".apk") || path.endsWith(".exe") || path.endsWith(".bat") || path.endsWith(".pdf");

            if (hasBadExtension) {
                try {
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(3000);
                    int responseCode = connection.getResponseCode();

                    if (responseCode >= 400) {
                        System.out.println("⚠️ Dead/Fake Malicious File Link Detected: " + url);
                    } else {
                        System.out.println("⚠️ LIVE Malicious File Link Detected: " + url);
                    }
                    // SMS এর মাধ্যমে সরাসরি .apk বা .exe দেওয়া মানেই ৯৯% ম্যালওয়্যার, তাই ফাইল থাকলেই TRUE
                    return CompletableFuture.completedFuture(true);
                } catch (Exception e) {
                    System.out.println("⚠️ Fake Domain with Malicious File Detected: " + url);
                    return CompletableFuture.completedFuture(true);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ File Check Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(false);
    }


    @Async("taskExecutor")
    public CompletableFuture<Integer> checkDomainRisk(String url) {
        int riskScore = 0;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost().toLowerCase();

            boolean isIpAddress = host.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

            if (isIpAddress) {
                riskScore += 1;
            } else {
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                String[] domainParts = host.split("\\.");
                if (domainParts.length >= 4) {
                    riskScore += 1;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Domain Analysis Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(riskScore);
    }

    public String expandUrl(String shortUrl) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(shortUrl).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000);
            connection.connect();
            String expandedUrl = connection.getHeaderField("Location");
            connection.disconnect();
            return (expandedUrl != null) ? expandedUrl : shortUrl;
        } catch (Exception e) {
            return shortUrl;
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Integer> calculateAdvancedDomainRisk(String url, String message) {
        int risk = 0;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost().toLowerCase();

            double entropy = getShannonEntropy(host);
            if (entropy > 3.8) risk += 5;

            String[] words = message.toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.length() > 4 && host.contains(word)) {
                    if (!host.startsWith(word + ".")) {
                        risk += 7;
                    }
                }
            }

            if (host.chars().filter(ch -> ch == '-').count() > 2) risk += 3;
            if (host.chars().filter(ch -> ch == '.').count() > 3) risk += 4;

        } catch (Exception e) {}
        return CompletableFuture.completedFuture(risk);
    }

    private double getShannonEntropy(String s) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : s.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);
        double entropy = 0;
        for (int count : freq.values()) {
            double p = (double) count / s.length();
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    // 🔥 aiLabel প্যারামিটারটি যুক্ত করা হলো
    public int checkBrandAndTldRisk(String url, String message, String aiLabel) {
        int risk = 0;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost().toLowerCase();
            String msg = message.toLowerCase();

            // ১. সস্তা TLD চেকিং (Always Dangerous)
            if (host.matches(".*\\.(xyz|top|pw|cc|link|zip|click|info|tk|gq|ml|ga|cf)$")) {
                risk += 5;
            }

            // ২. কিওয়ার্ড মিসম্যাচ (Always Dangerous)
            String[] sensitiveKeywords = {"bkash", "nagad", "rocket", "bank", "otp", "login", "verify", "secure", "update", "account", "dbbl"};
            for (String key : sensitiveKeywords) {
                if (msg.contains(key)) {
                    if (host.contains(key) && !host.startsWith(key + ".")) {
                        System.out.println("🚨 Suspicious Keyword Placement: " + key + " in " + host);
                        risk += 8;
                    }
                }
            }

            // ৩. র‍্যান্ডমনেস বা এনট্রপি চেক (Always Dangerous)
            if (host.length() > 25) {
                risk += 3;
            }

            // ৪. Punycode/Homograph Attack Detection (Always Dangerous)
            if (host.startsWith("xn--") || host.contains(".xn--")) {
                System.out.println("🚨 Punycode/Homograph Attack Detected! (Fake Domain)");
                risk += 15;
            }

            // 🔥 ৫. Trusted Cloud Storage Abuse (Context-Aware! AI এর ওপর নির্ভরশীল)
            String[] cloudProviders = {"drive.google.com", "dropbox.com", "onedrive.live.com", "mediafire.com", "mega.nz"};
            for (String cloud : cloudProviders) {
                if (host.equals(cloud) || host.endsWith("." + cloud)) {

                    // যদি AI মনে করে কথাগুলো সন্দেহজনক (SAFE নয়)
                    if (!aiLabel.equals("SAFE")) {
                        if (msg.contains("bank") || msg.contains("account") || msg.contains("statement") ||
                                msg.contains("pdf") || msg.contains("dbbl") || msg.contains("bkash") || msg.contains("update")) {
                            System.out.println("🚨 Cloud Storage Abuse Detected! (AI suspects Phishing + Drive Link)");
                            risk += 8;
                        }
                    } else {
                        // AI যদি SAFE বলে, তার মানে এটা কোনো বন্ধুর পাঠানো নরমাল লিংক
                        System.out.println("✅ Friendly Cloud Link Detected. AI says SAFE, bypassing cloud penalty.");
                    }
                }
            }

        } catch (Exception e) {}
        return risk;
    }

    public Map<String, Object> getOcrAndAiDecision(MultipartFile file) {
        try {
            // 🔥 Google Vision বাদ দিয়ে আমাদের ফ্রি Python API-তে ফিরে যাওয়া হলো
            String ocrUrl = "https://prohory-hybrid-model.onrender.com/api/v1/scan-ocr";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

            // ✅ ম্যাজিক ফিক্স: ByteArrayResource ব্যবহার করা হলো যাতে Python ঠিকমতো ফাইল সাইজ বুঝতে পারে
            org.springframework.core.io.ByteArrayResource fileAsResource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "scan_image.png";
                }
            };

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", fileAsResource);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(body, headers);

            // OCR স্ক্যানের জন্য ডেডিকেটেড RestTemplate
            org.springframework.web.client.RestTemplate ocrRestTemplate = new org.springframework.web.client.RestTemplate();

            org.springframework.http.ResponseEntity<Map> response = ocrRestTemplate.postForEntity(ocrUrl, requestEntity, Map.class);
            return response.getBody();

        } catch (Exception e) {
            System.err.println("❌ OCR API Error: " + e.getMessage());
            return null;
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Integer> checkDomainAgeRisk(String url) {
        int risk = 0;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost().toLowerCase();

            String[] parts = host.split("\\.");
            String rootDomain = host;
            if (parts.length >= 2) {
                rootDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
            }

            if (whoisApiKey == null || whoisApiKey.isEmpty()) {
                System.out.println("⚠️ WHOIS API Key not found. Skipping Domain Age Check.");
                return CompletableFuture.completedFuture(0);
            }

            String whoisUrl = "https://www.whoisxmlapi.com/whoisserver/WhoisService?apiKey="
                    + whoisApiKey + "&domainName=" + rootDomain + "&outputFormat=JSON";

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(whoisUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);

            java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            scanner.close();

            if (response.contains("\"createdDate\":")) {
                String createdDateStr = response.split("\"createdDate\":")[1].split(",")[0].replace("\"", "").trim();
                java.time.ZonedDateTime createdDate = java.time.ZonedDateTime.parse(createdDateStr);
                long daysOld = java.time.Duration.between(createdDate, java.time.ZonedDateTime.now()).toDays();

                System.out.println("🗓️ Domain: " + rootDomain + " is " + daysOld + " days old.");

                if (daysOld < 30) {
                    System.out.println("🚩 HIGH RISK: New Domain Detected!");
                    risk += 10;
                } else if (daysOld < 90) {
                    risk += 5;
                }
            }
        } catch (Exception e) {
            // API এরর হলে রিস্ক ০ রাখাটাই নিরাপদ
        }
        return CompletableFuture.completedFuture(risk);
    }
}