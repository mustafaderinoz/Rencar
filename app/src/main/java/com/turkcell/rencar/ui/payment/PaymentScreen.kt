package com.turkcell.rencar.ui.payment

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
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.turkcell.rencar.data.model.PaymentReceiptUi
import com.turkcell.rencar.data.model.PaymentResultUi
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar
import java.util.Locale

/** Marka mavisi — tema-bağımsız (bkz. ActiveRental/Login/OTP). */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5): state'i toplar, intent'leri VM'e iletir ──
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentScreen(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                PaymentIntent.BackClicked -> onNavigateBack()
                PaymentIntent.DoneClicked -> onNavigateToHome()
                else -> viewModel.onIntent(intent)
            }
        },
    )
}

// ── Stateless gövde (§4.5): uiState + onIntent ile çizer ──
@Composable
private fun PaymentScreen(
    uiState: PaymentUiState,
    onIntent: (PaymentIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        TopBar(onBack = { onIntent(PaymentIntent.BackClicked) })

        when {
            uiState.isLoading ->
                LoadingState(Modifier.weight(1f))

            uiState.loadError != null && uiState.receipt == null ->
                ErrorState(
                    message = uiState.loadError,
                    onRetry = { onIntent(PaymentIntent.Retry) },
                    modifier = Modifier.weight(1f),
                )

            uiState.isPaid ->
                PaidState(
                    result = uiState.result!!,
                    onDone = { onIntent(PaymentIntent.DoneClicked) },
                    modifier = Modifier.weight(1f),
                )

            uiState.receipt != null ->
                Content(
                    uiState = uiState,
                    receipt = uiState.receipt,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f),
                )
        }
    }

    if (uiState.showAddCard) {
        AddCardDialog(uiState = uiState, onIntent = onIntent)
    }

    uiState.deleteCandidateCardId?.let { candidateId ->
        val card = uiState.cards.firstOrNull { it.id == candidateId }
        DeleteCardDialog(last4 = card?.last4.orEmpty(), onIntent = onIntent)
    }

    // İyzico ödeme sayfası: ayrı sayfa değil, ekranı kaplayan katman (Kart Ekle pop-up'ıyla aynı
    // kalıp — decisions.md). Oturum açıkken diğer her şeyin üstünü örter.
    uiState.iyzicoCheckout?.let { checkout ->
        IyzicoCheckoutOverlay(
            paymentPageUrl = checkout.paymentPageUrl,
            onCallbackReached = { onIntent(PaymentIntent.IyzicoCallbackReached) },
            onClose = { onIntent(PaymentIntent.IyzicoDismissed) },
        )
    }
}

// ── Üst başlık: ‹ geri + "Ödeme" ──
@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.ChevronLeft,
                contentDescription = "Geri",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Ödeme",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── İçerik: özet kartı, ücret dökümü, yöntem, indirim, öde ──
@Composable
private fun Content(
    uiState: PaymentUiState,
    receipt: PaymentReceiptUi,
    onIntent: (PaymentIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            TripSummaryCard(receipt)
            Spacer(Modifier.height(14.dp))
            FeeBreakdownCard(receipt)
            Spacer(Modifier.height(22.dp))

            SectionTitle("Ödeme Yöntemi")
            Spacer(Modifier.height(12.dp))
            MethodSelector(uiState.method, onIntent)
            Spacer(Modifier.height(12.dp))
            when (uiState.method) {
                PaymentMethod.WALLET -> WalletSection(uiState)
                PaymentMethod.CARD -> CardSection(uiState, onIntent)
                PaymentMethod.IYZICO -> IyzicoSection(uiState)
            }

            // İndirim kodu İyzico ödemesinde API tarafından reddedilir (400) — alan gizlenir.
            if (uiState.isDiscountAvailable) {
                Spacer(Modifier.height(22.dp))
                SectionTitle("İndirim Kodu")
                Spacer(Modifier.height(12.dp))
                DiscountField(uiState.discountCode, onIntent)
            }
            Spacer(Modifier.height(20.dp))
        }

        PayBar(uiState = uiState, onIntent = onIntent)
    }
}

// ── Reusable kart kabı: yumuşak zemin + ince kenarlık + 20dp yuvarlama ──
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    padding: Int = 18,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(padding.dp),
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

