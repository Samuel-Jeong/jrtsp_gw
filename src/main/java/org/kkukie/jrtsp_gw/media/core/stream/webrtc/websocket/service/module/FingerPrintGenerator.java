package org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.module;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class FingerPrintGenerator {

    private static X509Certificate getCertObject(String filePath) throws IOException, CertificateException {
        try (FileInputStream is = new FileInputStream(filePath)) {
            CertificateFactory certificateFactory = CertificateFactory
                    .getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(is);
        }
    }

    public static String getFingerPrint(String filePath) throws NoSuchAlgorithmException, CertificateException, IOException {
        X509Certificate certObject = getCertObject(filePath);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(certObject.getEncoded());

        String rawFingerPrint = DatatypeConverter.printHexBinary(md.digest()).toUpperCase();
        String[] split = rawFingerPrint.split("");

        int count = 0;
        StringBuilder newFingerPrint = new StringBuilder();
        for (String c : split) {
            if (count > 0 && count % 2 == 0) {
                newFingerPrint.append(":");
            }
            newFingerPrint.append(c);
            count++;
        }

        return newFingerPrint.toString();
    }

}


