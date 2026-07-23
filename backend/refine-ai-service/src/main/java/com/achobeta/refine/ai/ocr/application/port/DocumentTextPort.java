package com.achobeta.refine.ai.ocr.application.port;

public interface DocumentTextPort {
    String extract(byte[] bytes, String fileType);
}