// ── Yolculuk özeti kartı: yeşil tik + araç + Süre/Mesafe ──
@Composable
private fun TripSummaryCard(receipt: PaymentReceiptUi) {
    SectionCard(padding = 20) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SuccessBadge()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Yolculuk tamamlandı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(receipt.vehicleTitle, receipt.vehiclePlate, receipt.planLabel)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryStat(label = "Süre", value = "${receipt.durationMinutes} dk", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            )
            SummaryStat(
                label = "Mesafe",
                value = "%.1f km".format(Locale.forLanguageTag("tr"), receipt.distanceKm),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SuccessBadge(size: Int = 56) {
    val success = MaterialTheme.rencar.success
    Box(
        modifier = Modifier
            .size((size + 12).dp)
            .clip(CircleShape)
            .background(success.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(success),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((size * 0.55f).dp),
            )
        }
    }
}

// ── Ücret dökümü kartı ──
@Composable
private fun FeeBreakdownCard(receipt: PaymentReceiptUi) {
    SectionCard {
        Text(
            text = "Ücret Dökümü",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        FeeRow("Kiralama ücreti (${receipt.durationMinutes} dk)", receipt.usageFee)
        Spacer(Modifier.height(11.dp))
        FeeRow("Başlangıç ücreti", receipt.startFee)
        Spacer(Modifier.height(11.dp))
        FeeRow("Hizmet bedeli", receipt.serviceFee)
        Spacer(Modifier.height(14.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Toplam",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatCost(receipt.totalPrice),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RencarBlue,
            )
        }
    }
}

@Composable
private fun FeeRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatCost(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

// ── Cüzdan / Kart / İyzico — segmentli seçici (pill) ──
@Composable
private fun MethodSelector(selected: PaymentMethod, onIntent: (PaymentIntent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MethodSegment(
            icon = RencarIcons.Wallet,
            label = "Cüzdan",
            selected = selected == PaymentMethod.WALLET,
            onClick = { onIntent(PaymentIntent.SelectMethod(PaymentMethod.WALLET)) },
            modifier = Modifier.weight(1f),
        )
        MethodSegment(
            icon = RencarIcons.CreditCard,
            label = "Kart",
            selected = selected == PaymentMethod.CARD,
            onClick = { onIntent(PaymentIntent.SelectMethod(PaymentMethod.CARD)) },
            modifier = Modifier.weight(1f),
        )
        MethodSegment(
            icon = RencarIcons.Shield,
            label = "İyzico",
            selected = selected == PaymentMethod.IYZICO,
            onClick = { onIntent(PaymentIntent.SelectMethod(PaymentMethod.IYZICO)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MethodSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) RencarBlue else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Cüzdan bölümü: bakiye + yetersizlik uyarısı ──
@Composable
private fun WalletSection(uiState: PaymentUiState) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RencarBlue.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RencarIcons.Wallet,
                        contentDescription = null,
                        tint = RencarBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Cüzdan bakiyesi",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatCost(uiState.walletBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (uiState.walletInsufficient) {
            Spacer(Modifier.height(12.dp))
            NoticeRow(
                text = "Bakiye yetersiz. Kartla ödeyebilir veya cüzdanınıza bakiye yükleyebilirsiniz.",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── İyzico bölümü: güvenli ödeme açıklaması + tutar sınırı uyarısı ──
@Composable
private fun IyzicoSection(uiState: PaymentUiState) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RencarBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RencarIcons.Shield,
                    contentDescription = null,
                    tint = RencarBlue,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "İyzico ile güvenli ödeme",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Kart bilgileriniz uygulamada tutulmaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        NoticeRow(
            text = "Ödeme, İyzico'nun güvenli ödeme sayfasında yapılır.",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (uiState.iyzicoAmountOutOfRange) {
            Spacer(Modifier.height(12.dp))
            NoticeRow(
                text = "Bu tutar İyzico ile ödenemez. Cüzdan veya kartla ödeyebilirsiniz.",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Kart bölümü: kart listesi + yeni kart ekle ──
@Composable
private fun CardSection(uiState: PaymentUiState, onIntent: (PaymentIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    text = "Ödemek için aşağıdan yeni bir kart ekleyin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            uiState.cards.forEach { card ->
                CardItem(
                    card = card,
                    selected = card.id == uiState.selectedCardId,
                    isSettingDefault = card.id == uiState.settingDefaultCardId,
                    isDeleting = card.id == uiState.deletingCardId,
                    onClick = { onIntent(PaymentIntent.SelectCard(card.id)) },
                    onSetDefault = { onIntent(PaymentIntent.SetDefaultCard(card.id)) },
                    onDelete = { onIntent(PaymentIntent.DeleteCardClicked(card.id)) },
                )
            }
        }
        AddCardButton(onClick = { onIntent(PaymentIntent.AddCardClicked) })
    }
}

// ── Kart öğesi: seçilebilir kart görünümü + varsayılan/sil aksiyonları ──
@Composable
private fun CardItem(
    card: CardUi,
    selected: Boolean,
    isSettingDefault: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    val borderColor = if (selected) RencarBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val bg = if (selected) RencarBlue.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceContainerLow
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioDot(selected)
            Spacer(Modifier.width(12.dp))
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
private fun AddCardButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                RencarBlue.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = RencarIcons.CreditCard,
            contentDescription = null,
            tint = RencarBlue,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Yeni kart ekle",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = RencarBlue,
        )
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val color = if (selected) RencarBlue else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(RencarBlue),
            )
        }
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

// ── İndirim kodu alanı + "ödemede uygulanacak" ipucu ──
@Composable
private fun DiscountField(value: String, onIntent: (PaymentIntent) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onIntent(PaymentIntent.DiscountCodeChanged(it)) },
        placeholder = { Text("Kodun varsa gir (opsiyonel)") },
        leadingIcon = {
            Icon(imageVector = RencarIcons.Gift, contentDescription = null, tint = RencarBlue, modifier = Modifier.size(20.dp))
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RencarBlue,
            cursorColor = RencarBlue,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    if (value.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        NoticeRow(text = "Kod ödeme sırasında uygulanacak.", tint = MaterialTheme.rencar.success)
    }
}

// ── Küçük bilgilendirme satırı (ikon + metin) ──
@Composable
private fun NoticeRow(text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = RencarIcons.Info, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

// ── Alt öde çubuğu: üst ayraç + ödenecek özeti + buton + güvenlik ipucu ──
@Composable
private fun PayBar(uiState: PaymentUiState, onIntent: (PaymentIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            uiState.payError?.let {
                NoticeRow(text = it, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ödenecek tutar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCost(uiState.payableAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Button(
                onClick = { onIntent(PaymentIntent.PayClicked) },
                enabled = uiState.canPay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RencarBlue,
                    contentColor = Color.White,
                    disabledContainerColor = RencarBlue.copy(alpha = 0.4f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (uiState.isPaying) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = "${formatCost(uiState.payableAmount)} Öde",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = RencarIcons.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Ödemeler güvenli şekilde işlenir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Kart Ekle pop-up (Dialog): marka + son 4 hane + SKT ──
@Composable
private fun AddCardDialog(uiState: PaymentUiState, onIntent: (PaymentIntent) -> Unit) {
    Dialog(
        onDismissRequest = { onIntent(PaymentIntent.DismissAddCard) },
        properties = DialogProperties(dismissOnClickOutside = !uiState.isAddingCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(RencarBlue.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RencarIcons.CreditCard,
                        contentDescription = null,
                        tint = RencarBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Kart Ekle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(18.dp))

            // Marka seçimi (VISA / Mastercard)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandChip(
                    label = "VISA",
                    selected = uiState.addCardBrand == CardBrand.VISA,
                    onClick = { onIntent(PaymentIntent.AddCardBrandChanged(CardBrand.VISA)) },
                    modifier = Modifier.weight(1f),
                )
                BrandChip(
                    label = "Mastercard",
                    selected = uiState.addCardBrand == CardBrand.MASTERCARD,
                    onClick = { onIntent(PaymentIntent.AddCardBrandChanged(CardBrand.MASTERCARD)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))

            DialogField(
                value = uiState.addCardLast4,
                onValueChange = { onIntent(PaymentIntent.AddCardLast4Changed(it)) },
                label = "Son 4 hane",
                placeholder = "1234",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogField(
                    value = uiState.addCardMonth,
                    onValueChange = { onIntent(PaymentIntent.AddCardMonthChanged(it)) },
                    label = "SKT Ay",
                    placeholder = "1-12",
                    modifier = Modifier.weight(1f),
                )
                DialogField(
                    value = uiState.addCardYear,
                    onValueChange = { onIntent(PaymentIntent.AddCardYearChanged(it)) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onIntent(PaymentIntent.DismissAddCard) },
                    enabled = !uiState.isAddingCard,
                ) {
                    Text(text = "Vazgeç", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onIntent(PaymentIntent.SubmitAddCard) },
                    enabled = uiState.canSubmitCard,
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RencarBlue,
                        contentColor = Color.White,
                        disabledContainerColor = RencarBlue.copy(alpha = 0.4f),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (uiState.isAddingCard) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(text = "Ekle", fontWeight = FontWeight.Bold)
                    }
                }
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

// ── Kart Sil onay pop-up (Dialog) ──
@Composable
private fun DeleteCardDialog(last4: String, onIntent: (PaymentIntent) -> Unit) {
    Dialog(onDismissRequest = { onIntent(PaymentIntent.DismissDeleteCard) }) {
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
                TextButton(onClick = { onIntent(PaymentIntent.DismissDeleteCard) }) {
                    Text(text = "Vazgeç", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onIntent(PaymentIntent.ConfirmDeleteCard) },
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

// ── Ödendi durumu: yeşil tik + makbuz + "Bitti" ──
@Composable
private fun PaidState(result: PaymentResultUi, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        SuccessBadge(size = 64)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Ödeme alındı",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatCost(result.paidAmount),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = RencarBlue,
        )
        Spacer(Modifier.height(20.dp))
        SectionCard {
            if (result.discountAmount > 0.0) {
                ReceiptRow("İndirim", "− ${formatCost(result.discountAmount)}", valueColor = MaterialTheme.rencar.success)
                Spacer(Modifier.height(10.dp))
            }
            ReceiptRow("Ödeme yöntemi", paidMethodLabel(result))
            Spacer(Modifier.height(10.dp))
            ReceiptRow("Ödenen tutar", formatCost(result.paidAmount), valueColor = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RencarBlue, contentColor = Color.White),
        ) {
            Text(text = "Bitti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

// ── Yükleniyor / Hata ──
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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

// ── Yardımcılar ──

/** Ücret: nokta ondalık, "24.50 ₺" (tasarımla birebir; ActiveRental ile tutarlı). */
private fun formatCost(value: Double): String = "%.2f ₺".format(Locale.US, value)

/**
 * Ödenen yöntem etiketi: İyzico, kart ("VISA · •••• 4291") veya cüzdan ("Cüzdan · kalan 203.50 ₺").
 * İyzico'da kart meta'sı dönmez (PayRentalResponse.card yalnız CARD yönteminde dolar).
 */
private fun paidMethodLabel(result: PaymentResultUi): String = when {
    result.method.equals("IYZICO", ignoreCase = true) -> "İyzico"
    result.method.equals("CARD", ignoreCase = true) -> {
        val brand = if (result.cardBrand.equals("MASTERCARD", ignoreCase = true)) "Mastercard" else "VISA"
        "$brand · •••• ${result.cardLast4.orEmpty()}"
    }
    result.walletBalance != null -> "Cüzdan · kalan ${formatCost(result.walletBalance)}"
    else -> "Cüzdan"
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewReceipt = PaymentReceiptUi(
    rentalId = "clx0rent1234567890",
    vehicleTitle = "Renault Clio",
    vehiclePlate = "34 HCH 305",
    planLabel = "Dakikalık",
    durationMinutes = 1,
    distanceKm = 0.7,
    usageFee = 4.5,
    startFee = 15.0,
    serviceFee = 0.5,
    totalPrice = 20.0,
    alreadyPaid = false,
)

private val PreviewCards = listOf(
    CardUi(id = "1", brand = "VISA", last4 = "4291", expLabel = "08/28", isDefault = true),
    CardUi(id = "2", brand = "MASTERCARD", last4 = "5310", expLabel = "11/27", isDefault = false),
)

@Preview(name = "Payment · Kart · Light", showBackground = true, heightDp = 980)
@Composable
private fun PaymentCardLightPreview() {
    RenCarTheme(darkTheme = false) {
        PaymentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                receipt = PreviewReceipt,
                method = PaymentMethod.CARD,
                cards = PreviewCards,
                selectedCardId = "1",
                walletBalance = 4904.0,
                discountCode = "İLKSÜRÜŞ",
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Payment · Cüzdan · Dark", showBackground = true, heightDp = 980)
@Composable
private fun PaymentWalletDarkPreview() {
    RenCarTheme(darkTheme = true) {
        PaymentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                receipt = PreviewReceipt,
                method = PaymentMethod.WALLET,
                walletBalance = 4904.0,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Payment · İyzico · Light", showBackground = true, heightDp = 980)
@Composable
private fun PaymentIyzicoLightPreview() {
    RenCarTheme(darkTheme = false) {
        PaymentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                receipt = PreviewReceipt,
                method = PaymentMethod.IYZICO,
                walletBalance = 4904.0,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Payment · Kart Ekle pop-up", showBackground = true, heightDp = 980)
@Composable
private fun PaymentAddCardPreview() {
    RenCarTheme(darkTheme = false) {
        PaymentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                receipt = PreviewReceipt,
                method = PaymentMethod.CARD,
                cards = PreviewCards,
                selectedCardId = "1",
                showAddCard = true,
                addCardLast4 = "4921",
                addCardMonth = "12",
                addCardYear = "2028",
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Payment · Ödendi", showBackground = true, heightDp = 700)
@Composable
private fun PaymentPaidPreview() {
    RenCarTheme(darkTheme = false) {
        PaymentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                receipt = PreviewReceipt,
                result = PaymentResultUi(
                    paidAmount = 18.0,
                    discountAmount = 2.0,
                    method = "CARD",
                    walletBalance = null,
                    cardBrand = "VISA",
                    cardLast4 = "4291",
                ),
            ),
            onIntent = {},
        )
    }
}
