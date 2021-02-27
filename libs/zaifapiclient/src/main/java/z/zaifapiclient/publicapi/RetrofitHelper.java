package z.zaifapiclient.publicapi;

import lombok.experimental.UtilityClass;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class RetrofitHelper {

    public static <T> CompletableFuture<T> toFuture(Call<T> call) {
        final var future = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (mayInterruptIfRunning) {
                    call.cancel();
                }
                return super.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return call.isCanceled();
            }

        };

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) {
                    future.complete(response.body());
                } else {
                    future.completeExceptionally(new HttpException(response));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

}
