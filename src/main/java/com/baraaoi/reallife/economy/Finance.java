package com.baraaoi.reallife.economy;

/**
 * Финансовые формулы, общие для сервера и клиента.
 */
public class Finance {
    /** Аннуитетный ежедневный платёж. */
    public static double annuity(double principal, double dailyRate, int days) {
        if (principal <= 0 || days <= 0) return 0;
        if (dailyRate <= 0) return principal / days;
        double k = Math.pow(1 + dailyRate, days);
        return principal * dailyRate * k / (k - 1);
    }

    public static String money(double v) {
        long r = Math.round(v);
        String s = String.valueOf(Math.abs(r));
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            if (++c % 3 == 0 && i > 0) sb.append(' ');
        }
        return (r < 0 ? "-" : "") + sb.reverse().toString() + "\u20BD";
    }

    public static String percent(double rate) {
        double p = rate * 100.0;
        if (Math.abs(p - Math.round(p)) < 0.001) return Math.round(p) + "%";
        return String.format(java.util.Locale.ROOT, "%.1f%%", p);
    }
}
