package com.turkcell.rencar.ui.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.model.CardUi
import com.turkcell.rencar.data.model.WalletTransactionUi
import com.turkcell.rencar.data.model.WalletUi
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar
import java.util.Locale

// Bakiye kartı gradyanı — tema-bağımsız marka mavisi (tasarımdaki kart).
private val CardBlueTop = Color(0xFF2E7BF6)
private val CardBlueBottom = Color(0xFF1558C8)
private val RencarBlue = Color(0xFF1A6BF0)

// ── Stateful sarmalayıcı (§4.5): state'i toplar, intent'leri VM'e iletir ──
@Composable
fun WalletScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WalletScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

// ── Stateless gövde (§4.5): uiState + onIntent ile çizer ──
@Composable
private fun WalletScreen(
    uiState: WalletUiState,
    onIntent: (WalletIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            uiState.isLoading ->
                LoadingState()

            uiState.loadError != null && uiState.wallet == null ->
                ErrorState(message = uiState.loadError, onRetry = { onIntent(WalletIntent.Retry) })

            else ->
                Content(uiState = uiState, onIntent = onIntent)
        }
    }

    if (uiState.showTopup) {
        TopupDialog(uiState = uiState, onIntent = onIntent)
    }
    if (uiState.showAddCard) {
        AddCardDialog(uiState = uiState, onIntent = onIntent)
    }
    uiState.deleteCandidateCardId?.let { candidateId ->
        val card = uiState.cards.firstOrNull { it.id == candidateId }
        DeleteCardDialog(last4 = card?.last4.orEmpty(), onIntent = onIntent)
    }
}

@Composable
private fun Content(uiState: WalletUiState, onIntent: (WalletIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Cüzdan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(18.dp))

        BalanceCard(balance = uiState.balance, onTopup = { onIntent(WalletIntent.TopupClicked) })
        Spacer(Modifier.height(24.dp))

        SectionHeader(title = "Kayıtlı kartlar", actionLabel = "+ Ekle") {
            onIntent(WalletIntent.AddCardClicked)
        }
        Spacer(Modifier.height(12.dp))
        uiState.cardActionError?.let {
            NoticeRow(text = it, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }
        CardsSection(uiState = uiState, onIntent = onIntent)
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Son işlemler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        TransactionsSection(transactions = uiState.wallet?.transactions.orEmpty())
        Spacer(Modifier.height(24.dp))
    }
}

// ── Mavi bakiye kartı: "Rencar bakiyesi" + tutar + Bakiye Yükle ──
@Composable
private fun BalanceCard(balance: Double, onTopup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(CardBlueTop, CardBlueBottom)))
            .padding(22.dp),
    ) {
        Text(
            text = "Rencar bakiyesi",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatTl(balance),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.20f))
                .clickable(onClick = onTopup)
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = RencarIcons.Plus,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Bakiye Yükle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

// ── Bölüm başlığı + sağda metin aksiyonu ("+ Ekle") ──
@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RencarBlue,
            )
        }
    }
}

// ── Kayıtlı kartlar listesi (boşsa bilgilendirme kartı) ──
@Composable
private fun CardsSection(uiState: WalletUiState, onIntent: (WalletIntent) -> Unit) {
    if (uiState.cards.isEmpty()) {
        SectionCard {
            Text(
                text = "Kayıtlı kartınız yok.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Yukarıdaki “+ Ekle” ile yeni bir kart ekleyebilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.cards.forEach { card ->
                CardItem(
                    card = card,
                    isSettingDefault = card.id == uiState.settingDefaultCardId,
                    isDeleting = card.id == uiState.deletingCardId,
                    onSetDefault = { onIntent(WalletIntent.SetDefaultCard(card.id)) },
                    onDelete = { onIntent(WalletIntent.DeleteCardClicked(card.id)) },
                )
            }
        }
    }
}

// ── Kart öğesi: marka + •••• son4 + SKT; Varsayılan chip veya Varsayılan yap/Sil ──
@Composable
private fun CardItem(
    card: CardUi,
    isSettingDefault: Boolean,
    isDeleting: Boolean,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandBadge(card.brand)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "•••• ${card.last4}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Son kullanma ${card.expLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.isDefault) {
                DefaultChip()
            }
        }
        Spacer(Modifier.height(10.dp))
        Divider()
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!card.isDefault) {
                ActionText(
                    text = "Varsayılan yap",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    loading = isSettingDefault,
                    onClick = onSetDefault,
                )
                Spacer(Modifier.width(4.dp))
            }
            ActionText(
                text = "Sil",
                color = MaterialTheme.colorScheme.error,
                loading = isDeleting,
                onClick = onDelete,
            )
        }
    }
}

