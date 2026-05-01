package com.bankpro.util;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BankUtil {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("##,##,##,##0.00");
    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Random RANDOM = new Random();

    public static String formatCurrency(double amount) {
        return "₹" + CURRENCY_FORMAT.format(amount);
    }

    public static String formatCurrencyForeign(double amount, String symbol) {
        return symbol + String.format("%,.2f", amount);
    }

    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(DT_FORMAT);
    }

    public static String formatDate(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(DATE_FORMAT);
    }

    public static String generateCustomerId() {
        return "CUST" + String.format("%08d", System.currentTimeMillis() % 100000000L);
    }

    public static String generateAccountNumber() {
        // 14-digit account number
        long base = System.currentTimeMillis() % 10000000000L;
        int suffix = RANDOM.nextInt(10000);
        return String.format("%010d%04d", base, suffix);
    }

    public static String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + String.format("%04d", RANDOM.nextInt(10000));
    }

    public static String generateLoanId() {
        return "LN" + String.format("%010d", System.currentTimeMillis() % 10000000000L);
    }

    public static String generateFDNumber() {
        return "FD" + String.format("%010d", System.currentTimeMillis() % 10000000000L);
    }

    public static String generateEmployeeId(int id) {
        return String.format("EMP%05d", id);
    }

    public static String generateCardNumber() {
        // Generate Luhn-valid-looking 16-digit card number
        StringBuilder sb = new StringBuilder("4"); // Visa prefix
        for (int i = 0; i < 14; i++) sb.append(RANDOM.nextInt(10));
        sb.append(luhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    private static int luhnCheckDigit(String partial) {
        int sum = 0;
        boolean alternate = false;
        for (int i = partial.length() - 1; i >= 0; i--) {
            int n = partial.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) return cardNumber;
        return "**** **** **** " + cardNumber.substring(12);
    }

    public static String formatCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) return cardNumber;
        return cardNumber.substring(0, 4) + " " + cardNumber.substring(4, 8)
            + " " + cardNumber.substring(8, 12) + " " + cardNumber.substring(12);
    }

    // EMI calculation using standard formula: P * r * (1+r)^n / ((1+r)^n - 1)
    public static double calculateEMI(double principal, double annualRate, int tenureMonths) {
        if (annualRate == 0) return principal / tenureMonths;
        double monthlyRate = annualRate / (12 * 100);
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        return principal * monthlyRate * factor / (factor - 1);
    }

    public static double calculateFDMaturity(double principal, double annualRate, int months) {
        // Quarterly compounding
        double rate = annualRate / 100;
        double quarters = months / 3.0;
        return principal * Math.pow(1 + rate / 4, quarters);
    }

    // Binary search on sorted customer list by customer ID
    public static <T> int binarySearchById(List<T> list, String targetId,
                                            java.util.function.Function<T, String> idExtractor) {
        int low = 0, high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int cmp = idExtractor.apply(list.get(mid)).compareTo(targetId);
            if (cmp == 0) return mid;
            else if (cmp < 0) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    // Binary search by account number
    public static <T> int binarySearchByAccount(List<T> list, String targetAcct,
                                                  java.util.function.Function<T, String> acctExtractor) {
        return binarySearchById(list, targetAcct, acctExtractor);
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[6-9]\\d{9}$");
    }

    public static boolean isValidPAN(String pan) {
        return pan != null && pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    }

    public static boolean isValidAadhar(String aadhar) {
        return aadhar != null && aadhar.matches("^\\d{12}$");
    }

    public static boolean isValidPincode(String pincode) {
        return pincode != null && pincode.matches("^\\d{6}$");
    }

    public static boolean isValidAmount(String amountStr) {
        try {
            double amt = Double.parseDouble(amountStr);
            return amt > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String generateExpiryDate() {
        LocalDateTime expiry = LocalDateTime.now().plusYears(4);
        return String.format("%02d/%02d", expiry.getMonthValue(), expiry.getYear() % 100);
    }

    public static String generateCVV() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }
}
