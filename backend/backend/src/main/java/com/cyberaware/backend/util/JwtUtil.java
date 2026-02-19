package com.cyberaware.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    // খুব শক্তিশালী সিক্রেট কি (এটি প্রোডাকশনে এনভায়রনমেন্ট ভেরিয়েবলে রাখা উচিত)
    private static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // টোকেন জেনারেট করা
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // ১০ ঘণ্টা মেয়াদ
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // টোকেন থেকে ইউজারনেম বের করা
    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // টোকেন ভ্যালিড কিনা চেক করা
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}