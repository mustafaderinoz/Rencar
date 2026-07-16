package com.turkcell.rencar.data.model

/**
 * Cüzdan akışı — UI/domain modelleri (decisions.md → "Katman Derinliği" mapper katmanı).
 * DTO'lar ([com.turkcell.rencar.data.remote.dto.WalletResponse],
 * [com.turkcell.rencar.data.remote.dto.WalletTransaction]) UI'a doğrudan verilmez; işaretli tutar
 * etiketi ve göreli tarih ("Bugün · 14:32") mapper'da üretilir.
 */

/** Cüzdan durumu: güncel bakiye + son işlemler (yeniden eskiye). */
data class WalletUi(
    val balance: Double,
    val transactions: List<WalletTransactionUi>,
)

/**
 * Tek cüzdan hareketi (Son işlemler satırı). [title] açıklama; [dateLabel] "Bugün · 14:32";
 * [amountLabel] işaretli tutar etiketi ("+₺200,00" / "−₺110,50"); [isCredit] pozitif (yükleme/bonus).
 */
data class WalletTransactionUi(
    val id: String,
    val title: String,
    val dateLabel: String,
    val amountLabel: String,
    val isCredit: Boolean,
)
