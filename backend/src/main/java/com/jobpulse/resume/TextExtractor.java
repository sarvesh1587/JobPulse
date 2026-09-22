package com.jobpulse.resume;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** One implementation per supported file format (PDF, DOCX, ...). */
public interface TextExtractor {

    /** Content types this extractor handles, e.g. "application/pdf". */
    boolean supports(String contentType);

    String extractText(MultipartFile file) throws IOException;
}
