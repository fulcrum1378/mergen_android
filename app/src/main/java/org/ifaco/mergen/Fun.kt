package org.ifaco.mergen

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Fun {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var c: Context
        lateinit var sp: SharedPreferences
        lateinit var fRegular: Typeface
        lateinit var fBold: Typeface
        var dirLtr = true
        var dm = DisplayMetrics()


        fun init(that: AppCompatActivity, root: View) {
            c = that.applicationContext
            dm = that.resources.displayMetrics
            dirLtr = c.resources.getBoolean(R.bool.dirLtr)
            if (!dirLtr) root.layoutDirection = View.LAYOUT_DIRECTION_RTL
            sp =  that.getPreferences(Context.MODE_PRIVATE)

            // Fonts
            fRegular = fonts(Fonts.REGULAR)
            fBold = fonts(Fonts.BOLD)
        }

        fun color(res: Int) = ContextCompat.getColor(c, res)

        fun drawable(res: Int) = ContextCompat.getDrawable(c, res)

        fun cf(res: Int = R.color.CS) = PorterDuffColorFilter(color(res), PorterDuff.Mode.SRC_IN)

        fun fonts(which: Fonts): Typeface = Typeface.createFromAsset(c.assets, which.path)

        fun dp(px: Int = 0) = (dm.density * px.toFloat()).toInt()

        fun vis(v: View, b: Boolean = true) {
            v.visibility = if (b) View.VISIBLE else View.GONE
        }

        fun vish(v: View, b: Boolean = true) {
            v.visibility = if (b) View.VISIBLE else View.INVISIBLE
        }

        fun drown(v: View, bb: Boolean = false) {
            if (bb) vis(v)
            AnimatorSet().apply {
                duration = 192
                interpolator = LinearInterpolator()
                playTogether(
                    ObjectAnimator.ofFloat(v, "scaleX", if (bb) 1f else 0f),
                    ObjectAnimator.ofFloat(v, "scaleY", if (bb) 1f else 0f),
                    ObjectAnimator.ofFloat(v, "rotation", if (bb) 0f else -360f)
                )
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        if (!bb) vis(v, false)
                    }
                })
                start()
            }
        }

        fun whirl(v: View, oa: ObjectAnimator? = null): ObjectAnimator? {
            drown(v, oa == null)
            return if (oa != null) {
                oa.cancel()
                null
            } else ObjectAnimator.ofFloat(v, "rotation", 0f, 360f).apply {
                duration = 343
                interpolator = LinearInterpolator()
                repeatCount = Animation.INFINITE
                start()
            }
        }

        fun fade(v: View, bb: Boolean = true) {
            if (bb) vis(v)
            ObjectAnimator.ofFloat(v, "alpha", if (bb) 1f else 0f).apply {
                duration = 192
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        if (!bb) vis(v, false)
                    }
                })
                start()
            }
        }

        fun pieChart(percent: Int = 0, col: Int = color(R.color.CS)) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            gradientType = GradientDrawable.SWEEP_GRADIENT
            colors = intArrayOf(
                col, col, col, col, col, col, col, col, col, col, Color.TRANSPARENT
            )
            useLevel = true
            level = percent * 100
        }

        fun shake(dur: Long = 78L) {
            val v = c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(dur)
        }

        fun permGranted(perm: String) =
            ActivityCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED

        fun permResult(grantResults: IntArray) =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    enum class Fonts(val path: String) {
        REGULAR("trebuchet.ttf"),
        BOLD("trebuchet_bold.ttf")
    }
}
