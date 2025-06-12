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

    /**
     * NCP Clova OCR API 호출 후 응답 결과 반환용 메소드
     *
     * @param type - general|template
     * @param path - OCR 할 파일의 경로
     * @return - OCR 응답결과(문자열)
     */
    public String processOCR(String type, String path) {

        try {
            URL url = new URL(GENERAL_OCR_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setReadTimeout(30000);
            con.setRequestMethod("POST");
            String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("X-OCR-SECRET", GENERAL_OCR_SECRET_KEY);

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

            con.connect();


            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            long start = System.currentTimeMillis();
            File file = new File(path);
            writeMultiPart(wr, postParams, file, boundary);
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            System.out.println(response);

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
            IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
        sb.append(jsonMessage);
        sb.append("\r\n");

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();

        if (file != null && file.isFile()) {
            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            StringBuilder fileString = new StringBuilder();
            fileString
                    .append("Content-Disposition:form-data; name=\"file\"; filename=");
            fileString.append("\"" + file.getName() + "\"\r\n");
            fileString.append("Content-Type: application/octet-stream\r\n\r\n");
            out.write(fileString.toString().getBytes("UTF-8"));
            out.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.write("\r\n".getBytes());
            }

            out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        }
        out.flush();
    }
}

