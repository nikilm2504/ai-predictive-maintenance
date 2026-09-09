package com.pmp.predictivemaintenance.prediction.client;

import com.pmp.predictivemaintenance.prediction.client.dto.ExtractFeaturesRequest;
import com.pmp.predictivemaintenance.prediction.client.dto.ExtractFeaturesResponse;
import com.pmp.predictivemaintenance.prediction.client.dto.PredictRequest;
import com.pmp.predictivemaintenance.prediction.client.dto.PredictResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MlPredictionClient {

    private final RestClient restClient;

    public MlPredictionClient(@Value("${ml.service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ExtractFeaturesResponse extractFeatures(ExtractFeaturesRequest request) {
        return restClient.post()
                .uri("/features/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ExtractFeaturesResponse.class);
    }

    public PredictResponse predict(PredictRequest request) {
        return restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PredictResponse.class);
    }
}
