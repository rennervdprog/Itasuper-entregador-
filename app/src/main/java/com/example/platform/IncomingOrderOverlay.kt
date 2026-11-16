package com.example.platform

import android.content.Context
import android.content.Intent
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
import com.example.MainActivity
import com.example.data.model.IncomingOrderAlert
import com.example.data.model.PaymentSummary
import com.example.data.model.isOnlinePaidMethod

/**
 * Chamada visual em tela inteira para uma entrega disponível.
 *
 * A sobreposição só AVISA: o botão "Ver entrega" abre o app já na
 * oferta, e o aceite acontece dentro do app. Aceitar daqui falhava com
 * o processo frio — o push acorda o processo para desenhar a
 * sobreposição, mas a sessão do entregador não fica pronta nesse
 * contexto e o servidor recusava o aceite (visto ao vivo nas 2.4.4 e
 * 2.4.5). Dentro do app, com a sessão carregada, o aceite funciona.
 * A IncomingOrderActivity continua existindo para o toque na
 * notificação comum, quando a sobreposição não é exibida.
 */
object IncomingOrderOverlay {
    private var windowManager: WindowManager? = null
    private var activeView: View? = null

    @Volatile
    private var shownOrderId: String? = null

    /** Pedido cuja sobreposição está na tela agora (ou null). */
    fun shownOrderId(): String? = shownOrderId

    /** Fecha a sobreposição se ela for deste pedido (oferta expirada). */
    fun dismissIfShowing(orderId: String) {
        if (shownOrderId == orderId) dismiss()
    }

    @Volatile
    private var lastDiagnostic: String = "Nenhum teste de painel executado."

    fun lastDiagnostic(): String = lastDiagnostic

    fun isAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun show(
        context: Context,
        orderId: String,
        storeName: String,
        neighborhood: String,
        shortCode: String,
        alert: IncomingOrderAlert? = null
    ): Boolean {
        if (orderId.isBlank()) {
            lastDiagnostic = "O pedido do painel está vazio."
            return false
        }
        if (!isAllowed(context)) {
            lastDiagnostic = "O Android não reconheceu a permissão ‘Exibir sobre outros apps’."
            return false
        }
        dismiss()

        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // Laranja da marca (ItaOrange #FF6A00) — o overlay usava #C2410C,
        // divergente do resto do app. Cores do tema, em modo claro.
        val orange = Color.rgb(255, 106, 0)
        val orangeDark = Color.rgb(230, 81, 0)
        val orangeLight = Color.rgb(255, 234, 217)
        val deepInk = Color.rgb(15, 23, 42)
        val slate = Color.rgb(51, 65, 85)
        val slateMuted = Color.rgb(100, 116, 139)
        val surface = Color.rgb(255, 255, 255)
        val backgroundColor = Color.rgb(250, 250, 250)
        val lineGray = Color.rgb(226, 232, 240)
        val slatePillBg = Color.rgb(226, 232, 240)
        val green = Color.rgb(16, 185, 129)
        val greenDark = Color.rgb(46, 125, 50)
        val greenLight = Color.rgb(209, 250, 229)

        fun label(text: String, size: Float, color: Int, style: Int = Typeface.NORMAL) =
            TextView(appContext).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setTypeface(typeface, style)
            }

