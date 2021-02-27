package z.zaifapiclient.cashtxapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import java.io.IOException;

final public class ZaifResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final ObjectMapper mapper;
    private final ObjectReader adapter;
    private static final String SUCCESS_FIELD = "success";
    private static final int SUCCESS_FIELD_VALUE = 1;
    private static final String RESULT_FIELD = "return";
    private static final String ERROR_FIELD = "error";

    ZaifResponseBodyConverter(ObjectMapper mapper, ObjectReader adapter) {
        this.mapper = mapper;
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        try (value) {
            final var node = mapper.readTree(value.bytes());
            final var success = node.get(SUCCESS_FIELD).asInt();
            if (success == SUCCESS_FIELD_VALUE) {
                return adapter.readValue(node.get(RESULT_FIELD));
            }
            final var errorMessage = node.get(ERROR_FIELD).asText();
            throw new RuntimeException("error_code=" + success + " message=" + errorMessage);
        }
    }
}
