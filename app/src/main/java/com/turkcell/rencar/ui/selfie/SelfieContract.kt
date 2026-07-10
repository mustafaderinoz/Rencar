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
 * Canlı ön kamera + ML Kit yüz algılama; yüz ~1 sn ortalı kalınca ehliyet ön+arka
 * görselleri POST /license/upload'a yüklenir (client-side liveness kapısı).
 */
data class SelfieUiState(
    val permissionGranted: Boolean = false,
    /** En az bir kez izin istendi mi (ilk açılışta gereksiz rationale göstermemek için). */
    val permissionRequested: Boolean = false,
    val faceStatus: FaceStatus = FaceStatus.NoFace,
    /** Yüz ortalı tutulduğunda 0f→1f dolan ilerleme; dolunca yükleme tetiklenir. */
    val holdProgress: Float = 0f,
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
    data object RetryClicked : SelfieIntent
    data object BackClicked : SelfieIntent
    data object DoneClicked : SelfieIntent
}