/** Kart altındaki metin aksiyonu ("Varsayılan yap" / "Sil"); yükleniyorsa spinner. */
@Composable
private fun ActionText(text: String, color: Color, loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

@Composable
private fun DefaultChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.rencar.success.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Varsayılan",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.rencar.success,
        )
    }
}

@Composable
private fun BrandBadge(brand: String) {
    val isMc = brand.equals("MASTERCARD", ignoreCase = true)
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (isMc) MastercardMark() else VisaMark()
    }
}

/** VISA — klasik mavi italik wordmark. */
@Composable
private fun VisaMark() {
    Text(
        text = "VISA",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        letterSpacing = 0.5.sp,
        color = Color(0xFF1A1F71),
    )
}

/** Mastercard — kırmızı + amber iç içe geçen iki halka; kesişim turuncu. */
@Composable
private fun MastercardMark() {
    Canvas(modifier = Modifier.size(width = 34.dp, height = 22.dp)) {
        val r = size.height / 2f
        val cy = size.height / 2f
        val cxLeft = size.width / 2f - r * 0.5f
        val cxRight = size.width / 2f + r * 0.5f
        drawCircle(color = Color(0xFFEB001B), radius = r, center = Offset(cxLeft, cy))
        drawCircle(color = Color(0xFFF79E1B), radius = r, center = Offset(cxRight, cy))
        // Kesişim (vesica) yaklaşık turuncu bir elipsle vurgulanır.
        val lensW = r * 0.95f
        val lensH = r * 1.7f
        drawOval(
            color = Color(0xFFFF5F00),
            topLeft = Offset(size.width / 2f - lensW / 2f, cy - lensH / 2f),
            size = Size(lensW, lensH),
        )
    }
}

