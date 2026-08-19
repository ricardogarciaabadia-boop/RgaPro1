package com.rgapro1.ocaso;

import android.content.Context;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Offline-first secure sharing. Private keys never leave the recipient device. */
public final class SecureShareManager {
    private static final String PREF="rgapro_secure_share";
    private static final String PRIV="private_key";
    private static final String PUB="public_key";
    private SecureShareManager(){}

    public static JSONObject ensureIdentity(Context c,String username)throws Exception{
        android.content.SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        if(!p.contains(PRIV)||!p.contains(PUB)){
            KeyPairGenerator g=KeyPairGenerator.getInstance("RSA"); g.initialize(3072);
            KeyPair kp=g.generateKeyPair();
            p.edit().putString(PRIV,b64(kp.getPrivate().getEncoded())).putString(PUB,b64(kp.getPublic().getEncoded())).apply();
        }
        return new JSONObject().put("username",username).put("publicKey",p.getString(PUB,""));
    }

    public static String exportRegistration(Context c,String username,String invite)throws Exception{
        JSONObject o=ensureIdentity(c,username); o.put("invite",invite).put("version",1); return o.toString();
    }

    public static String encryptForRecipient(Context c,JSONObject payload,String recipientPublicKey,String sender)throws Exception{
        KeyGenerator kg=KeyGenerator.getInstance("AES"); kg.init(256); SecretKey aes=kg.generateKey();
        byte[] iv=new byte[12]; new java.security.SecureRandom().nextBytes(iv);
        Cipher a=Cipher.getInstance("AES/GCM/NoPadding"); a.init(Cipher.ENCRYPT_MODE,aes,new GCMParameterSpec(128,iv));
        JSONObject body=new JSONObject(payload.toString()); body.put("sharedBy",sender).put("securityVersion",1);
        byte[] cipher=a.doFinal(body.toString().getBytes(StandardCharsets.UTF_8));
        PublicKey pub=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(recipientPublicKey,Base64.DEFAULT)));
        Cipher r=Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"); r.init(Cipher.ENCRYPT_MODE,pub);
        JSONObject out=new JSONObject(); out.put("type","RGAPRO_SECURE_SHARE").put("version",1).put("sender",sender).put("iv",b64(iv)).put("key",b64(r.doFinal(aes.getEncoded()))).put("data",b64(cipher)); return out.toString();
    }

    public static JSONObject decrypt(Context c,String packageText)throws Exception{
        JSONObject in=new JSONObject(packageText); if(!"RGAPRO_SECURE_SHARE".equals(in.optString("type")))throw new IllegalArgumentException("Paquete RgaPro no válido");
        android.content.SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE); String raw=p.getString(PRIV,""); if(raw.isEmpty())throw new IllegalStateException("Este dispositivo no tiene identidad segura");
        PrivateKey priv=KeyFactory.getInstance("RSA").generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Base64.decode(raw,Base64.DEFAULT)));
        Cipher r=Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"); r.init(Cipher.DECRYPT_MODE,priv); byte[] aes=r.doFinal(Base64.decode(in.getString("key"),Base64.DEFAULT));
        Cipher a=Cipher.getInstance("AES/GCM/NoPadding"); a.init(Cipher.DECRYPT_MODE,new SecretKeySpec(aes,"AES"),new GCMParameterSpec(128,Base64.decode(in.getString("iv"),Base64.DEFAULT)));
        return new JSONObject(new String(a.doFinal(Base64.decode(in.getString("data"),Base64.DEFAULT)),StandardCharsets.UTF_8));
    }

    private static String b64(byte[] b){return Base64.encodeToString(b,Base64.NO_WRAP);}
}
