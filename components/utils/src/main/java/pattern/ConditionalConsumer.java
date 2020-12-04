package pattern;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;

/**
 * 条件消费者
 * */
public class ConditionalConsumer {
    public static Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 尝试性消费。
     * 若满足前置条件则直接消费，否则进行用户指定次数的尝试，每次尝试重新判断前置条件。
     * NOTE: 该方法内部在主线程执行，若有耗时操作，请用户将耗时操作投递到后台线程。
     * @param product 消费品
     * @param precondition 前置条件
     * @param consumer 满足前置条件情况下的消费者
     * @param maxTimesToTry 最大尝试次数。若<=0则无限次数。
     * @param interval 尝试间隔时长。单位：毫秒
     * @param consumerIfFailed 经过最大尝试次数后仍失败时的消费者
     * */
    public static <T> void tryConsume(@NonNull T product, @NonNull Predicate<T> precondition, @NonNull Consumer<T> consumer,
                                      int maxTimesToTry, int interval, @Nullable Consumer<T> consumerIfFailed){
        handler.post(new Runnable() {
            int triedTimes;

            @Override
            public void run() {
                ++triedTimes;
                if (precondition.test(product)){
                    consumer.accept(product);
                }else{
                    if (triedTimes == maxTimesToTry){
                        if (consumerIfFailed != null) consumerIfFailed.accept(product);
                    }else{
                        handler.postDelayed(this, interval);
                    }
                }
            }
        });
    }

}
