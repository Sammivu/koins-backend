package com.middleware.util;

import com.middleware.security.CryptoUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Converter
@RequiredArgsConstructor
public class BalanceEncryptionConverter implements AttributeConverter<BigDecimal, String> {

    private final CryptoUtil cryptoUtil;

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {

        if (attribute == null) {
            return null;
        }
        return cryptoUtil.encrypt(attribute.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {

        if (dbData == null) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(cryptoUtil.decrypt(dbData));
    }
}