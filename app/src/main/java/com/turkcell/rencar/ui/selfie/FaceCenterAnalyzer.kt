package com.turkcell.rencar.ui.selfie

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

/**
 * CameraX [ImageAnalysis.Analyzer]: ML Kit ile her karede yüzü bulur ve yüzün oval
 * çerçeveye ortalı olup olmadığını [FaceStatus] olarak bildirir.
 *
 * Ortalama ölçütü (görüntü koordinatında): tek yüz + yüz merkezi görüntü merkezine yakın
 * ([CENTER_TOLERANCE_X]/[CENTER_TOLERANCE_Y]) + yüz genişliği makul aralıkta
 * ([MIN_FACE_RATIO]..[MAX_FACE_RATIO]) → [FaceStatus.Centered]. FAST mod, offline model.
 */
class FaceCenterAnalyzer(
    private val onStatus: (FaceStatus) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build(),
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        // Bounding box'lar döndürülmüş koordinat uzayında döner; boyutları buna göre kur.
        val (width, height) = if (rotation == 90 || rotation == 270) {
            mediaImage.height to mediaImage.width
        } else {
            mediaImage.width to mediaImage.height
        }

        val input = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(input)
            .addOnSuccessListener { faces -> onStatus(evaluate(faces, width, height)) }
            .addOnFailureListener { onStatus(FaceStatus.NoFace) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun evaluate(faces: List<Face>, width: Int, height: Int): FaceStatus {
        if (faces.isEmpty() || width <= 0 || height <= 0) return FaceStatus.NoFace

        // En büyük yüzü (kameraya en yakın) baz al.
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return FaceStatus.NoFace
        val box = face.boundingBox

        val dx = abs(box.exactCenterX() - width / 2f) / width
        val dy = abs(box.exactCenterY() - height / 2f) / height
        val faceRatio = box.width().toFloat() / width

        return when {
            faceRatio < MIN_FACE_RATIO -> FaceStatus.NotCentered // çok uzak
            faceRatio > MAX_FACE_RATIO -> FaceStatus.NotCentered // çok yakın
            dx > CENTER_TOLERANCE_X || dy > CENTER_TOLERANCE_Y -> FaceStatus.NotCentered
            else -> FaceStatus.Centered
        }
    }

    private companion object {
        const val CENTER_TOLERANCE_X = 0.16f
        const val CENTER_TOLERANCE_Y = 0.18f
        const val MIN_FACE_RATIO = 0.28f
        const val MAX_FACE_RATIO = 0.85f
    }
}
