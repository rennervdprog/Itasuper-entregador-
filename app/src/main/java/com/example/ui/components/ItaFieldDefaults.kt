package com.example.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.example.ui.theme.ItaFieldBackground
import com.example.ui.theme.ItaFieldBorder
import com.example.ui.theme.ItaFieldDisabledBackground
import com.example.ui.theme.ItaFieldDisabledText
import com.example.ui.theme.ItaFieldIcon
import com.example.ui.theme.ItaFieldPlaceholder
import com.example.ui.theme.ItaFieldText
import com.example.ui.theme.ItaOrange

/** Padrão único de contraste para campos de texto e seleção do entregador. */
@Composable
fun itaOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ItaFieldText,
    unfocusedTextColor = ItaFieldText,
    disabledTextColor = ItaFieldDisabledText,
    focusedContainerColor = ItaFieldBackground,
    unfocusedContainerColor = ItaFieldBackground,
    disabledContainerColor = ItaFieldDisabledBackground,
    cursorColor = ItaOrange,
    focusedBorderColor = ItaOrange,
    unfocusedBorderColor = ItaFieldBorder,
    disabledBorderColor = ItaFieldBorder,
    focusedLabelColor = ItaOrange,
    unfocusedLabelColor = ItaFieldIcon,
    disabledLabelColor = ItaFieldDisabledText,
    focusedPlaceholderColor = ItaFieldPlaceholder,
    unfocusedPlaceholderColor = ItaFieldPlaceholder,
    disabledPlaceholderColor = ItaFieldDisabledText,
    focusedLeadingIconColor = ItaFieldIcon,
    unfocusedLeadingIconColor = ItaFieldIcon,
    disabledLeadingIconColor = ItaFieldDisabledText,
    focusedTrailingIconColor = ItaFieldIcon,
    unfocusedTrailingIconColor = ItaFieldIcon,
    disabledTrailingIconColor = ItaFieldDisabledText
)