// ── Son işlemler ──
@Composable
private fun TransactionsSection(transactions: List<WalletTransactionUi>) {
    if (transactions.isEmpty()) {
        SectionCard {
            Text(
                text = "Henüz işlem yok.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Bakiye yükleme ve yolculuk ödemeleriniz burada görünür.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            transactions.forEach { TransactionRow(it) }
        }
    }
}

@Composable
private fun TransactionRow(txn: WalletTransactionUi) {
    val success = MaterialTheme.rencar.success
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (txn.isCredit) success else MaterialTheme.colorScheme.error
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (txn.isCredit) RencarIcons.Plus else RencarIcons.Car,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = txn.dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = txn.amountLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (txn.isCredit) success else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Bakiye Yükle pop-up (Dialog): tutar ──
@Composable
private fun TopupDialog(uiState: WalletUiState, onIntent: (WalletIntent) -> Unit) {
    Dialog(
        onDismissRequest = { onIntent(WalletIntent.DismissTopup) },
        properties = DialogProperties(dismissOnClickOutside = !uiState.isToppingUp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
        ) {
            DialogHeader(icon = RencarIcons.Wallet, title = "Bakiye Yükle")
            Spacer(Modifier.height(18.dp))

            DialogField(
                value = uiState.topupAmount,
                onValueChange = { onIntent(WalletIntent.TopupAmountChanged(it)) },
                label = "Tutar (₺)",
                placeholder = "200",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            NoticeRow(
                text = "Tek seferde 10 – 5.000 ₺ arası yükleyebilirsiniz.",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.topupError?.let {
                Spacer(Modifier.height(10.dp))
                NoticeRow(text = it, tint = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            DialogButtons(
                onCancel = { onIntent(WalletIntent.DismissTopup) },
                onConfirm = { onIntent(WalletIntent.SubmitTopup) },
                confirmLabel = "Yükle",
                enabled = uiState.canSubmitTopup,
                loading = uiState.isToppingUp,
            )
        }
    }
}

// ── Kart Ekle pop-up (Dialog): marka + son 4 hane + SKT ──
@Composable
private fun AddCardDialog(uiState: WalletUiState, onIntent: (WalletIntent) -> Unit) {
    Dialog(
        onDismissRequest = { onIntent(WalletIntent.DismissAddCard) },
        properties = DialogProperties(dismissOnClickOutside = !uiState.isAddingCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
        ) {
            DialogHeader(icon = RencarIcons.CreditCard, title = "Kart Ekle")
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandChip(
                    label = "VISA",
                    selected = uiState.addCardBrand == WalletCardBrand.VISA,
                    onClick = { onIntent(WalletIntent.AddCardBrandChanged(WalletCardBrand.VISA)) },
                    modifier = Modifier.weight(1f),
                )
                BrandChip(
                    label = "Mastercard",
                    selected = uiState.addCardBrand == WalletCardBrand.MASTERCARD,
                    onClick = { onIntent(WalletIntent.AddCardBrandChanged(WalletCardBrand.MASTERCARD)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))

            DialogField(
                value = uiState.addCardLast4,
                onValueChange = { onIntent(WalletIntent.AddCardLast4Changed(it)) },
                label = "Son 4 hane",
                placeholder = "1234",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogField(
                    value = uiState.addCardMonth,
                    onValueChange = { onIntent(WalletIntent.AddCardMonthChanged(it)) },
                    label = "SKT Ay",
                    placeholder = "1-12",
                    modifier = Modifier.weight(1f),
                )
                DialogField(
                    value = uiState.addCardYear,
                    onValueChange = { onIntent(WalletIntent.AddCardYearChanged(it)) },
                    label = "SKT Yıl",
                    placeholder = "2028",
                    modifier = Modifier.weight(1f),
                )
            }

            uiState.addCardError?.let {
                Spacer(Modifier.height(12.dp))
                NoticeRow(text = it, tint = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            DialogButtons(
                onCancel = { onIntent(WalletIntent.DismissAddCard) },
                onConfirm = { onIntent(WalletIntent.SubmitAddCard) },
                confirmLabel = "Ekle",
                enabled = uiState.canSubmitCard,
                loading = uiState.isAddingCard,
            )
        }
    }
}

// ── Kart Sil onay pop-up (Dialog) ──
@Composable
private fun DeleteCardDialog(last4: String, onIntent: (WalletIntent) -> Unit) {
    Dialog(onDismissRequest = { onIntent(WalletIntent.DismissDeleteCard) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
        ) {
            Text(
                text = "Kartı sil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "•••• $last4 kartı silinsin mi? Bu işlem geri alınamaz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onIntent(WalletIntent.DismissDeleteCard) }) {
                    Text(text = "Vazgeç", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onIntent(WalletIntent.ConfirmDeleteCard) },
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = "Sil", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Ortak pop-up parçaları ──
@Composable
private fun DialogHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(RencarBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RencarBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DialogButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    enabled: Boolean,
    loading: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel, enabled = !loading) {
            Text(text = "Vazgeç", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onConfirm,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RencarBlue,
                contentColor = Color.White,
                disabledContainerColor = RencarBlue.copy(alpha = 0.4f),
                disabledContentColor = Color.White,
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(text = confirmLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RencarBlue,
            focusedLabelColor = RencarBlue,
            cursorColor = RencarBlue,
        ),
        modifier = modifier,
    )
}

@Composable
private fun BrandChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border = if (selected) RencarBlue else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) RencarBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) RencarBlue else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Ortak küçük parçalar ──
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    )
}

@Composable
private fun NoticeRow(text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = RencarIcons.Info, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(text = "Tekrar dene", color = RencarBlue)
        }
    }
}

/** "₺340,00" / "₺1.250,50" — Türkçe biçim (virgül ondalık, nokta binlik); WalletMapper ile tutarlı. */
private fun formatTl(value: Double): String = "₺%,.2f".format(Locale.forLanguageTag("tr"), value)

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewWallet = WalletUi(
    balance = 340.0,
    transactions = listOf(
        WalletTransactionUi("1", "Renault Clio kiralama", "Bugün · 14:32", "−₺110,50", isCredit = false),
        WalletTransactionUi("2", "Bakiye yükleme", "Dün · 09:10", "+₺200,00", isCredit = true),
    ),
)

private val PreviewCards = listOf(
    CardUi(id = "1", brand = "VISA", last4 = "4291", expLabel = "08/27", isDefault = true),
    CardUi(id = "2", brand = "MASTERCARD", last4 = "7740", expLabel = "11/26", isDefault = false),
)

@Preview(name = "Cüzdan · Light", showBackground = true, heightDp = 920)
@Composable
private fun WalletLightPreview() {
    RenCarTheme(darkTheme = false) {
        WalletScreen(
            uiState = WalletUiState(isLoading = false, wallet = PreviewWallet, cards = PreviewCards),
            onIntent = {},
        )
    }
}

@Preview(name = "Cüzdan · Dark", showBackground = true, heightDp = 920)
@Composable
private fun WalletDarkPreview() {
    RenCarTheme(darkTheme = true) {
        WalletScreen(
            uiState = WalletUiState(isLoading = false, wallet = PreviewWallet, cards = PreviewCards),
            onIntent = {},
        )
    }
}

@Preview(name = "Cüzdan · Bakiye Yükle pop-up", showBackground = true, heightDp = 920)
@Composable
private fun WalletTopupPreview() {
    RenCarTheme(darkTheme = false) {
        WalletScreen(
            uiState = WalletUiState(
                isLoading = false,
                wallet = PreviewWallet,
                cards = PreviewCards,
                showTopup = true,
                topupAmount = "200",
            ),
            onIntent = {},
        )
    }
}
