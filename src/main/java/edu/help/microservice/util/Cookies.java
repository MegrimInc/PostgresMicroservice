package edu.help.microservice.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class Cookies 
{


    private static final String[] SIGNATURE_ALGORITHMS = {
      "SHA256withRSA"
    };
    
    
    
    // DO NOT COPY AND PASTE THIS ANYWHERE OR EXPOSE THIS 
    private static String privKey = "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";

    // This one is fine to expose.
    private static String pubKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAiTmHSt2J0B2FrXf1lozenAF5qwyzzEv8wpSDa6HLTvZOVTWu71GXusgNoPjurjKDMmrflB62V5YMjTOTLdkJOir45XZJHbYudvATITaP/c3ySnoSkNDmvQpkMpTWkEq6jTx29f4kvc6PAz2Gg0IIeuXNKZW0SHP9qe4WjZshj/bEeye5rpnelOBSkpnPrDyNiz/SMMNggHGpgkc74+h30jC2dYwCqaV22GtXFQlNyK9Ai35OlGnKitcjoP7de+qQ2NQrySoFN81WBNSl+ODFl/MNju5PtHtYQ50IR24SpdzGEZLwkfQgmMc81VtvDZZ02zkJBH5dn6DlfGr/9zJOiLyQBd4U4rMSXFRv+SFMK9qzJyfFZIYpzV6v9VlfCDIRC7Hd6f4CY3VCbM5LdiUsr+jRJFYs6gkWlgGbPa525uGaptel7Af4vWNITYsYv+idUc4N6jlDc/5TkXszgFUSjaaXFSPjMxvejXNbdiI2GQ+HjZZwt3nx1C6LvdUpZJ0/W/XIkLWgTA6v5QBeiWJfVNeolGqc0SmnGny4+lqc/4RqCosPpH8VffhR4oBi5I//lQvnKvVuMHgfh+d1wLeGoWKizaqkLgH11R5XMHxXoFpPhjNGU55sEXlls7ijDZ9znhulpi1u9A4Z0krZPuZnWyIZx8l+ZgujG5fEG7+xQXkCAwEAAQ==";
    
    public static String generateSignature(String str ) {

        try {
            PrivateKey privateKeyForSigning = getPrivateKey();
            String signature = signText(str, privateKeyForSigning);

            return signature;
        } catch (Exception e) {
            System.out.println("Critical failure in generateSignature");
            e.printStackTrace();
            return null;
        }
    }
    

    public static boolean validateSignature( String txt, String signature ) {
        try {
            PublicKey publicKeyForVerification = getPublicKey();
            String result = tryOneSignatureAlgorithm(txt, signature, publicKeyForVerification);
            System.out.println(result);

            return true;
        } catch (Exception e) {
            System.out.println("Critical failure in verifySignature");
            e.printStackTrace();
            return false;
        }
        
    }


    public static Integer getIdFromCookie(String authCookie) {
        try {
            if (authCookie == null || authCookie.isEmpty()) return -1;

            String decoded = new String(Base64.getDecoder().decode(authCookie), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");

            if (parts.length != 3) return -2;

            String id = parts[0];
            String expiry = parts[1];
            String signature = parts[2];
            String signedData = id + "." + expiry;

            if (System.currentTimeMillis() > Long.parseLong(expiry)) {
                System.out.println("Cookie expired");
                return -1;
            }
            if (!validateSignature(signedData, signature)) {
                System.out.println("Invalid signature");
                return -3;
            }

            return Integer.parseInt(id);
        } catch (Exception e) {
            e.printStackTrace();
            return -4;
        }
    }


    private static PrivateKey getPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }


    private static String signText(String text, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(text.getBytes());
        byte[] digitalSignature = signature.sign();
        return Base64.getEncoder().encodeToString(digitalSignature);
    }



    private static PublicKey getPublicKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(pubKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }



    // Try all signature algorithms, report success if any
    private static String tryOneSignatureAlgorithm(String text, String signatureStr, PublicKey publicKey) {
        for (String algorithm : SIGNATURE_ALGORITHMS) {
            try {
                if (verifySignature(text, signatureStr, publicKey, algorithm)) {
                    return "Signature verified successfully using: " + algorithm;
                }
            } catch (Exception ignored) {
            }
        }
        return "Signature verification failed with all known algorithms.";
    }


    private static boolean verifySignature(String text, String signatureStr, PublicKey publicKey, String algorithm) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey);
        signature.update(text.getBytes());
        byte[] digitalSignature = Base64.getDecoder().decode(signatureStr);
        return signature.verify(digitalSignature);
    }
}