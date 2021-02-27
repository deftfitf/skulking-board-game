package z.zaifapiclient.cashtxapi.retrofit;

import okhttp3.Interceptor;
import okhttp3.Response;
import okio.Buffer;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ZaifRequestInterceptor implements Interceptor {

    private static final String API_KEY_HEADER = "key";
    private static final String SIGN_HEADER = "sign";
    private static final String SIGN_ALGORITHM = "HmacSHA512";
    private final String apiKey;
    private final SecretKeySpec secretKeySpec;

    public ZaifRequestInterceptor(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                SIGN_ALGORITHM);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final var request = chain.request();
        if (request.body() == null) {
            return chain.proceed(request);
        }

        final var requestBodyBuffer = new Buffer();
        request.body().writeTo(requestBodyBuffer);
        final var signature = sign(requestBodyBuffer.readByteArray());

        return chain.proceed(request.newBuilder()
                .addHeader(API_KEY_HEADER, apiKey)
                .addHeader(SIGN_HEADER, signature)
                .build());
    }

    private String sign(byte[] inputBytes) {
        try {
            final var mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(secretKeySpec);
            final var signed = mac.doFinal(inputBytes);
            return Hex.encodeHexString(signed);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }


}
