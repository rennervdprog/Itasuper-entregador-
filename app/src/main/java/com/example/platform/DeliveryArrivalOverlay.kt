package com.example.platform

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Alerta assistido exibido ao chegar próximo ao destino. A janela não toma a
 * navegação de volta à força: o motoboy decide tocar em "Voltar ao ItaSuper".
 */
object DeliveryArrivalOverlay {
    private var windowManager: WindowManager? = null
    private var activeView: View? = null

    fun show(context: Context, shortCode: String, destination: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return false
        dismiss()

        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val orange = Color.rgb(255, 106, 0)
        val ink = Color.rgb(15, 23, 42)
        val surface = Color.WHITE
        val muted = Color.rgb(71, 85, 105)

        fun text(value: String, size: Float, color: Int, style: Int = Typeface.NORMAL) = TextView(appContext).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            setTypeface(typeface, style)
        }

        val close = TextView(appContext).apply {
            text = "×"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Fechar alerta de chegada"
            setOnClickListener { dismiss() }
            background = rounded(Color.argb(35, 255, 255, 255), dp(appContext, 22))
        }

        val header = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text("ITASUPER ENTREGADOR", 12f, Color.WHITE, Typeface.BOLD), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(close, LinearLayout.LayoutParams(dp(appContext, 44), dp(appContext, 44)))
        }

        val arrivalBadge = TextView(appContext).apply {
            text = "  CHEGADA AO DESTINO  "
            textSize = 12f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(orange)
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(surface, dp(appContext, 18))
        }

        val details = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 22), dp(appContext, 22), dp(appContext, 22), dp(appContext, 22))
            background = rounded(surface, dp(appContext, 24))
            elevation = dp(appContext, 8).toFloat()
            addView(text(shortCode.ifBlank { "Entrega em andamento" }, 13f, orange, Typeface.BOLD))
            addView(space(appContext, 14))
            addView(text("Você chegou próximo\nao destino", 25f, ink, Typeface.BOLD))
            addView(space(appContext, 12))
            addView(text("Confirme a chegada e volte ao ItaSuper para concluir a entrega com o PIN do cliente.", 15f, muted))
            addView(space(appContext, 18))
            addView(text("DESTINO", 11f, muted, Typeface.BOLD))
            addView(space(appContext, 4))
            addView(text(destination.ifBlank { "Destino da entrega" }, 15f, ink, Typeface.NORMAL))
        }

        val keepNavigation = Button(appContext).apply {
            text = "Continuar no mapa"
            textSize = 15f
            isAllCaps = false
            setTextColor(ink)
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(Color.rgb(241, 245, 249), dp(appContext, 16))
            setOnClickListener { dismiss() }
        }
        val returnToApp = Button(appContext).apply {
            text = "Voltar ao ItaSuper"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(orange, dp(appContext, 16))
            setOnClickListener {
                val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
                    ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (launch != null) appContext.startActivity(launch)
                dismiss()
            }
        }
        val actions = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER
            addView(keepNavigation, LinearLayout.LayoutParams(0, dp(appContext, 58), 1f).apply { marginEnd = dp(appContext, 10) })
            addView(returnToApp, LinearLayout.LayoutParams(0, dp(appContext, 58), 1.35f))
        }

        val root = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 24), dp(appContext, 26), dp(appContext, 24), dp(appContext, 28))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(ink, Color.rgb(31, 41, 55))
            )
            isClickable = true
            addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 44)))
            addView(space(appContext, 52))
            addView(arrivalBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36)))
            addView(space(appContext, 20))
            addView(details, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(View(appContext), LinearLayout.LayoutParams(1, 0, 1f))
            addView(actions, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 58)))
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            setTitle("ItaSuperDeliveryArrival")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        return runCatching {
            manager.addView(root, params)
            windowManager = manager
            activeView = root
        }.isSuccess
    }

    fun dismiss() {
        val manager = windowManager
        val view = activeView
        if (manager != null && view != null) runCatching { manager.removeViewImmediate(view) }
        activeView = null
        windowManager = null
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun space(context: Context, height: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(context, height))
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
