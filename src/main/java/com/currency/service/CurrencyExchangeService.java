package com.currency.service;

import com.currency.dto.ExchangeRateApiResponse;
import com.currency.model.ExchangeRate;
import com.currency.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class CurrencyExchangeService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/";

    public CurrencyExchangeService(ExchangeRateRepository exchangeRateRepository,
                                 @Value("${exchange.rate.api.key}") String apiKey) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    public double getExchangeRate(String baseCurrency, String targetCurrency) {
        // Check cache first
        ExchangeRate cachedRate = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency)
                .orElse(null);

        if (cachedRate != null && !isRateExpired(cachedRate)) {
            return cachedRate.getRate();
        }

        // Fetch new rate from API
        String url = API_URL + apiKey + "/latest/" + baseCurrency;
        ExchangeRateApiResponse response = restTemplate.getForObject(url, ExchangeRateApiResponse.class);
        
        if (response != null && "success".equals(response.getResult()) && response.getConversionRates() != null) {
            Map<String, Double> rates = response.getConversionRates();
            Double rate = rates.get(targetCurrency);
            
            if (rate == null) {
                throw new RuntimeException("Target currency not found in response");
            }

            // Update cache
            if (cachedRate != null) {
                cachedRate.setRate(rate);
                cachedRate.setLastUpdated(LocalDateTime.now());
                exchangeRateRepository.save(cachedRate);
            } else {
                ExchangeRate newRate = new ExchangeRate();
                newRate.setBaseCurrency(baseCurrency);
                newRate.setTargetCurrency(targetCurrency);
                newRate.setRate(rate);
                exchangeRateRepository.save(newRate);
            }

            return rate;
        }

        throw new RuntimeException("Failed to fetch exchange rate: " + 
            (response != null ? response.getResult() : "Unknown error"));
    }

    private boolean isRateExpired(ExchangeRate rate) {
        return ChronoUnit.HOURS.between(rate.getLastUpdated(), LocalDateTime.now()) >= 1;
    }
} 