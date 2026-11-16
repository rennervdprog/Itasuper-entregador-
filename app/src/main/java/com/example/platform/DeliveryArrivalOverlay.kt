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
 *
 * Aviso COMPACTO: a janela ocupa só a faixa inferior da tela e o fundo fora
 * do cartão é transparente — o mapa continua visível atrás do aviso.
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
        val slateMuted = Color.rgb(100, 116, 139)
        val lineGray = Color.rgb(226, 232, 240)
        val slatePillBg = Color.rgb(241, 245, 249)

        fun text(value: String, size: Float, color: Int, style: Int = Typeface.NORMAL) = TextView(appContext).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            setTypeface(typeface, style)
        }

        val close = TextView(appContext).apply {
            text = "×"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(ink)
            contentDescription = "Fechar alerta de chegada"
            setOnClickListener { dismiss() }
            background = bordered(surface, dp(appContext, 19), lineGray, dp(appContext, 1))
        }

        val mapNote = TextView(appContext).apply {
            text = "Seu mapa continua aberto atrás deste aviso"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(appContext, 12), 0, dp(appContext, 12), 0)
            background = bordered(surface, dp(appContext, 18), lineGray, dp(appContext, 1))
        }
        val mapNoteRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(mapNote, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36)))
        }

        val arrivalBadge = TextView(appContext).apply {
            text = "CHEGADA AO DESTINO"
            textSize = 11f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(appContext, 12), 0, dp(appContext, 12), 0)
            background = rounded(orange, dp(appContext, 16))
        }
        val badgeRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(arrivalBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 32)))
            addView(View(appContext), LinearLayout.LayoutParams(0, 1, 1f))
            addView(close, LinearLayout.LayoutParams(dp(appContext, 38), dp(appContext, 38)))
        }

        val details = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 18), dp(appContext, 18), dp(appContext, 18), dp(appContext, 18))
            background = rounded(surface, dp(appContext, 20))
            elevation = dp(appContext, 8).toFloat()
            addView(badgeRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(space(appContext, 12))
            addView(text(shortCode.ifBlank { "Entrega em andamento" }, 12f, slateMuted, Typeface.BOLD).apply {
                letterSpacing = 0.08f
            })
            addView(space(appContext, 8))
            addView(text("Você chegou próximo ao destino", 24f, ink, Typeface.BOLD))
            addView(space(appContext, 12))
            addView(text("DESTINO", 11f, slateMuted, Typeface.BOLD))
            addView(space(appContext, 4))
            addView(text(destination.ifBlank { "Destino da entrega" }, 22f, ink, Typeface.BOLD))
            addView(space(appContext, 8))
            addView(text("Confirme a chegada e volte ao ItaSuper para concluir a entrega com o PIN do cliente.", 14f, muted))
        }

        val keepNavigation = Button(appContext).apply {
            text = "Continuar no mapa"
            textSize = 15f
            isAllCaps = false
            setTextColor(ink)
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(slatePillBg, dp(appContext, 18))
            setOnClickListener { dismiss() }
        }
        val returnToApp = Button(appContext).apply {
            text = "Voltar ao ItaSuper"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(orange, dp(appContext, 18))
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
        details.addView(space(appContext, 16))
        details.addView(actions, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 58)))

        // Fundo transparente e raiz não-clicável: só o cartão ocupa a
        // atenção; o mapa atrás segue visível e tocável fora do cartão.
        val root = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 16), dp(appContext, 8), dp(appContext, 16), dp(appContext, 16))
            isClickable = false
            addView(mapNoteRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(space(appContext, 12))
            addView(details, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
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

    private fun bordered(color: Int, radius: Int, strokeColor: Int, strokeWidth: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(strokeWidth, strokeColor)
        }

    private fun space(context: Context, height: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(context, height))
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
