package com.achobeta.refine.ai.ocr;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.ai.ocr.application.OcrService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ocr/")
public class OcrController {
    private final OcrService service;
    private final OcrUploadProperties uploadProperties;

    public OcrController(OcrService service, OcrUploadProperties uploadProperties) {
        this.service = service;
        this.uploadProperties = uploadProperties;
    }

    @PostMapping(value = "extract-first", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<OcrService.OcrResult> extract(@RequestPart("file") MultipartFile file,
                                                   @RequestParam(required = false) String fileType) throws IOException {
        if (file.isEmpty()) throw new AppException(1001, "file must not be empty");
        if (file.getSize() > uploadProperties.getMaxSize().toBytes()) {
            throw new AppException(1001, "file exceeds maximum upload size");
        }
        String type = fileType == null || fileType.isBlank() ? file.getOriginalFilename() : fileType;
        if (!uploadProperties.supports(type) && !uploadProperties.supports(file.getContentType())) {
            throw new AppException(1001, "unsupported file type");
        }
        return Response.success(service.extractFirst(UserContext.get(), file.getBytes(), type));
    }
}
