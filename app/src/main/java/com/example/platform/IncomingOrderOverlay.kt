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
import com.example.data.model.IncomingOrderAlert
import com.example.ui.orders.IncomingOrderActivity

/**
 * Chamada visual em tela inteira para uma entrega disponível.
 *
 * A sobreposição somente apresenta o alerta. O aceite continua autenticado e
 * revalidado na tela do pedido, sem alterar a regra de rota no servidor.
 */
object IncomingOrderOverlay {
    private var windowManager: WindowManager? = null
    private var activeView: View? = null

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
        val orange = Color.rgb(255, 106, 0)
        val deepInk = Color.rgb(15, 23, 42)
        val slate = Color.rgb(100, 116, 139)
        val surface = Color.rgb(255, 255, 255)
        val mist = Color.rgb(241, 245, 249)

        fun label(text: String, size: Float, color: Int, style: Int = Typeface.NORMAL) =
            TextView(appContext).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setTypeface(typeface, style)
            }

        val close = TextView(appContext).apply {
            text = "×"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Fechar alerta"
            setOnClickListener { dismiss() }
            background = roundedBackground(Color.argb(35, 255, 255, 255), dp(appContext, 22))
        }

        val topRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(
                label("ITASUPER ENTREGADOR", 12f, Color.WHITE, Typeface.BOLD),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(close, LinearLayout.LayoutParams(dp(appContext, 44), dp(appContext, 44)))
        }

        val incomingPill = TextView(appContext).apply {
            text = "  NOVA ENTREGA DISPONÍVEL  "
            textSize = 12f
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setTextColor(orange)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(Color.WHITE, dp(appContext, 18))
        }

        val headline = label("Uma nova rota está\nesperando por você", 30f, Color.WHITE, Typeface.BOLD).apply {
            setLineSpacing(dp(appContext, 4).toFloat(), 1f)
        }
        val supporting = label(
            "Você está livre para atender. Confira os dados\ne escolha como deseja seguir.",
            16f,
            Color.rgb(203, 213, 225)
        ).apply {
            setLineSpacing(dp(appContext, 3).toFloat(), 1f)
        }

        val accentLine = View(appContext).apply {
            background = roundedBackground(orange, dp(appContext, 2))
        }

        val orderCode = TextView(appContext).apply {
            text = shortCode.ifBlank { "NOVO PEDIDO" }
            textSize = 14f
            letterSpacing = 0.06f
            setTextColor(orange)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(Color.rgb(255, 237, 213), dp(appContext, 12))
            gravity = Gravity.CENTER
        }

        val storeTitle = label(storeName.ifBlank { "Loja ItaSuper" }, 23f, deepInk, Typeface.BOLD)
        val pickupLabel = label("RETIRADA", 11f, slate, Typeface.BOLD)
        val pickupValue = label(alert?.pickupAddress?.ifBlank { "Retirada na loja parceira" } ?: "Retirada na loja parceira", 15f, deepInk, Typeface.NORMAL)
        val destinationLabel = label("DESTINO", 11f, slate, Typeface.BOLD)
        val destinationValue = label(
            alert?.destinationAddress?.ifBlank { neighborhood.ifBlank { "Destino disponível" } }
                ?: neighborhood.ifBlank { "Destino disponível" },
            15f,
            deepInk,
            Typeface.NORMAL
        )
        val orderInfoLabel = label("PEDIDO", 11f, slate, Typeface.BOLD)
        val orderInfo = label(
            listOfNotNull(
                alert?.itemCount?.takeIf { it > 0 }?.let { "$it ${if (it == 1) "item" else "itens"}" },
                alert?.paymentMethod?.takeIf { it.isNotBlank() },
                alert?.totalLabel?.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "Detalhes disponíveis ao visualizar" },
            15f,
            deepInk,
            Typeface.NORMAL
        )

        fun divider() = View(appContext).apply {
            background = android.graphics.drawable.ColorDrawable(Color.rgb(226, 232, 240))
        }

        val details = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 22), dp(appContext, 22), dp(appContext, 22), dp(appContext, 22))
            background = roundedBackground(surface, dp(appContext, 28))
            elevation = dp(appContext, 8).toFloat()
            addView(orderCode, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 30)))
            addView(space(appContext, 14))
            addView(storeTitle)
            addView(space(appContext, 18))
            addView(pickupLabel)
            addView(space(appContext, 4))
            addView(pickupValue)
            addView(space(appContext, 16))
            addView(divider(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
            addView(space(appContext, 16))
            addView(destinationLabel)
            addView(space(appContext, 4))
            addView(destinationValue)
            addView(space(appContext, 16))
            addView(divider(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
            addView(space(appContext, 16))
            addView(orderInfoLabel)
            addView(space(appContext, 4))
            addView(orderInfo)
        }

        val dismissButton = Button(appContext).apply {
            text = "Fechar aviso"
            textSize = 15f
            isAllCaps = false
            setTextColor(deepInk)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(mist, dp(appContext, 16))
            setOnClickListener { dismiss() }
        }
        val viewOrderButton = Button(appContext).apply {
            text = "Ver entrega"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(orange, dp(appContext, 16))
            setOnClickListener {
                val intent = IncomingOrderActivity.intent(
                    context = appContext,
                    orderId = orderId,
                    storeName = storeName,
                    neighborhood = neighborhood,
                    shortCode = shortCode,
                    pickupAddress = alert?.pickupAddress.orEmpty(),
                    destinationAddress = alert?.destinationAddress.orEmpty(),
                    itemCount = alert?.itemCount ?: 0,
                    paymentMethod = alert?.paymentMethod.orEmpty(),
                    totalLabel = alert?.totalLabel.orEmpty()
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                appContext.startActivity(intent)
                dismiss()
            }
        }
        val actionRow = LinearLayout(appContext).apply {
            gravity = Gravity.CENTER
            addView(dismissButton, LinearLayout.LayoutParams(0, dp(appContext, 58), 0.88f).apply {
                marginEnd = dp(appContext, 10)
            })
            addView(viewOrderButton, LinearLayout.LayoutParams(0, dp(appContext, 58), 1.35f))
        }

        val root = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(appContext, 24), dp(appContext, 26), dp(appContext, 24), dp(appContext, 28))
            background = verticalBackground(deepInk, Color.rgb(31, 41, 55))
            isClickable = true
            addView(topRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 44)))
            addView(space(appContext, 38))
            addView(incomingPill, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(appContext, 36)))
            addView(space(appContext, 20))
            addView(headline)
            addView(space(appContext, 14))
            addView(supporting)
            addView(space(appContext, 24))
            addView(accentLine, LinearLayout.LayoutParams(dp(appContext, 58), dp(appContext, 4)))
            addView(space(appContext, 30))
            addView(details, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(View(appContext), LinearLayout.LayoutParams(1, 0, 1f))
            addView(actionRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 58)))
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
    }

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun verticalBackground(top: Int, bottom: Int) = android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
        intArrayOf(top, bottom)
    )

    private fun space(context: Context, height: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(context, height))
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
