package com.cyberaware.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class CyberBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint()
    private val particles = ArrayList<Particle>()
    private val starCount = 100 // পার্টিকেল সংখ্যা
    private val random = Random.Default

    init {
        // ইনিশিয়াল পার্টিকেল তৈরি
        for (i in 0 until starCount) {
            particles.add(createParticle())
        }
    }

    private fun createParticle(): Particle {
        return Particle(
            x = random.nextFloat() * width,
            y = random.nextFloat() * height,
            radius = random.nextFloat() * 4f + 1f,
            speed = random.nextFloat() * 2f + 0.5f,
            alpha = random.nextInt(100, 255)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ব্যাকগ্রাউন্ড কালার (Deep Cyber Blue/Black)
        canvas.drawColor(Color.parseColor("#050A14"))

        // পার্টিকেল ড্র করা
        for (p in particles) {
            paint.color = Color.parseColor("#4F46E5") // Indigo Neon
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, paint)

            // মুভমেন্ট লজিক (উপর থেকে নিচে নামবে - Matrix Style)
            p.y -= p.speed // নিচ থেকে উপরে উঠলে -=, উপর থেকে নিচে +=

            // যদি স্ক্রিনের বাইরে চলে যায়, আবার নিচে রিসেট হবে
            if (p.y < 0) {
                p.y = height.toFloat()
                p.x = random.nextFloat() * width
            }
        }

        // প্রতি ফ্রেমে রেন্ডার করার জন্য
        invalidate()
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var alpha: Int
    )
}