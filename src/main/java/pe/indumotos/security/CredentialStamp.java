package pe.indumotos.security;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class CredentialStamp {
    private CredentialStamp() {}
    public static String of(String encoded) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    public static boolean matches(String stamp,String encoded) {
        return stamp!=null && MessageDigest.isEqual(stamp.getBytes(StandardCharsets.UTF_8),of(encoded).getBytes(StandardCharsets.UTF_8));
    }
}
