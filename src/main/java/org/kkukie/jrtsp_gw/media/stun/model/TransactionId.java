package org.kkukie.jrtsp_gw.media.stun.model;

import java.util.Arrays;
import java.util.Random;

public class TransactionId {

    public static final int RFC5389_TRANSACTION_ID_LENGTH = 12;
    public static final int RFC3489_TRANSACTION_ID_LENGTH = 16;

    private final byte[] transactionId;
    private Object applicationData;
    private static final Random random = new Random(System.currentTimeMillis());
    private int hashCode;

    private TransactionId() {
        this(false);
    }

    private TransactionId(boolean rfc3489Compatibility) {
        this.applicationData = null;
        this.hashCode = 0;
        this.transactionId = new byte[rfc3489Compatibility ? 16 : 12];
    }

    public static TransactionId createNewTransactionID() {
        TransactionId tid = new TransactionId();
        generateTransactionID(tid, 12);
        return tid;
    }

    public static TransactionId createNewRFC3489TransactionID() {
        TransactionId tid = new TransactionId(true);
        generateTransactionID(tid, 16);
        return tid;
    }

    private static void generateTransactionID(TransactionId tid, int nb) {
        long left = System.currentTimeMillis();
        long right = random.nextLong();
        int b = nb / 2;

        for(int i = 0; i < b; ++i) {
            tid.transactionId[i] = (byte)((int)(left >> i * 8 & 255L));
            tid.transactionId[i + b] = (byte)((int)(right >> i * 8 & 255L));
        }

        tid.hashCode = (tid.transactionId[3] << 24 & -16777216)
                | (tid.transactionId[2] << 16 & 16711680)
                | (tid.transactionId[1] << 8 & '\uff00')
                | (tid.transactionId[0] & 255);
    }

    public byte[] getBytes() {
        return this.transactionId;
    }

    public boolean isRFC3489Compatible() {
        return this.transactionId.length == 16;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof TransactionId)) {
            return false;
        } else {
            byte[] targetBytes = ((TransactionId)obj).transactionId;
            return Arrays.equals(this.transactionId, targetBytes);
        }
    }

    public boolean equals(byte[] targetID) {
        return Arrays.equals(this.transactionId, targetID);
    }

    public int hashCode() {
        return this.hashCode;
    }

    public String toString() {
        return toString(this.transactionId);
    }

    public static String toString(byte[] transactionID) {
        StringBuilder idStr = new StringBuilder();
        idStr.append("0x");

        for (byte b : transactionID) {
            if ((b & 255) <= 15) {
                idStr.append("0");
            }

            idStr.append(Integer.toHexString(b & 255).toUpperCase());
        }

        return idStr.toString();
    }

    public void setApplicationData(Object applicationData) {
        this.applicationData = applicationData;
    }

    public Object getApplicationData() {
        return this.applicationData;
    }

}
