package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun PinInputBoxes(
    pin: String,
    onPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val boxesFocusRequester = remember { FocusRequester() }
    // Trava de reentrada (auditoria B14): o 4º dígito dispara onConfirm e o
    // onDone do teclado dispara de novo — sem guarda, a confirmação podia
    // sair duplicada ou repetir durante o envio. Dispara 1x por PIN
    // completo; apagar um dígito rearma a trava. O teclado próprio usa a
    // mesma guarda, pelo mesmo caminho de entrada.
    var lastConfirmedInput by remember { mutableStateOf<String?>(null) }

    fun handlePinInput(input: String) {
        if (input.length <= 4 && input.all { it.isDigit() }) {
            onPinChange(input)
            if (input.length < 4) {
                lastConfirmedInput = null
            } else if (lastConfirmedInput != input) {
                lastConfirmedInput = input
                onConfirm()
            }
        }
    }

    fun eraseLast() {
        if (pin.isNotEmpty()) {
            lastConfirmedInput = null
            onPinChange(pin.dropLast(1))
        }
    }

    fun clearAll() {
        lastConfirmedInput = null
        onPinChange("")
    }

    // Foco automático na abertura: o conjunto de caixas recebe o foco sem
    // nenhum toque extra. O campo oculto NÃO recebe foco aqui de propósito:
    // ele convocaria o teclado do sistema por cima do teclado próprio da
    // tela. O teclado próprio já nasce usável; o campo oculto continua
    // disponível para teclado físico/externo ao tocar nas caixas.
    LaunchedEffect(Unit) {
        boxesFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hidden text field capturing keyboard input
        BasicTextField(
            value = pin,
            onValueChange = { input -> handlePinInput(input) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            keyboardActions = KeyboardActions(onDone = {
                if (pin.length == 4 && lastConfirmedInput != pin) {
                    lastConfirmedInput = pin
                    onConfirm()
                }
            }),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .testTag("pin_hidden_input")
        )

        // 4 PIN visual boxes (62x74 dp, usáveis com luva)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .focusRequester(boxesFocusRequester)
                .clickable { focusRequester.requestFocus() }
                .testTag("pin_boxes_container")
        ) {
            for (i in 0 until 4) {
                val digit = pin.getOrNull(i)?.toString() ?: ""
                val isCurrent = i == pin.length
                val isFilled = i < pin.length

                Box(
                    modifier = Modifier
                        .size(width = 62.dp, height = 74.dp)
                        .background(
                            if (isFilled) ItaOrangeLight else ItaSurface,
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = if (isCurrent || isFilled) 2.dp else 1.dp,
                            color = if (isCurrent || isFilled) ItaOrange else ItaSlate300,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (digit.isNotEmpty()) digit else if (isCurrent) "•" else "",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (digit.isNotEmpty()) ItaTextPrimary else ItaTextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Teclado numérico próprio: alvos de 62 dp, usável com luva.
        // Mesma entrada do campo oculto — o 4º dígito confirma sozinho.
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            ).forEach { rowDigits ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowDigits.forEach { digit ->
                        PinKey(
                            onClick = { handlePinInput(pin + digit) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pin_key_$digit")
                        ) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.titleLarge,
                                color = ItaTextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PinKey(
                    onClick = { clearAll() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_key_clear")
                ) {
                    Text(
                        text = "Limpar",
                        style = MaterialTheme.typography.labelLarge,
                        color = ItaTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                PinKey(
                    onClick = { handlePinInput(pin + "0") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_key_0")
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.titleLarge,
                        color = ItaTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
                PinKey(
                    onClick = { eraseLast() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_key_backspace")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Backspace,
                        contentDescription = "Apagar último dígito",
                        tint = ItaTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(62.dp)
            .background(ItaSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ItaBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
