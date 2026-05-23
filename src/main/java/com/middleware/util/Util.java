package com.middleware.util;

import com.middleware.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class Util {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + timestamp + "-" + unique;
    }

    public static String normalizePhoneNumber(String phoneNumber) {

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new ApiException("Phone number is required", HttpStatus.BAD_REQUEST);
        }

        phoneNumber = phoneNumber
                .replaceAll("\\s+", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (phoneNumber.startsWith("+")) {
            phoneNumber = phoneNumber.substring(1);
        }

        if (phoneNumber.startsWith("0")) {
            phoneNumber = "234" + phoneNumber.substring(1);
        }

        if (!phoneNumber.matches("^234[789][01]\\d{8}$")) {
            throw new ApiException("Invalid Nigerian phone number", HttpStatus.BAD_REQUEST);
        }

        return phoneNumber;
    }

    public String generateOtp() {
        int bound = (int) Math.pow(10, 6);
        int min = bound / 10;
        int otp = min + secureRandom.nextInt(bound - min);
        return String.valueOf(otp);
    }
}
