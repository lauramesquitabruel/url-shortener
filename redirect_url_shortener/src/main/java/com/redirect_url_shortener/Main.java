package com.redirect_url_shortener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>>{
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        final S3Client s3Client = S3Client.builder().build();
        final ObjectMapper objectMapper = new ObjectMapper();

        //extrai somente o UUID da url para fazer as consultar no s3
        String pathParameters = input.get("rawPath").toString();
        String shortUrlCode = pathParameters.replace("/", "");

        //verifica se a url existe
        if(shortUrlCode == null || shortUrlCode.isEmpty()){
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required");
        }

        //monta o request indicando o bucket e o arquivo
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket("url-shortener-storage-laurabruel")
        .key(shortUrlCode + ".json")
        .build();

        //monta o objeto final com as informações recebidas do s3
        InputStream s3ObjectStream;
        //manda a requisição 
        try{
            s3ObjectStream = s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching URL data from S3: " + e.getMessage());
        }

        //transforma o json num objeto java
        UrlData urlData;
        try{
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch (Exception e){
            throw new RuntimeException("Error deserializing URL data: " + e.getMessage());
        }

        long currentTimeInSeconds = System.currentTimeMillis()/1000;

        Map<String, Object> response = new HashMap<>();

        //verifica se a url ainda é válida
        if(currentTimeInSeconds < urlData.getExpirationTime()){
            response.put("statusCode", 302);

            //redireciona até a url original
            Map<String, String> headers = new HashMap<>();
            headers.put("Location", urlData.getOriginalUrl());

            response.put("headers", headers);
        } else {
            //retorna na resposta que a url está expirada
            response.put("statusCode", 410);
            response.put("body", "URL has expired");
        }

        return response;

    }
}