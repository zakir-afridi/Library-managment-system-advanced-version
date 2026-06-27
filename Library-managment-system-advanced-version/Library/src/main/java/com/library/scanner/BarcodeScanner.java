package com.library.scanner;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * ZXing-based barcode/QR scanner for LibraCore Pro.
 *
 * Supports:
 *  - EAN-13 / EAN-8 (book ISBNs)
 *  - Code 128 / Code 39 (library barcodes)
 *  - QR Code (member ID cards)
 *  - Data Matrix
 *
 * Usage (still-image mode — no camera required for desktop):
 *   BarcodeScanner.decodeFromImage(imageBytes, result -> Platform.runLater(() -> { ... }));
 *
 * Usage (JavaFX Image):
 *   BarcodeScanner.decodeFromFxImage(image).ifPresent(code -> { ... });
 *
 * QR Code generation for member cards:
 *   BarcodeScanner.generateQrCodeImage(text, 200, 200)
 */
public final class BarcodeScanner {

    private static final Logger LOG = LoggerFactory.getLogger(BarcodeScanner.class);

    private static final MultiFormatReader READER;

    static {
        READER = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.UPC_A
        ));
        READER.setHints(hints);
    }

    private static final ExecutorService EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    private BarcodeScanner() {}

    // ── Decode from byte array ────────────────────────────────────────────────

    /**
     * Decode a barcode/QR from image bytes (JPEG, PNG, etc.) asynchronously.
     * The callback is called on success; ignored on failure.
     */
    public static void decodeFromImageAsync(byte[] imageBytes, Consumer<String> onSuccess) {
        EXECUTOR.submit(() -> {
            decodeFromImageBytes(imageBytes).ifPresent(onSuccess);
        });
    }

    /**
     * Synchronously decode a barcode from raw image bytes.
     */
    public static java.util.Optional<String> decodeFromImageBytes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0)
            return java.util.Optional.empty();
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBytes));
            return decode(bi);
        } catch (Exception e) {
            LOG.debug("BarcodeScanner decode error: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * Synchronously decode a barcode from a JavaFX Image.
     */
    public static java.util.Optional<String> decodeFromFxImage(Image fxImage) {
        if (fxImage == null) return java.util.Optional.empty();
        try {
            WritableImage wi = new WritableImage(
                (int) fxImage.getWidth(), (int) fxImage.getHeight());
            // snapshot approach
            BufferedImage bi = SwingFXUtils.fromFXImage(fxImage, null);
            return decode(bi);
        } catch (Exception e) {
            LOG.debug("BarcodeScanner FX decode error: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    // ── QR Code generation ────────────────────────────────────────────────────

    /**
     * Generate a QR code image for the given text (e.g., member ID).
     * Returns a JavaFX Image ready to display in an ImageView.
     */
    public static java.util.Optional<Image> generateQrCode(String text, int width, int height) {
        if (text == null || text.isBlank()) return java.util.Optional.empty();
        try {
            com.google.zxing.qrcode.QRCodeWriter writer =
                new com.google.zxing.qrcode.QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bi = com.google.zxing.client.j2se.MatrixToImageWriter
                .toBufferedImage(matrix);
            Image fxImage = SwingFXUtils.toFXImage(bi, null);
            return java.util.Optional.of(fxImage);
        } catch (Exception e) {
            LOG.warn("QR generation failed for '{}': {}", text, e.getMessage());
            return java.util.Optional.empty();
        }
    }

    // ── Core decode ───────────────────────────────────────────────────────────

    private static java.util.Optional<String> decode(BufferedImage image) {
        if (image == null) return java.util.Optional.empty();
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = READER.decodeWithState(bitmap);
            String text = result.getText();
            LOG.info("Barcode decoded: {} (format: {})", text, result.getBarcodeFormat());
            return java.util.Optional.of(text);
        } catch (NotFoundException e) {
            return java.util.Optional.empty();
        } catch (Exception e) {
            LOG.debug("BarcodeScanner decode failed: {}", e.getMessage());
            return java.util.Optional.empty();
        } finally {
            READER.reset();
        }
    }
}
