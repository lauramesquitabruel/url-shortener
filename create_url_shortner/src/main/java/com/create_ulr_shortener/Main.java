package com.create_ulr_shortener;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.amazonaws.services.lambda.runtime.Context;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>>{

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString();

        //mapeia o body da requisição
        Map<String, String> bodyMap;
        try{
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Error processing JSON body: " + exception.getMessage(), exception);
        }

        //divide o body em url original e data de expiração
        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");
        long expirationTimeInSeconds = Long.parseLong(expirationTime);

        //cria id utilizado para identificar cada url encurtada
        String shortUrlCode = java.util.UUID.randomUUID().toString().substring(0, 8);

        //salva os dados extraídos da requisição em um objeto
        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);
        try{
            //transforma o objeto em um JSON
            String urlDataJson = objectMapper.writeValueAsString(urlData);

            //monta o request indicando o bucket e o arquivo
            PutObjectRequest request = PutObjectRequest.builder()
            .bucket("url-shortener-storage-laurabruel")
            .key(shortUrlCode + ".json")
            .build();

            //passa o conteúdo do arquivo
            s3Client.putObject(request, RequestBody.fromString(urlDataJson));
        } catch(Exception e){
            throw new RuntimeException("Error saving data to S3: " + e.getMessage(), e);
        }

        //retorna o código da url como resposta
        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);
        
        return response;
    }
}