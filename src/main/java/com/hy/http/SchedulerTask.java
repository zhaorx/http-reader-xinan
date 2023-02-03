package com.hy.http;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.hy.http.model.DataItem;
import com.hy.http.model.Gas;
import com.hy.http.model.LoginResult;
import com.hy.http.model.WritterResult;
import okhttp3.*;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class SchedulerTask {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerTask.class);
    @Value("${push-url}")
    private String pushUrl;
    @Value("${push-multi-url}")
    private String pushMultiUrl;
    @Value("${login-url}")
    private String loginUrl;
    @Value("${data-url}")
    private String dataUrl;
    @Value("${pspaceUsername}")
    private String pspaceUsername;
    @Value("${pspacePassword}")
    private String pspacePassword;
    @Value("${thirdAppId}")
    private String thirdAppId;
    @Value("${licenseCode}")
    private String licenseCode;
    @Value("${region}")
    private String region;
    @Value("${tags:}")
    private String tags;
    private String sep = "_";
    private boolean isLogin = false;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build();


    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() throws Exception {
        try {
            logger.info("starting transfer...");
            // 调用login 初始化接口会话
            if (!isLogin) {
                isLogin = this.login();
                logger.info("isLogin：" + isLogin);
            }

            if (!isLogin) {
                return;
            }

//            List<DataItem> list = this.getRecentData();
            List<DataItem> list = this.getRecentDataByOkHttp();

            if (list == null || list.size() == 0) {
                logger.error("getRecentData error：no data");
                return;
            }

            String dateStr = sdf.format(new Date());
            List<Gas> addList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                DataItem item = list.get(i);
                logger.debug("######get_data:" + item.toString());
                Gas g = new Gas();
                g.setTs(dateStr);
                g.setRegion(region);
                String tag = item.getTag().substring(1, item.getTag().length());
                g.setPoint(region + sep + tag);
                g.setPname(region + sep + item.getTAGDESC());
                g.setValue(item.getValue());
                g.setUnit(item.getUNIT());
                logger.debug("######point_data:" + g.toString());
                addList.add(g);
            }

            WritterResult result = this.addMultiTaos(addList);
            logger.info(result.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("###error###", e.getMessage());
        }
    }


    public Boolean login() {
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginUrl)
                .queryParam("pspaceUsername", pspaceUsername)
                .queryParam("pspacePassword", pspacePassword)
                .queryParam("thirdAppId", thirdAppId)
                .queryParam("licenseCode", licenseCode);
        HttpEntity<JSONObject> request = new HttpEntity<>(null, headers);
        URI uri = builder.build().encode().toUri();
        ResponseEntity<LoginResult> response = null;
        try {
            response = restTemplate.exchange(uri, HttpMethod.POST, request, LoginResult.class);
        } catch (RestClientException e) {
            e.printStackTrace();
            logger.error("###login error###", e.getMessage());
            throw new RuntimeException(e);
        }
        LoginResult res = response.getBody();
        logger.info("接口初始化：" + res.getMessage() + res.getCode() + res.getData());
        boolean flag = (res != null && "0".equals(res.getCode()));

        return flag;
    }

    public List<DataItem> getRecentData() {
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(dataUrl)
                .queryParam("thirdAppId", thirdAppId)
                .queryParam("licenseCode", licenseCode)
                .queryParam("method", "c_")
                .queryParam("tags", tags);
        HttpEntity<JSONObject> request = new HttpEntity<>(null, headers);
        URI uri = builder.build().encode().toUri();
        logger.info("starting getRecentData...");
        ArrayList<DataItem> list = null;
        try {
            ResponseEntity<ArrayList<DataItem>> response = restTemplate.exchange(uri, HttpMethod.POST, null, new ParameterizedTypeReference<ArrayList<DataItem>>() {
            });
            logger.info("getRecentData done");
            logger.debug("getRecentData：" + response.getBody());

            list = response.getBody();
            return list;
        } catch (RestClientException e) {
            e.printStackTrace();
            logger.error("###login error###", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            return list;
        }
    }

    public List<DataItem> getRecentDataByOkHttp() {
        HttpUrl url = HttpUrl.parse(dataUrl);
        url = url.newBuilder()
                .addQueryParameter("thirdAppId", thirdAppId)
                .addQueryParameter("licenseCode", licenseCode)
                .addQueryParameter("method", "c_")
                .addQueryParameter("tags", tags)
                .build();


        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "java http-reader-xinan")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(new FormBody.Builder().build())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            // 调用parseObject()

            List<DataItem> list = JSON.parseObject(response.body().string(), new TypeReference<ArrayList<DataItem>>() {
            });
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("###getRecentDataByOkHttp error###", e.getMessage());
            return null;
        }
    }

    public WritterResult addMultiTaos(List<Gas> list) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("gasList", list);

        HttpEntity<Map<String, Object>> r = new HttpEntity<>(requestBody, requestHeaders);
        WritterResult result = restTemplate.postForObject(pushMultiUrl, r, WritterResult.class);

        return result;
    }

    private WritterResult addTaos(Gas data) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        RestTemplate restTemplate = new RestTemplate();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = sdf.format(data.getTs());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ts", dateString);
        requestBody.put("point", data.getPoint());
        requestBody.put("pname", data.getPname());
        requestBody.put("unit", data.getUnit());
        requestBody.put("region", data.getRegion());
        requestBody.put("value", data.getValue());

        HttpEntity<Map<String, Object>> r = new HttpEntity<>(requestBody, requestHeaders);

        // 请求服务端添加玩家
        WritterResult result = restTemplate.postForObject(pushUrl, r, WritterResult.class);

        return result;
    }
}
