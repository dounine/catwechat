package com.dounine.catwechat.tools.util

import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.{ByteMatrix, Encoder, QRCode}
import com.google.zxing.{BarcodeFormat, EncodeHintType, Writer}

import java.util

class MyQRCodeWriter extends Writer {

  private final val QUIET_ZONE_SIZE: Int = 4

  override def encode(
      contents: String,
      format: BarcodeFormat,
      width: Int,
      height: Int
  ): BitMatrix = {
    encode(contents, format, width, height, null)
  }

  override def encode(
      contents: String,
      format: BarcodeFormat,
      width: Int,
      height: Int,
      hints: util.Map[EncodeHintType, _]
  ): BitMatrix = {
    if (contents.isEmpty) {
      throw new IllegalArgumentException("Found empty contents")
    }

    if (format != BarcodeFormat.QR_CODE) {
      throw new IllegalArgumentException(
        "Can only encode QR_CODE, but got " + format
      )
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException(
        "Requested dimensions are too small: " + width + 'x' +
          height
      )
    }
    var errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.L
    var quietZone: Int = QUIET_ZONE_SIZE
    if (hints != null) {
      if (hints.containsKey(EncodeHintType.ERROR_CORRECTION)) {
        errorCorrectionLevel = ErrorCorrectionLevel.valueOf(
          hints.get(EncodeHintType.ERROR_CORRECTION).toString
        )
      }
      if (hints.containsKey(EncodeHintType.MARGIN)) {
        quietZone = Integer.parseInt(hints.get(EncodeHintType.MARGIN).toString)
      }
    }

    val code: QRCode = Encoder.encode(contents, errorCorrectionLevel, hints)
    renderResult(code, width, height, quietZone);
  }

  def renderResult(
      code: QRCode,
      width: Int,
      height: Int,
      quietZone: Int
  ): BitMatrix = {
    val input: ByteMatrix = code.getMatrix
    if (input == null) {
      throw new IllegalStateException()
    }
    val inputWidth: Int = input.getWidth
    val inputHeight: Int = input.getHeight;
    val qrWidth: Int = inputWidth + (quietZone * 2)
    val qrHeight: Int = inputHeight + (quietZone * 2)
    var outputWidth: Int = Math.max(width, qrWidth)
    var outputHeight: Int = Math.max(height, qrHeight)

    val multiple: Int = Math.min(outputWidth / qrWidth, outputHeight / qrHeight)

//    outputWidth = qrWidth * multiple; // 改动点
//    outputHeight = qrWidth * multiple; // 改动点

    var leftPadding: Int = (outputWidth - (inputWidth * multiple)) / 2
    var topPadding: Int = (outputHeight - (inputHeight * multiple)) / 2
//    leftPadding = 0
//    topPadding = 0
    val output: BitMatrix = new BitMatrix(outputWidth, outputHeight)

    var outputX: Int = leftPadding
    var outputY: Int = topPadding
    (0 until inputHeight)
      .foreach(inputY => {
        outputY = outputY + multiple
        outputX = leftPadding
        (0 until inputWidth)
          .foreach(inputX => {
            outputX = outputX + multiple
            if (input.get(inputX, inputY) == 1) {
              output.setRegion(outputX, outputY, multiple, multiple)
            }
          })
      })
//    for (var inputY: Int = 0, var outputY: Int = topPadding; inputY < inputHeight; inputY = inputY+1, outputY = outputY + multiple) {
//      for (var inputX: Int = 0, var outputX: Int = leftPadding; inputX < inputWidth; inputX = inputX+1, outputX =outputX + multiple) {
//        if (input.get(inputX, inputY) == 1) {
//          output.setRegion(outputX, outputY, multiple, multiple)
//        }
//      }
//    }
    output
  }
}
