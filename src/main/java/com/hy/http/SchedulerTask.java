package com.hy.http;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hy.http.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class SchedulerTask {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerTask.class);
    @Value("${push-url}")
    private String pushUrl;
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

    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() {
        logger.info("starting transfer...");

        if (!isLogin) {
            isLogin = this.login();
            logger.info("isLogin：" + isLogin);
        }

        if (!isLogin) {
            return;
        }

//        logger.info("getRecentData...");

        List<DataItem> list = this.getRecentData();
        Gas g = new Gas();
        g.setRegion(region);

        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            DataItem item = list.get(i);

            g.setTs(item.getTime());
            String tag = item.getTag().substring(1, item.getTag().length());
            g.setPoint(region + sep + tag);
            g.setPname(region + sep + item.getTAGDESC());
            g.setValue(item.getValue());
            g.setUnit(item.getUNIT());

            WritterResult result = this.addTaos(g);
            logger.info(result.getMessage());
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
        ResponseEntity<LoginResult> response = restTemplate.exchange(uri, HttpMethod.POST, request, LoginResult.class);
        LoginResult res = response.getBody();
        logger.info("接口初始化：" + res.getMessage());
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
        ResponseEntity<ArrayList<DataItem>> response = restTemplate.exchange(uri, HttpMethod.POST, null, new ParameterizedTypeReference<ArrayList<DataItem>>() {
        });
        ArrayList<DataItem> list = response.getBody();

        return list;
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
