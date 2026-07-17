package com.turkcell.rencar.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Telefon girişinin Login ve Kayıt ekranlarınca paylaşılan parçaları.
 *
 * (Önceden ui/login/LoginScreen içinde private'dı; Kayıt ekranı da aynı "TR +90 + 10 hane"
 * kuralını ve görünümünü kullandığından kopyalamak yerine buraya taşındı — tek kaynak.)
 */

/** Ülke kodu kutusu: "TR  +90" — telefon alanının solunda sabit durur (yükseklik 56dp, radius 16dp). */
@Composable
fun CountryCodeBox() {
    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TR",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "+90",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Telefon girişini "5XX XXX XX XX" biçiminde gruplar (yalnızca görsel; state saf rakam tutar).
 * Boşluklar 3., 6. ve 8. rakamdan sonra eklenir.
 */
val PhoneVisualTransformation = VisualTransformation { text ->
    val digits = text.text.take(10)
    val out = buildString {
        digits.forEachIndexed { i, c ->
            append(c)
            if (i == 2 || i == 5 || i == 7) append(' ')
        }
    }
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var add = 0
            if (offset > 2) add++
            if (offset > 5) add++
            if (offset > 7) add++
            return offset + add
        }

        override fun transformedToOriginal(offset: Int): Int {
            var sub = 0
            if (offset > 3) sub++
            if (offset > 7) sub++
            if (offset > 10) sub++
            return (offset - sub).coerceIn(0, digits.length)
        }
    }
    TransformedText(AnnotatedString(out), mapping)
}
