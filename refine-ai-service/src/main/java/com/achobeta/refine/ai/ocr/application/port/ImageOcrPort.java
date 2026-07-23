package com.achobeta.refine.ai.ocr.application.port;

public interface ImageOcrPort {
    String recognize(byte[] imageBytes);
}
