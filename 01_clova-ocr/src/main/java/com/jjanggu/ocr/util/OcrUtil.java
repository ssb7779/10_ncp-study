package com.jjanggu.ocr.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Component
public class OcrUtil {


    @Value("${ncp.clova-ocr.general.url}")
    private String GENERAL_OCR_URL;
    @Value("${ncp.clova-ocr.general.secretkey}")
    private String GENERAL_OCR_SECRET_KEY;

    @Value("${ncp.clova-ocr.template.url}")
    private String TEMPLATE_OCR_URL;
    @Value("${ncp.clova-ocr.template.secretkey}")
    private String TEMPLATE_OCR_SECRET_KEY;

    /**
     * NCP Clova OCR API 호출 후 응답 결과 반환용 메소드
     *
     * @param type - general|template
     * @param path - OCR 할 파일의 경로
     * @return - OCR 응답결과(문자열)
     */
    public String processOCR(String type, String path) {

        final String OCR_URL = "general".equals(type) ? GENERAL_OCR_URL : TEMPLATE_OCR_URL;
        final String SECRET_KEY = "general".equals(type) ? GENERAL_OCR_SECRET_KEY :TEMPLATE_OCR_SECRET_KEY;

        System.out.println(type);
        System.out.println(SECRET_KEY);

        try {
            // java.net.URL : 문자열 경로 정보를 parsing(잘게 쪼개서)해서 관리하는 객체
            URL url = new URL(OCR_URL);
            // HttpURLConnection : 해당 URL을 통해 통신 가능한 Connection 객체
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            // Request Header(요청 헤더) 설정
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setReadTimeout(30000);
            con.setRequestMethod("POST");

            // Request Header 경계 설정
            String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
            // NCP 요청 필수 설정
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("X-OCR-SECRET", SECRET_KEY); // X-OCR-SECRET : HTTP 표준 아니고 자체적으로 정해놓은 것

            // Request Body(요청 본문) 설정
            // JSON 방식으로 JSON 문자열화 시키는거 => Jackson 방식으로
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("version", "V2");
            jsonMap.put("requestId", UUID.randomUUID().toString());
            jsonMap.put("timestamp", System.currentTimeMillis());

            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("format", "jpg");
            imageMap.put("name", "demo");

            List<Map<String, Object>> imagesList = new ArrayList<>();
            imagesList.add(imageMap);

            jsonMap.put("images", imagesList); // {version:V2, requestId:xxx, timestamp:xxx, .., images:[] }

            //         Jackson(ObjectMapper)
            //Java 객체 (Map) =====> JSON 문자열 변환
            ObjectMapper objectMapper = new ObjectMapper();
            String postParams = objectMapper.writeValueAsString(jsonMap); // '{"version":"V2", "requestId":"xxx", timestamp:xxx, .., images:[] }

            // 통신 시작
            con.connect();

            // 1. 요청
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());

            File file = new File(path);
            // multipart/form-data 형식의 요청 메세지 작성
            writeMultiPart(wr, postParams, file, boundary);
            wr.close();
            // ------------------------------------------------------------------

            // 2. 응답
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                // conn 입력 스트림으로부터 응답데이터 읽어들이기 위한 입력용 스트림 생성
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            // 응답 읽어들이기 (한 줄씩)
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            return response.toString(); // JSON문자열

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
            IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n"); // 경계1 - message=json문자열
        sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
        sb.append(jsonMessage);
        sb.append("\r\n");

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();

        if (file != null && file.isFile()) {
            out.write(("--" + boundary + "\r\n").getBytes("UTF-8")); // 경계2 - 파일명, 파일이진데이터
            StringBuilder fileString = new StringBuilder();
            fileString
                    .append("Content-Disposition:form-data; name=\"file\"; filename=");
            fileString.append("\"" + file.getName() + "\"\r\n");
            fileString.append("Content-Type: application/octet-stream\r\n\r\n");
            out.write(fileString.toString().getBytes("UTF-8"));
            out.flush();

            // 파일 이진데이터 처리
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.write("\r\n".getBytes());
            }

            out.write(("--" + boundary + "--\r\n").getBytes("UTF-8")); //경계3
        }
        out.flush();
    }
}

