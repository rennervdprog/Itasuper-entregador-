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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

@Composable
fun PinInputBoxes(
    pin: String,
    onPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hidden text field capturing keyboard input
        BasicTextField(
            value = pin,
            onValueChange = { input ->
                if (input.length <= 4 && input.all { it.isDigit() }) {
                    onPinChange(input)
                    if (input.length == 4) {
                        onConfirm()
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .testTag("pin_hidden_input")
        )

        // 4 PIN visual boxes (Geometric aspect-square style)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clickable { focusRequester.requestFocus() }
                .testTag("pin_boxes_container")
        ) {
            for (i in 0 until 4) {
                val digit = pin.getOrNull(i)?.toString() ?: ""
                val isCurrent = i == pin.length
                val isFilled = i < pin.length

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(ItaSurface, RoundedCornerShape(12.dp))
                        .border(
                            width = if (isCurrent) 2.dp else if (isFilled) 2.dp else 1.5.dp,
                            color = if (isCurrent) ItaOrange else if (isFilled) ItaSlate900 else ItaSlate300,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (digit.isNotEmpty()) digit else if (isCurrent) "•" else "",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSlate900,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

    }
}
