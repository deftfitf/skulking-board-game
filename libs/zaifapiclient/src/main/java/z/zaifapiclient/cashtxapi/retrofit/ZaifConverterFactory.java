package z.zaifapiclient.cashtxapi.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public final class ZaifConverterFactory extends Converter.Factory {

    private final ObjectMapper mapper;

    public static ZaifConverterFactory create() {
        return create(new ObjectMapper());
    }

    public static ZaifConverterFactory create(@NonNull ObjectMapper mapper) {
        return new ZaifConverterFactory(mapper);
    }

    private ZaifConverterFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        final var javaType = mapper.getTypeFactory().constructType(type);
        final var reader = mapper.readerFor(javaType);
        return new ZaifResponseBodyConverter<>(mapper, reader);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        return new ZaifRequestBodyConverter<>(mapper);
    }

}
