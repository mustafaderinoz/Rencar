package com.turkcell.rencar.ui.selfie

/** Selfie tarama sırasında yüz konum durumu (FaceCenterAnalyzer'dan gelir). */
enum class FaceStatus {
    /** Kadrajda yüz yok. */
    NoFace,

    /** Yüz var ama ovalin dışında / çok uzak / çok yakın. */
    NotCentered,

    /** Yüz ovale ortalandı. */
    Centered,
}

/**
 * 06 Selfie Doğrulama (2. adım) — saf UI durumu (§4.2).
 * Canlı ön kamera + ML Kit yüz algılama; yüz ~1 sn ortalı kalınca ekran katmanı selfie karesini
 * çeker ve ehliyet ön+arka + selfie POST /license/upload'a yüklenir (selfie backend'de zorunlu — D5).
 */
data class SelfieUiState(
    val permissionGranted: Boolean = false,
    /** En az bir kez izin istendi mi (ilk açılışta gereksiz rationale göstermemek için). */
    val permissionRequested: Boolean = false,
    val faceStatus: FaceStatus = FaceStatus.NoFace,
    /** Yüz ortalı tutulduğunda 0f→1f dolan ilerleme; dolunca çekim tetiklenir. */
    val holdProgress: Float = 0f,
    /**
     * Yüz yeterince ortalı kaldı → ekran katmanı ön kameradan selfie karesini çeksin. Sonuç
     * [SelfieIntent.SelfieCaptured]/[SelfieIntent.SelfieCaptureFailed] ile döner ve bayrak sıfırlanır.
     */
    val captureRequested: Boolean = false,
    /** Çekim + yükleme sürerken "Doğrulanıyor…" gösterilir; yeni kareler değerlendirilmez. */
    val isUploading: Boolean = false,
    /** POST /license/upload başarılı → Onay adımı overlay'i gösterilir. */
    val uploaded: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Kullanıcı/sistem aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface SelfieIntent {
    data class PermissionResult(val granted: Boolean) : SelfieIntent
    data class FaceStatusChanged(val status: FaceStatus) : SelfieIntent

    /** Ön kameradan selfie karesi diske yazıldı ([path]); yükleme bu dosyayla tetiklenir. */
    data class SelfieCaptured(val path: String) : SelfieIntent

    /** Selfie karesi çekilemedi (kamera hatası); kullanıcı tekrar deneyebilir. */
    data object SelfieCaptureFailed : SelfieIntent

    data object RetryClicked : SelfieIntent
    data object BackClicked : SelfieIntent
    data object DoneClicked : SelfieIntent
}
