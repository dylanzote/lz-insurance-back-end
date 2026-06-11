package com.lz_Insurance.core.request;

import org.springframework.util.MultiValueMap;

import java.util.Map;

public interface HttpService {

    <T> T get(String url, Class<T> responseType);

    <T> T get(String url, Class<T> responseType, String bearerToken);

    <T> T get(String url, Class<T> responseType, Map<String, String> headers);

    <T> T get(String url, Class<T> responseType, Map<String, String> headers, Map<String, String> queryParams);

    <T, R> T post(String url, R request, Class<T> responseType, Map<String, String> headers);

    <T> T post(String url, MultiValueMap<String, String> requestData, Class<T> responseType);
}
