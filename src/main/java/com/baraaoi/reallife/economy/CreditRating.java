package com.baraaoi.reallife.economy;

/**
 * Кредитный рейтинг (300..850) и «рентабельность» клиента.
 *
 * Банк смотрит на:
 *  - платёжную дисциплину (платежи вовремя / дни просрочки / дефолты);
 *  - накопления на счёте;
 *  - доход (сколько всего внесено в банк) и средний доход в день;
 *  - возраст счёта;
 *  - наличие микрозаймов (банк их не любит) и текущей просрочки.
 */
public class CreditRating {

    /** Рейтинг без ручной поправки и без ограничения 300..850. */
    private static double base(Account a, long today) {
        double s = 500;
        s += Math.min(150, a.onTimePayments * 5);
        s -= Math.min(300, a.lateDaysTotal * 15);
        s -= a.defaults * 200;
        s += Math.min(100, Math.log10(1 + Math.max(0, a.balance)) * 25);
        s += Math.min(80, a.lifetimeDeposited / 250.0);
        s += Math.min(50, accountAge(a, today) * 2);
        if (a.hasMfo()) s -= 40;
        if (a.arrears > 0.01) s -= 60;
        return s;
    }

    public static int score(Account a, long today) {
        return (int) Math.max(300, Math.min(850, Math.round(base(a, today) + a.ratingBonus)));
    }

    /** Какая поправка нужна, чтобы рейтинг стал равен target. */
    public static int bonusFor(Account a, long today, int target) {
        return (int) Math.round(target - base(a, today));
    }

    public static long accountAge(Account a, long today) {
        return a.createdDay < 0 ? 0 : Math.max(0, today - a.createdDay);
    }

    public static String category(int score) {
        if (score < 450) return "§4Очень плохой";
        if (score < 550) return "§cНизкий";
        if (score < 650) return "§eСредний";
        if (score < 750) return "§aХороший";
        return "§2Отличный";
    }

    /** Средний доход в день — для расчёта платёжеспособности (ПДН). */
    public static double avgDailyIncome(Account a, long today) {
        return a.lifetimeDeposited / Math.max(3, accountAge(a, today) + 1);
    }

    /** Дневная ставка по кредиту. */
    public static double loanRate(int score) {
        if (score >= 750) return 0.005;
        if (score >= 650) return 0.01;
        if (score >= 550) return 0.015;
        return 0.025;
    }

    /** Дневная ставка по ипотеке — в два раза ниже. */
    public static double mortgageRate(int score) {
        return loanRate(score) / 2.0;
    }

    /** Максимальная сумма кредита. 0 = отказ. */
    public static double maxLoan(Account a, long today) {
        int sc = score(a, today);
        if (sc < 450 || a.arrears > 0.01 || a.captivity != Account.FREE) return 0;
        double base = 200 + a.lifetimeDeposited * 0.3 + Math.max(0, a.balance) * 0.5;
        double limit = base * (sc - 400) / 150.0;
        return Math.max(0, Math.floor(limit / 10) * 10);
    }

    /** Допустимая сумма всех ежедневных платежей (показатель долговой нагрузки). */
    public static double maxDailyBurden(Account a, long today) {
        return Math.max(25, avgDailyIncome(a, today) * 0.6);
    }
}
