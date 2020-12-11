package pattern;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;
import com.annimon.stream.function.Supplier;

import java.util.HashSet;
import java.util.Set;


public class ConsumerHelper {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Set<Order<?>> orders = new HashSet<>();



    /**
     * 尽力消费
     * @see #tryConsume(Object, Supplier, Predicate, Consumer, Runnable, int, int, int)
     * */
    public static <T> int tryConsume(@NonNull Object tag,
                                     @NonNull Supplier<T> supplier,
                                     @Nullable Predicate<T> predicate,
                                     @NonNull Consumer<T> consumer,
                                     @Nullable Runnable actionIfFailed,
                                     int delay
    ){
        return tryConsume(tag, supplier, predicate, consumer, actionIfFailed, delay, 1, 0);
    }


    /**
     * 尽力消费
     * @see #tryConsume(Object, Supplier, Predicate, Consumer, Runnable, int, int, int)
     * */
    public static <T> int tryConsume(@NonNull Object tag,
                                     @NonNull Supplier<T> supplier,
                                     @NonNull Consumer<T> consumer,
                                     @Nullable Runnable actionIfFailed,
                                     int maxTimesToTry,
                                     int interval
    ){
        return tryConsume(tag, supplier, null, consumer, actionIfFailed, 0, maxTimesToTry, interval);
    }


    /**
     * 尽力消费
     * @see #tryConsume(Object, Supplier, Predicate, Consumer, Runnable, int, int, int)
     * */
    public static <T> int tryConsume(@NonNull Object tag,
                                     @NonNull Supplier<T> supplier,
                                     @Nullable Predicate<T> predicate,
                                     @NonNull Consumer<T> consumer,
                                     @Nullable Runnable actionIfFailed,
                                     int maxTimesToTry,
                                     int interval
    ){
        return tryConsume(tag, supplier, predicate, consumer, actionIfFailed, 0, maxTimesToTry, interval);
    }


    /**
     * 尽力消费。
     * 若消费品提供且满足前置条件则消费，否则间隔一段时间再尝试，每次尝试均重新获取消费品并判断前置条件。
     * NOTE: 该方法内部在主线程执行，若有耗时操作，请将耗时操作投递到后台线程。
     * @param tag 订单标记。用户可以使用该tag对订单进行一些处理，比如取消该tag下的所有订单{@link #cancelOrdersByTag(Object)}
     * @param supplier 消费品提供者
     * @param predicate 前置条件
     * @param consumer 消费者
     * @param actionIfFailed 失败时的处理。
     * @param delay 执行订单的延迟。单位：毫秒。
     * @param maxTimesToTry 最大尝试次数。若<=0则表示无限次。（尝试次数包括首次，例如：尝试次数为1则表示仅试一次不重试，为2表示若失败重试一次）
     * @param interval 重试间隔时长。单位：毫秒
     * @return 订单号。一次消费生成一个订单，订单号大于0且唯一。
     *          用户可以使用该订单号对订单进行一些处理，比如取消订单{@link #cancelOrder(int)}
     * */
    public static <T> int tryConsume(@NonNull Object tag,
                                     @NonNull Supplier<T> supplier,
                                     @Nullable Predicate<T> predicate,
                                     @NonNull Consumer<T> consumer,
                                     @Nullable Runnable actionIfFailed,
                                     int delay,
                                     int maxTimesToTry,
                                     int interval
    ){
        RetryOrder<T> order = new RetryOrder<>(tag, supplier, predicate, consumer, actionIfFailed, delay, maxTimesToTry, interval);
        order.execute();
        return order.id;
    }


    /**
     * 取消订单
     * @param orderId 订单号
     * @return 成功返回true，若没有该订单则返回false。
     * */
    public static boolean cancelOrder(int orderId){
        Order<?> order = findOrder(orderId);
        if (order != null) {
            order.cancel();
        }
        return order != null;
    }


    /**
     * 取消tag下的所有订单
     * @return 成功返回true，若没有该tag的订单则返回false。
     * */
    public static boolean cancelOrdersByTag(@NonNull Object tag){
        Set<Order<?>> orders = findOrdersByTag(tag);
        if (!orders.isEmpty()) {
            Stream.of(orders).forEach(Order::cancel);
        }
        return !orders.isEmpty();
    }


    private static Order<?> findOrder(int orderId){
        return Stream.of(orders)
                .filter(o-> o.id == orderId)
                .findFirst()
                .orElse(null);
    }

    private static Set<Order<?>> findOrdersByTag(@NonNull Object tag){
        return Stream.of(orders)
                .filter(o-> o.tag == tag)
                .collect(Collectors.toSet());
    }


    private static class Order<T>{
        static int count;
        int id; // 订单号
        Object tag; // 标签
        Supplier<T> supplier;  // 消费品提供者
        Predicate<T> predicate; // 消费的前置条件
        Consumer<T> consumer; // 消费者
        Runnable actionIfFailed; // 失败时的处理。
        int delay;  // 订单执行延迟。单位：毫秒

        Runnable process; // 订单执行流程
        long timestamp = System.currentTimeMillis();

        Order(@NonNull Object tag,
              @NonNull Supplier<T> supplier,
              @Nullable Predicate<T> predicate,
              @NonNull Consumer<T> consumer,
              @Nullable Runnable actionIfFailed,
              int delay
        ) {
            id = ++count;
            this.tag = tag;
            this.supplier = supplier;
            this.predicate = predicate;
            this.consumer = consumer;
            this.actionIfFailed = actionIfFailed;
            this.delay = delay;

            process = () -> {
                T product = supplier.get();
                if (product != null && (predicate == null || predicate.test(product))){
                    consumer.accept(product);
                }else{
                    if (actionIfFailed != null) {
                        actionIfFailed.run();
                    }
                }
                orders.remove(Order.this);
            };

            orders.add(this);
        }


        void execute(){
            if (delay > 0) {
                handler.postDelayed(process, delay);
            }else{
                process.run();
            }
        }

        void cancel(){
            handler.removeCallbacks(process);
            orders.remove(this);
        }

    }


    private static class RetryOrder<T> extends Order<T>{
        int maxTimesToTry; // 最大尝试次数（包括首次）。若为0表示无限次
        int interval;   // 每次尝试的间隔时长。单位：毫秒

        RetryOrder(@NonNull Object tag,
                   @NonNull Supplier<T> supplier,
                   @Nullable Predicate<T> predicate,
                   @NonNull Consumer<T> consumer,
                   @Nullable Runnable actionIfFailed,
                   int delay,
                   int maxTimesToTry,
                   int interval
        ){
            super(tag, supplier, predicate, consumer, actionIfFailed, delay);
            this.maxTimesToTry = maxTimesToTry;
            this.interval = interval;

            process = new Runnable() {
                int triedTimes;
                @Override
                public void run() {
                    ++triedTimes;
                    T product = supplier.get();
                    if (product != null && (predicate == null || predicate.test(product))){
                        consumer.accept(product);
                    }else{
                        if (triedTimes == maxTimesToTry) {
                            if (actionIfFailed != null) {
                                actionIfFailed.run();
                            }
                        }else{
                            handler.postDelayed(this, interval);
                            return;
                        }
                    }

                    orders.remove(RetryOrder.this);
                }
            };
        }

    }

}
