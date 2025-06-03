package com.example.currency.service;

import com.example.currency.cache.SimpleCache;
import com.example.currency.client.NbrbApiClient;
import com.example.currency.models.CurrencyInfo;
import com.example.currency.models.CurrencyRate;
import com.example.currency.repository.CurrencyInfoRepository;
import com.example.currency.repository.CurrencyRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrencyServiceTest {

    @Mock
    private CurrencyInfoRepository currencyInfoRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private NbrbApiClient apiClient;

    @Mock
    private SimpleCache cacheService;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    public void testGetCurrencyRateByAbbreviationFoundInCache() {
        String abbreviation = "USD";
        CurrencyRate cachedRate = new CurrencyRate();
        cachedRate.setCurOfficialRate(new BigDecimal("1.0"));
        cachedRate.setDate(LocalDate.now());
        when(cacheService.get("rateByAbbreviation:USD:" + LocalDate.now())).thenReturn(Optional.of(cachedRate));

        CurrencyRate result = currencyService.getCurrencyRateByAbbreviation(abbreviation);

        assertEquals(cachedRate, result);
        verify(currencyInfoRepository, never()).findByCurAbbreviation(anyString());
        verify(apiClient, never()).getCurrencyRate(anyInt());
    }

    @Test
    public void testGetCurrencyRateByAbbreviationFoundInDb() {
        String abbreviation = "USD";
        CurrencyInfo currency = new CurrencyInfo();
        currency.setCurId(1);
        currency.setCurAbbreviation(abbreviation);
        CurrencyRate rate = new CurrencyRate();
        rate.setCurOfficialRate(new BigDecimal("1.0"));
        rate.setDate(LocalDate.now());
        currency.setRates(Arrays.asList(rate));

        when(cacheService.get("rateByAbbreviation:USD:" + LocalDate.now())).thenReturn(Optional.empty());
        when(currencyInfoRepository.findByCurAbbreviation(abbreviation)).thenReturn(Optional.of(currency));

        CurrencyRate result = currencyService.getCurrencyRateByAbbreviation(abbreviation);

        assertEquals(rate, result);
        verify(cacheService).put("rateByAbbreviation:USD:" + LocalDate.now(), rate);
        verify(apiClient, never()).getCurrencyRate(anyInt());
    }

    @Test
    public void testGetCurrencyRateByAbbreviationFromApi() {
        String abbreviation = "USD";
        CurrencyInfo currency = new CurrencyInfo();
        currency.setCurId(1);
        currency.setCurAbbreviation(abbreviation);
        currency.setRates(new ArrayList<>());
        CurrencyRate rate = new CurrencyRate();
        rate.setCurOfficialRate(new BigDecimal("1.0"));
        rate.setDate(LocalDate.now());

        when(cacheService.get("rateByAbbreviation:USD:" + LocalDate.now())).thenReturn(Optional.empty());
        when(currencyInfoRepository.findByCurAbbreviation(abbreviation)).thenReturn(Optional.of(currency));
        when(apiClient.getCurrencyRate(1)).thenReturn(rate);
        when(currencyRateRepository.save(any(CurrencyRate.class))).thenReturn(rate);

        CurrencyRate result = currencyService.getCurrencyRateByAbbreviation(abbreviation);

        assertEquals(rate, result);
        verify(cacheService).put("rateByAbbreviation:USD:" + LocalDate.now(), rate);
        verify(currencyRateRepository).save(rate);
    }

    @Test
    public void testGetCurrencyRateByAbbreviationNotFound() {
        String abbreviation = "XYZ";
        when(cacheService.get("rateByAbbreviation:XYZ:" + LocalDate.now())).thenReturn(Optional.empty());
        when(currencyInfoRepository.findByCurAbbreviation(abbreviation)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                currencyService.getCurrencyRateByAbbreviation(abbreviation));
        assertEquals("Currency not found for abbreviation: XYZ", exception.getMessage());
    }
}