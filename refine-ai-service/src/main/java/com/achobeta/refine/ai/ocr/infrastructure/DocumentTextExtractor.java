package com.achobeta.refine.ai.ocr.infrastructure;

import com.achobeta.refine.ai.ocr.application.port.ImageOcrPort;
import com.achobeta.refine.ai.ocr.application.port.DocumentTextPort;
import com.achobeta.refine.common.api.AppException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class DocumentTextExtractor implements DocumentTextPort {
    private final ImageOcrPort imageOcrProvider;

    public DocumentTextExtractor(ImageOcrPort imageOcrProvider) { this.imageOcrProvider = imageOcrProvider; }

    @Override
    public String extract(byte[] bytes, String fileType) {
        if (bytes == null || bytes.length == 0) throw new AppException(1001, "文件为空");
        String type = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        try {
            if (isPlainText(type)) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            if (type.endsWith(".docx") || type.equals("docx") || type.contains("wordprocessingml")) {
                try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            }
            if (type.endsWith(".pdf") || type.equals("pdf") || type.equals("application/pdf")) {
                try (PDDocument document = Loader.loadPDF(bytes)) {
                    String text = new PDFTextStripper().getText(document);
                    if (text != null && !text.isBlank()) return text;
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 160), "png", output);
                    return imageOcrProvider.recognize(output.toByteArray());
                }
            }
            return imageOcrProvider.recognize(bytes);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(10003, "文件解析或 OCR 识别失败");
        }
    }

    private boolean isPlainText(String type) {
        return type.endsWith(".txt") || type.equals("txt")
                || type.endsWith(".md") || type.equals("md")
                || type.startsWith("text/");
    }
}
