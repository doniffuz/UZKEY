// ============================================================================
// DIQQAT: BU KOTLIN TESTLARI EMAS.
// Bu ClipBackup.kt algoritmining Java'dagi QO'LDA YOZILGAN MA'LUMOT NUSXASI (reference port)
// va uni tekshiruvchi mustaqil asosiy dastur. Maqsad — kompilyatorsiz muhitda ALGORITM va format
// mantig'ini (fixture'lar, AAD, chegaralar, NFC, buzilgan fayllar) JVM'da ishlatib ko'rish.
// U ClipBackup.kt ni KOMPILYATSIYA QILMAYDI va Kotlin kodining to'g'riligini KAFOLATLAMAYDI:
// port va Kotlin orasida farq bo'lishi mumkin. Haqiqiy tekshiruv: ./gradlew testDebugUnitTest.
// Ishga tushirish (JDK 17+):  java tools/reference-port/ClipBackupPort.java
// ============================================================================
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ClipBackupPort {
    static final byte[] M1 = "OZKB1".getBytes(StandardCharsets.US_ASCII);
    static final byte[] M2 = "OZKB2".getBytes(StandardCharsets.US_ASCII);
    static final int ITER_V1 = 120_000, ITER_V2 = 600_000, MIN_ITER = 100_000, MAX_ITER = 2_000_000;
    static boolean forceManual = false; // API 23–25 holatini taqlid qilish

    static boolean isAscii(String s) { for (char c : s.toCharArray()) if (c >= 0x80) return false; return true; }
    static String normalizePw(String p) { return isAscii(p) ? p : Normalizer.normalize(p, Normalizer.Form.NFC); }
    static void wipe(byte[] a) { Arrays.fill(a, (byte) 0); }
    static byte[] i2b(int v) { return new byte[]{(byte)(v>>>24),(byte)(v>>>16),(byte)(v>>>8),(byte)v}; }
    static int b2i(byte[] d, int o) { return ((d[o]&0xFF)<<24)|((d[o+1]&0xFF)<<16)|((d[o+2]&0xFF)<<8)|(d[o+3]&0xFF); }
    static byte[] cat(byte[]... p) { int n=0; for (byte[] x:p) n+=x.length; byte[] r=new byte[n]; int o=0; for (byte[] x:p){System.arraycopy(x,0,r,o,x.length);o+=x.length;} return r; }
    static boolean starts(byte[] d, byte[] m) { if (d.length < m.length) return false; for (int i=0;i<m.length;i++) if (d[i]!=m[i]) return false; return true; }

    static byte[] pbkdf2Sha256Manual(byte[] pw, byte[] salt, int iter, int outLen) throws GeneralSecurityException {
        if (pw.length == 0) throw new IllegalArgumentException("empty password");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(pw, "HmacSHA256"));
        int hLen = mac.getMacLength(), blocks = (outLen + hLen - 1) / hLen;
        byte[] out = new byte[outLen];
        for (int b = 1; b <= blocks; b++) {
            mac.update(salt); mac.update(i2b(b));
            byte[] u = mac.doFinal(); byte[] t = u.clone();
            for (int i = 1; i < iter; i++) {
                byte[] next = mac.doFinal(u); wipe(u); u = next;
                for (int j = 0; j < t.length; j++) t[j] ^= u[j];
            }
            int off = (b - 1) * hLen;
            System.arraycopy(t, 0, out, off, Math.min(hLen, outLen - off));
            wipe(u); wipe(t);
        }
        return out;
    }

    static SecretKeySpec deriveSha256(String pw, byte[] salt, int iter) throws GeneralSecurityException {
        byte[] sys = null;
        if (!forceManual && isAscii(pw)) {
            char[] ch = pw.toCharArray();
            try {
                PBEKeySpec spec = new PBEKeySpec(ch, salt, iter, 256);
                try { sys = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
                catch (GeneralSecurityException e) { sys = null; }
                finally { spec.clearPassword(); }
            } finally { Arrays.fill(ch, '\0'); }
        }
        byte[] raw = sys;
        if (raw == null) { byte[] pb = pw.getBytes(StandardCharsets.UTF_8); try { raw = pbkdf2Sha256Manual(pb, salt, iter, 32); } finally { wipe(pb); } }
        try { return new SecretKeySpec(raw, "AES"); } finally { wipe(raw); }
    }

    static SecretKeySpec deriveSha1(String pw, byte[] salt, int iter) throws GeneralSecurityException {
        char[] ch = pw.toCharArray(); PBEKeySpec spec = new PBEKeySpec(ch, salt, iter, 256);
        try { byte[] raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
              try { return new SecretKeySpec(raw, "AES"); } finally { wipe(raw); } }
        finally { spec.clearPassword(); Arrays.fill(ch, '\0'); }
    }

    static byte[] encryptV2(String json, String pw, int iter) throws Exception {
        if (iter < MIN_ITER || iter > MAX_ITER) throw new IllegalArgumentException("iterations");
        SecureRandom r = new SecureRandom(); byte[] salt = new byte[16], iv = new byte[12]; r.nextBytes(salt); r.nextBytes(iv);
        byte[] header = cat(M2, i2b(iter), salt); byte[] plain = json.getBytes(StandardCharsets.UTF_8);
        try { Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
              c.init(Cipher.ENCRYPT_MODE, deriveSha256(normalizePw(pw), salt, iter), new GCMParameterSpec(128, iv));
              c.updateAAD(header); return cat(header, iv, c.doFinal(plain)); }
        finally { wipe(plain); }
    }
    static byte[] encrypt(String json, String pw) throws Exception {
        if (pw.isEmpty()) throw new IllegalArgumentException("empty password");
        return encryptV2(json, pw, ITER_V2);
    }

    static String tryV2(String pw, byte[] salt, int iter, byte[] iv, byte[] header, byte[] ct) {
        try { Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
              c.init(Cipher.DECRYPT_MODE, deriveSha256(pw, salt, iter), new GCMParameterSpec(128, iv));
              c.updateAAD(header); byte[] p = c.doFinal(ct);
              try { return new String(p, StandardCharsets.UTF_8); } finally { wipe(p); }
        } catch (GeneralSecurityException e) { return null; }
    }
    static String decryptV2(byte[] d, String pw) {
        int hl = 5 + 4 + 16; if (d.length < hl + 12 + 16) return null;
        int iter = b2i(d, 5); if (iter < MIN_ITER || iter > MAX_ITER) return null;
        byte[] header = Arrays.copyOfRange(d, 0, hl), salt = Arrays.copyOfRange(d, 9, hl);
        byte[] iv = Arrays.copyOfRange(d, hl, hl + 12), ct = Arrays.copyOfRange(d, hl + 12, d.length);
        String nfc = normalizePw(pw);
        List<String> cands = nfc.equals(pw) ? List.of(pw) : List.of(nfc, pw);
        for (String p : cands) { String o = tryV2(p, salt, iter, iv, header, ct); if (o != null) return o; }
        return null;
    }
    static String decryptV1(byte[] d, String pw) throws Exception {
        if (d.length < 5 + 16 + 12 + 16) return null;
        byte[] salt = Arrays.copyOfRange(d, 5, 21), iv = Arrays.copyOfRange(d, 21, 33), ct = Arrays.copyOfRange(d, 33, d.length);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, deriveSha1(pw, salt, ITER_V1), new GCMParameterSpec(128, iv));
        byte[] p = c.doFinal(ct); try { return new String(p, StandardCharsets.UTF_8); } finally { wipe(p); }
    }
    static String decrypt(byte[] d, String pw) {
        try {
            if (pw.isEmpty()) return null;
            if (starts(d, M2)) return decryptV2(d, pw);
            if (starts(d, M1)) return decryptV1(d, pw);
            return null;
        } catch (Exception e) { return null; }
    }

    // ------------------------------ harness ------------------------------
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) { if (ok) pass++; else { fail++; System.out.println("  FAIL: " + name); } }
    static byte[] b64(String s) { return Base64.getDecoder().decode(s); }

    public static void main(String[] a) throws Exception {
        String json = "{\"v\": 1, \"items\": [{\"text\": \"Salom, dunyo! o\u2018zbek\", \"cat\": \"Ish\"}]}";
        byte[] v1 = b64("T1pLQjEBAgMEBQYHCAkKCwwNDg8QZWZnaGlqa2xtbm9wunYhMt6c48LUcWdr+IWs9mN6/QhNKyNt9DuR5NX771T4qEjA4G2LGG8/OvLs7aK4IX4PuSqWw3g0OXoDIyTC5nJN4mltSzkEJvBIr8kbQGzm0ckKWA==");
        byte[] v2 = b64("T1pLQjIACSfAFRYXGBkaGxwdHh8gISIjJDM0NTY3ODk6Ozw9PkEjIMbyprQIfOUwXgIU7OyFLG246csi18J/Ii5TChCvxP9iRxy91Xhbo0xxeI5QPkZvdM7J+/MC0JY6HLZ2/m/7VrMH4WoaIU26DoPeStn2F/FYM6k=");
        byte[] v2u = b64("T1pLQjIAAYagUFFSU1RVVldYWVpbXF1eX3h5ent8fX5/gIGCg1hacTiXp6NGsrJ5w4gVSlbOm4VflWKNt4eu9qIRoSrK9rV4TeEBvX8MexfFxEieMQ0nr770sDTllgHZ06BMnPdp2eMCPnd8YSMeY/RtLE/4UIZ6jBw=");
        String nfc = "caf\u00e9-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c", nfd = "cafe\u0301-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c";

        for (int mode = 0; mode < 2; mode++) {
            forceManual = (mode == 1);
            String tag = forceManual ? "[manual/API23-25]" : "[system]";
            System.out.println("== " + tag);
            check(tag + " v1 fixture opens", json.equals(decrypt(v1, "eski-parol-123")));
            check(tag + " v2 fixture opens", json.equals(decrypt(v2, "yangi-parol-456")));
            check(tag + " v2 unicode fixture NFC", json.equals(decrypt(v2u, nfc)));
            check(tag + " v2 unicode fixture NFD", json.equals(decrypt(v2u, nfd)));
            check(tag + " v2 unicode wrong pw", decrypt(v2u, "cafe-o\u2018zbek-\u043f\u0430\u0440\u043e\u043b\u044c") == null);
            check(tag + " wrong pw v1", decrypt(v1, "x") == null);
            check(tag + " wrong pw v2", decrypt(v2, "x") == null);
            check(tag + " empty pw", decrypt(v2, "") == null);
            byte[] n = encrypt(json, "yangi-parol-456");
            check(tag + " roundtrip v2 + header", json.equals(decrypt(n, "yangi-parol-456")) && starts(n, M2) && b2i(n, 5) == ITER_V2);
            byte[] u = encryptV2(json, nfd, MIN_ITER);
            check(tag + " NFD-enc opens with NFC", json.equals(decrypt(u, nfc)) && json.equals(decrypt(u, nfd)));
            check(tag + " two encryptions differ", !Arrays.equals(encryptV2(json, "abcdef", MIN_ITER), encryptV2(json, "abcdef", MIN_ITER)));
        }
        forceManual = false;
        System.out.println("== tamper/bounds");
        byte[] pwv = v2.clone();
        for (int i : new int[]{8, 12, 40, v2.length - 1}) { byte[] f = v2.clone(); f[i] ^= 1; check("flip v2[" + i + "]", decrypt(f, "yangi-parol-456") == null); }
        for (int i : new int[]{20, 40, v1.length - 1}) { byte[] f = v1.clone(); f[i] ^= 1; check("flip v1[" + i + "]", decrypt(f, "eski-parol-123") == null); }
        for (int len : new int[]{0, 1, 4, 5, 8, 9, 20, 32, 40, 44, 48, v2.length - 1}) check("trunc v2 " + len, decrypt(Arrays.copyOf(v2, len), "yangi-parol-456") == null);
        for (int len : new int[]{0, 3, 5, 20, 33, 44, v1.length - 1}) check("trunc v1 " + len, decrypt(Arrays.copyOf(v1, len), "eski-parol-123") == null);
        check("garbage", decrypt("hello world, this is not a backup file at all".getBytes(), "x") == null && decrypt(new byte[200], "x") == null);
        long t0 = System.nanoTime();
        for (int it : new int[]{Integer.MIN_VALUE, -1, 0, 1, MIN_ITER - 1, MAX_ITER + 1, Integer.MAX_VALUE}) {
            byte[] f = v2.clone(); byte[] ib = i2b(it); System.arraycopy(ib, 0, f, 5, 4);
            check("iter bound " + it, decrypt(f, "yangi-parol-456") == null);
        }
        check("bounds checked before KDF (<2s)", (System.nanoTime() - t0) / 1_000_000 < 2000);
        // AAD: AAD'siz ochilmasligi kerak
        SecretKeySpec k = deriveSha256("yangi-parol-456", Arrays.copyOfRange(v2, 9, 25), ITER_V2);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(128, Arrays.copyOfRange(v2, 25, 37)));
        boolean failed = false; try { c.doFinal(Arrays.copyOfRange(v2, 37, v2.length)); } catch (GeneralSecurityException e) { failed = true; }
        check("no-AAD decrypt fails", failed);
        boolean t1 = false, t2 = false;
        try { encrypt(json, ""); } catch (IllegalArgumentException e) { t1 = true; }
        try { encryptV2(json, "abcdef", MAX_ITER + 1); } catch (IllegalArgumentException e) { t2 = true; }
        check("encrypt rejects empty pw / bad iter", t1 && t2);
        // manual PBKDF2 vs RFC 7914 and system
        byte[] rfc = pbkdf2Sha256Manual("passwd".getBytes(), "salt".getBytes(), 1, 64);
        StringBuilder sb = new StringBuilder(); for (int i = 0; i < 16; i++) sb.append(String.format("%02x", rfc[i]));
        check("RFC7914 vector", sb.toString().equals("55ac046e56e3089fec1691c22544b605"));
        byte[] salt = new byte[16]; for (int i = 0; i < 16; i++) salt[i] = (byte) i;
        byte[] sys = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec("sinov-parol".toCharArray(), salt, 5000, 256)).getEncoded();
        check("manual == system", Arrays.equals(sys, pbkdf2Sha256Manual("sinov-parol".getBytes(), salt, 5000, 32)));
        byte[] pwArr = "sinov-parol".getBytes(), cp = pwArr.clone(); pbkdf2Sha256Manual(pwArr, salt, 10, 32);
        check("manual keeps caller array", Arrays.equals(pwArr, cp));

        // indikativ vaqt (BU SANDBOX JVM — real telefon EMAS)
        for (boolean man : new boolean[]{false, true}) {
            forceManual = man; long s = System.nanoTime(); deriveSha256("abcdef12", salt, ITER_V2);
            System.out.println("sandbox JVM 600k PBKDF2-SHA256 " + (man ? "manual Mac" : "system") + ": " + (System.nanoTime() - s) / 1_000_000 + " ms (telefonda ko'p marta sekinroq bo'lishi mumkin)");
        }
        System.out.println("PORT RESULT: pass=" + pass + " fail=" + fail);
        System.exit(fail == 0 ? 0 : 1);
    }
}