        fun pill(text: String, textColor: Int, bgColor: Int) =
            TextView(appContext).apply {
                this.text = text
                textSize = 12f
                gravity = Gravity.CENTER
                letterSpacing = 0.06f
                setTextColor(textColor)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(appContext, 14), 0, dp(appContext, 14), 0)
                background = roundedBackground(bgColor, dp(appContext, 18))
            }

        fun dot(color: Int) = View(appContext).apply {
            background = ovalBackground(color)
        }

        fun hspace(width: Int) = View(appContext).apply {
            layoutParams = LinearLayout.LayoutParams(dp(appContext, width), 1)
        }

        val close = TextView(appContext).apply {
            text = "×"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(deepInk)
            contentDescription = "Fechar alerta"
            setOnClickListener { dismiss() }
            background = borderedBackground(surface, dp(appContext, 22), lineGray, dp(appContext, 1))
        }

        val topRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(
                label("ITASUPER ENTREGADOR", 12f, slateMuted, Typeface.BOLD),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(close, LinearLayout.LayoutParams(dp(appContext, 44), dp(appContext, 44)))
        }

        val pillsRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(
                pill("NOVA ENTREGA DISPONÍVEL", orangeDark, orangeLight),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36))
            )
            addView(hspace(8))
            addView(
                pill("Com som", slate, slatePillBg),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36))
            )
        }

        // Linha da rota como heroína: retirada → destino gigante.
        val pickupAddress = alert?.pickupAddress?.ifBlank { "Retirada na loja parceira" }
            ?: "Retirada na loja parceira"
        val destinationAddress = alert?.destinationAddress?.ifBlank { neighborhood.ifBlank { "Destino disponível" } }
            ?: neighborhood.ifBlank { "Destino disponível" }

        fun stopRow(dotColor: Int, tag: String, address: String, addressSize: Float, sub: String?): LinearLayout {
            val dotView = dot(dotColor)
            val texts = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(tag, 11f, slateMuted, Typeface.BOLD).apply { letterSpacing = 0.08f })
                addView(space(appContext, 4))
                addView(label(address, addressSize, deepInk, Typeface.BOLD))
                if (!sub.isNullOrBlank()) {
                    addView(space(appContext, 4))
                    addView(label(sub, 14f, slate))
                }
            }
            return LinearLayout(appContext).apply {
                addView(
                    dotView,
                    LinearLayout.LayoutParams(dp(appContext, 16), dp(appContext, 16)).apply {
                        topMargin = dp(appContext, 4)
                        marginEnd = dp(appContext, 12)
                    }
                )
                addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
        }

        val connector = View(appContext).apply {
            background = android.graphics.drawable.ColorDrawable(lineGray)
        }

        val routeTimeline = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                stopRow(
                    green,
                    "RETIRADA · ${storeName.ifBlank { "Loja ItaSuper" }}",
                    pickupAddress,
                    19f,
                    null
                )
            )
            addView(
                connector,
                LinearLayout.LayoutParams(dp(appContext, 3), dp(appContext, 28)).apply {
                    marginStart = dp(appContext, 6)
                    topMargin = dp(appContext, 4)
                    bottomMargin = dp(appContext, 4)
                }
            )
            addView(
                stopRow(
                    orange,
                    "DESTINO",
                    destinationAddress,
                    26f,
                    neighborhood.takeIf { it.isNotBlank() && it != destinationAddress }
                )
            )
        }

        // Pílulas de itens/pagamento. O rótulo do método reutiliza a
        // instrução operacional do pagamento (mesma regra da rota ativa);
        // o valor total já vem rotulado no alerta e é exibido verbatim,
        // sem verbo "Cobrar" inventado.
        val rawMethod = alert?.paymentMethod.orEmpty()
        val methodLabel = if (rawMethod.isNotBlank()) {
            PaymentSummary(rawMethod, 0.0).driverInstruction.substringBefore(" — ")
        } else {
            ""
        }
        val isPaid = isOnlinePaidMethod(rawMethod)
        val totalLabel = alert?.totalLabel.orEmpty()

        val detailPills = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
        }
        val detailRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        var detailCount = 0
        alert?.itemCount?.takeIf { it > 0 }?.let { count ->
            detailRow.addView(
                pill("$count ${if (count == 1) "item" else "itens"}", slate, slatePillBg),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36))
            )
            detailCount++
        }
        if (methodLabel.isNotBlank()) {
            if (detailCount > 0) detailRow.addView(hspace(8))
            detailRow.addView(
                pill(
                    if (isPaid) "Já pago · $methodLabel" else methodLabel,
                    if (isPaid) greenDark else slate,
                    if (isPaid) greenLight else slatePillBg
                ),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36))
            )
            detailCount++
        }
        if (detailCount > 0) {
            detailPills.addView(detailRow)
        }
        if (totalLabel.isNotBlank()) {
            if (detailCount > 0) detailPills.addView(space(appContext, 8))
            val totalRow = LinearLayout(appContext).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            totalRow.addView(
                pill(totalLabel, Color.WHITE, orange),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36))
            )
            detailPills.addView(totalRow)
        }

        val orderCode = TextView(appContext).apply {
            text = shortCode.ifBlank { "NOVO PEDIDO" }
            textSize = 13f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(slateMuted)
            setTypeface(typeface, Typeface.BOLD)
        }

        val dismissButton = Button(appContext).apply {
            text = "Fechar aviso"
            textSize = 15f
            isAllCaps = false
            setTextColor(deepInk)
            setTypeface(typeface, Typeface.BOLD)
            background = borderedBackground(surface, dp(appContext, 18), lineGray, dp(appContext, 1))
            setOnClickListener { dismiss() }
        }
        val viewButton = Button(appContext).apply {
            text = "VER ENTREGA"
            textSize = 20f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(orange, dp(appContext, 18))
            setOnClickListener {
                dismiss()
                appContext.startActivity(
                    Intent(appContext, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_VIEW_ORDER_ID, orderId)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
        }

        val root = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 24), dp(appContext, 26), dp(appContext, 24), dp(appContext, 28))
            setBackgroundColor(backgroundColor)
            isClickable = true
            addView(topRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 44)))
            addView(space(appContext, 22))
            addView(pillsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 36)))
            addView(space(appContext, 22))
            addView(routeTimeline, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(space(appContext, 20))
            addView(detailPills, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(View(appContext), LinearLayout.LayoutParams(1, 0, 1f))
            addView(orderCode, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(space(appContext, 12))
            addView(viewButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 74)))
            addView(space(appContext, 8))
            addView(dismissButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 54)))
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            setTitle("ItaSuperIncomingOrderFullScreen")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            manager.addView(root, params)
            windowManager = manager
            activeView = root
            shownOrderId = orderId
            // Fase 2: a sobreposição não é mais muda — som e vibração do
            // alerta acompanham a tela cheia.
            DriverNotificationHelper.alertAttention(appContext, orderId)
            lastDiagnostic = "Painel visual em tela inteira exibido com sucesso."
            return true
        } catch (error: WindowManager.BadTokenException) {
            dismiss()
            lastDiagnostic = "Não foi possível exibir o aviso de nova entrega. Verifique a permissão de sobreposição nas configurações do aparelho."
            return false
        } catch (error: SecurityException) {
            dismiss()
            lastDiagnostic = "Permissão de sobreposição negada. Acesse Configurações > Aplicativos > ItaSuper Entregador e ative a permissão de exibir sobre outros apps."
            return false
        } catch (error: RuntimeException) {
            dismiss()
            lastDiagnostic = "Não foi possível exibir o aviso de nova entrega neste momento. Tente reabrir o aplicativo."
            return false
        }
    }

    fun dismiss() {
        val manager = windowManager
        val view = activeView
        if (manager != null && view != null) {
            runCatching { manager.removeViewImmediate(view) }
        }
        activeView = null
        windowManager = null
        shownOrderId = null
    }

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun ovalBackground(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun borderedBackground(color: Int, radius: Int, strokeColor: Int, strokeWidth: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(strokeWidth, strokeColor)
        }

    private fun space(context: Context, height: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(context, height))
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
