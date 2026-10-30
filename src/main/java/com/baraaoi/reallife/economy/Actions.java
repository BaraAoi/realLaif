package com.baraaoi.reallife.economy;

public class Actions {
    public static final int DEPOSIT = 1;
    public static final int WITHDRAW = 2;
    public static final int TAKE_LOAN = 3;
    public static final int REPAY_LOAN = 4;
    public static final int MORTGAGE = 5;
    public static final int REPAY_MORTGAGE = 6;
    public static final int REISSUE_ORDER = 7;
    /** v1.10: купить дом сразу, без ипотеки (arg1 — HouseType). */
    public static final int BUY_HOUSE = 8;

    public static final int MFO_TAKE = 20;
    public static final int MFO_PAY_SLOT = 21;
    public static final int MFO_PAY_BANK = 22;
    public static final int MFO_BUY_BUSINESS = 23;
    public static final int MFO_BUY_LAUNDER = 24;
    public static final int MFO_LAUNDER = 25;
    public static final int MFO_BUY_PRINTER = 26;

    public static final int CASINO_PLAY = 40;

    public static final int PRINTER_SET = 45;
    public static final int CRYPTO_WITHDRAW = 46;

    public static final int BIZ_HIRE = 50;
    public static final int BIZ_LAUNDER = 51;
    public static final int BIZ_FIRE = 52;
    public static final int BIZ_DOCS = 53;

    public static final int PHONE_SYNC = 60;
    public static final int PHONE_WITHDRAW = 61;
    public static final int PHONE_LOAN = 62;
    public static final int PHONE_REPAY = 63;
    public static final int PHONE_WHOLESALE = 64;
    public static final int PHONE_BUY_BIZ = 65;
    public static final int PHONE_SELL_BTC = 66;
    public static final int PHONE_BUY_EQUIP = 67;
    public static final int PHONE_BUY_CRYPTO = 68;
    /** v1.10: лицензия + здание «под ключ» с оборудованием (arg1 — BizType). */
    public static final int PHONE_BUY_TURNKEY = 69;
    /** v1.10: разрешение на строительство торгового центра. */
    public static final int PHONE_BUY_MALL = 70;
    /** Последнее действие телефона (для диапазона). */
    public static final int PHONE_LAST = 70;
}
