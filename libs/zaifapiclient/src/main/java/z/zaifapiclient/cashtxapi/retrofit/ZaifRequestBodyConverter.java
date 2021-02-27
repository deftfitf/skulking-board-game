package z.zaifapiclient.cashtxapi.retrofit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import retrofit2.Converter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class ZaifRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private static final TypeReference<Map<String, String>> typeReference = new TypeReference<>() {
    };
    private static final String NONCE_FIELD = "nonce";

    private final ObjectMapper mapper;

    ZaifRequestBodyConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RequestBody convert(T value) {
        final Map<String, String> convertedMap = mapper.convertValue(value, typeReference);
        final var bldr = new FormBody.Builder(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entry : convertedMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            final var key = camelCaseToSnakeCase(entry.getKey());
            bldr.add(key, entry.getValue());
        }
        bldr.add(NONCE_FIELD, getNonce());
        return bldr.build();
    }

    private String getNonce() {
        return String.valueOf(Instant.now().toEpochMilli() / 1000.);
    }

    private static String camelCaseToSnakeCase(String camelCaseString) {
        final var b = new StringBuilder();
        for (char c : camelCaseString.toCharArray()) {
            if (Character.isUpperCase(c)) {
                b.append('_');
                b.append(Character.toLowerCase(c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

}
