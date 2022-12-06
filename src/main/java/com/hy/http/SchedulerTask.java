package com.hy.http;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hy.http.model.*;
import com.sun.jndi.toolkit.url.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${data-url}")
    private String dataUrl;
    @Value("${UserAuthorityCode}")
    private String UserAuthorityCode;
    @Value("${region}")
    private String region;
    @Value("#{${unit-map}}")
    private Map<String, String> unitMap;
    @Value("${points:}")
    private String[] points;
    private String sep = "_";

    @Scheduled(fixedDelayString = "${interval}")
    public void transferSchedule() {
        logger.info("starting transfer...");
        List<DataItem> list = this.getRecentData();

        Gas g = new Gas();
        g.setRegion(region);

        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            DataItem item = list.get(i);

            g.setTs(item.getTimeStamp());
            g.setPoint(region + sep + item.getTagName());
            g.setPname(region + sep + item.getTagName());
            g.setValue(item.getValue());
            g.setUnit(item.getUnits());
            g.setUnit(unitMap.get(item.getTagName()));

            WritterResult result = this.addTaos(g);
            logger.info(result.getMessage());
        }
    }

    public List<DataItem> getRecentData() {
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(dataUrl)
                .queryParam("UserAuthorityCode", UserAuthorityCode)
                .queryParam("tagsStr", points);
        HttpEntity<JSONObject> request = new HttpEntity<>(null, headers);
        URI uri = builder.build().encode().toUri();
//        logger.info("##########\n" + uri.toString());
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
        String str = response.getBody();
        List<DataItem> list = new ArrayList<>();
//        logger.info("!!!!!!!!!!!!!\n" + str + '\n');
        if (!"".equals(str) && str.length() > 0) {
            str = str.replace('\'', '\"');
            str = str.substring(1, str.length() - 1);
            list = JSON.parseObject(str, new TypeReference<ArrayList<DataItem>>() {
            });
        }

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
