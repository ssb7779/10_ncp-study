package com.jjanggu.ocr.controller;

import com.jjanggu.ocr.util.FileUtil;
import com.jjanggu.ocr.util.OcrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Controller
public class OCRController {

    private final FileUtil fileUtil;
    private final OcrUtil ocrUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(String type, MultipartFile file){

        // 파일 저장
        Map<String, String> map = fileUtil.fileupload("ocr", file);
        // 저장된 파일의 path : map.get("filePath") + "/" + map.get("filesystemName")

        // OCR API 호출

        String response = ocrUtil.processOCR(type, map.get("filePath") + "/" + map.get("filesystemName"));

        Map<String, Object> responseMessage = new HashMap<>();
        responseMessage.put("message", file.getOriginalFilename() + "이미지가 정상적으로 처리되었습니다.");
        responseMessage.put("result", response);

        return ResponseEntity
                .ok()
                .body(responseMessage);
    }
}
