package pattern;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Predicate;

import java.util.HashSet;
import java.util.Set;


public class ConsumerHelper {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Set<Order<?>> orders = new HashSet<>();


    /**
     * 消费。
     * @see #consume(Object, Object, Predicate, Consumer, Consumer, int, int, int)
     * */
    public static <T> int consume(@NonNull Object consumer,
                                  @Nullable T product,
                                  @NonNull Predicate<T> predicate,
                                  @NonNull Consumer<T> okActivity,
                                  @Nullable Consumer<T> failedActivity,
                                  int delay
    ){
        return consume(consumer, product, predicate, okActivity, failedActivity, 0, 0, delay);
    }

    /**
     * 消费。
     * @see #consume(Object, Object, Predicate, Consumer, Consumer, int, int, int)
     * */
    public static <T> int consume(@NonNull Object consumer,
                                  @Nullable T product,
                                  @NonNull Predicate<T> predicate,
                                  @NonNull Consumer<T> okActivity,
                                  @Nullable Consumer<T> failedActivity,
                                  int okDelay,
                                  int failedDelay
    ){
        return consume(consumer, product, predicate, okActivity, failedActivity, okDelay, failedDelay, 0);
    }


    /**
     * 消费。
     * NOTE: 该方法是异步的；
     *       该方法内部在主线程执行，若有耗时操作，请将耗时操作投递到后台线程。
     * @param consumer 消费者
     * @param product 消费品
     * @param predicate 前置条件。满足该条件才能正常消费。若为null表示没有条件约束。
     * @param okActivity 满足前置条件时的消费行为
     * @param failedActivity 不满足前置条件时的行为
     * @param okDelay 执行满足条件的消费行为前的延迟
     * @param failedDelay 执行不满足条件的消费行为前的延迟
     * @param delay 执行订单的延迟。单位：毫秒
     * @return 订单号。一次消费生成一个订单，订单号大于0且唯一。
     * */
    public static <T> int consume(@NonNull Object consumer,
                                  @Nullable T product,
                                  @NonNull Predicate<T> predicate,
                                  @NonNull Consumer<T> okActivity,
                                  @Nullable Consumer<T> failedActivity,
                                  int okDelay,
                                  int failedDelay,
                                  int delay
    ){
        Order<?> order = new Order<>(consumer, product, predicate, okActivity, failedActivity, okDelay, failedDelay, delay);
        order.execute();
        return order.id;
    }

    /**
     * 尽力消费
     * @see #tryConsume(Object, Object, Predicate, Consumer, Consumer, int, int, int, int, int)
     * */
    public static <T> int tryConsume(@NonNull Object consumer,
                                     @Nullable T product,
                                     @NonNull Predicate<T> predicate,
                                     @NonNull Consumer<T> okActivity,
                                     @Nullable Consumer<T> failedActivity,
                                     int maxTimesToTry,
                                     int interval){
        RetryOrder<T> order = new RetryOrder<>(consumer, product, predicate, okActivity, failedActivity, 0, 0, 0, maxTimesToTry, interval);
        order.execute();
        return order.id;
    }

    /**
     * 尽力消费
     * @see #tryConsume(Object, Object, Predicate, Consumer, Consumer, int, int, int, int, int)
     * */
    public static <T> int tryConsume(@NonNull Object consumer,
                                     @Nullable T product,
                                     @NonNull Predicate<T> predicate,
                                     @NonNull Consumer<T> okActivity,
                                     @Nullable Consumer<T> failedActivity,
                                     int delay,
                                     int maxTimesToTry,
                                     int interval){
        RetryOrder<T> order = new RetryOrder<>(consumer, product, predicate, okActivity, failedActivity, 0, 0, delay, maxTimesToTry, interval);
        order.execute();
        return order.id;
    }

    /**
     * 尽力消费。
     * 若满足前置条件则消费，否则间隔一段时间再尝试，每次尝试均重新判断前置条件。
     * NOTE: 该方法是异步的；
     *      该方法内部在主线程执行，若有耗时操作，请用户将耗时操作投递到后台线程。
     * @param consumer 消费者
     * @param product 消费品
     * @param predicate 前置条件
     * @param okActivity 满足前置条件情况下的消费行为
     * @param failedActivity 最大尝试次数后仍失败时的行为
     * @param okDelay 执行满足条件的消费行为前的延迟
     * @param failedDelay 执行不满足条件的消费行为前的延迟
     * @param delay 执行订单的延迟。单位：毫秒
     * @param maxTimesToTry 最大尝试次数。若<=0则表示无限次。（尝试次数包括首次，例如：尝试次数为1则表示仅试一次不重试，为2表示若失败重试一次）
     * @param interval 重试间隔时长。单位：毫秒
     * @return 订单号。一次消费生成一个订单，订单号大于0且唯一。
     * */
    public static <T> int tryConsume(@NonNull Object consumer,
                                     @Nullable T product,
                                     @NonNull Predicate<T> predicate,
                                     @NonNull Consumer<T> okActivity,
                                     @Nullable Consumer<T> failedActivity,
                                     int okDelay,
                                     int failedDelay,
                                     int delay,
                                     int maxTimesToTry,
                                     int interval){
        RetryOrder<T> order = new RetryOrder<>(consumer, product, predicate, okActivity, failedActivity, okDelay, failedDelay, delay, maxTimesToTry, interval);
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
     * 取消消费者的所有订单
     * @param consumer 消费者
     * @return 成功返回true，若没有该消费者的订单则返回false。
     * */
    public static boolean cancelOrdersOfConsumer(@NonNull Object consumer){
        Set<Order<?>> orders = findOrdersOfConsumer(consumer);
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

    private static Set<Order<?>> findOrdersOfConsumer(@NonNull Object consumer){
        return Stream.of(orders)
                .filter(o-> o.consumer == consumer)
                .collect(Collectors.toSet());
    }


    private static class Order<T>{
        static int count;
        int id; // 订单号
        Object consumer; // 消费者
        T product;  // 消费品
        Predicate<T> predicate; // 前置条件。若为null表示没有前置条件。
        Consumer<T> okActivity; // 满足前置条件时的消费行为
        Consumer<T> failedActivity; // 不满足前置条件时的消费行为
        int okDelay; // 执行满足条件的消费行为前的延迟。
        int failedDelay; // 执行不满足条件的消费行为前的延迟。
        int delay; // 订单执行延迟。单位：毫秒

        Runnable okRunnable;
        Runnable failedRunnable;
        Runnable process; // 订单执行流程
        long createTimestamp = System.currentTimeMillis();

        Order(@NonNull Object consumer, @Nullable T product, @Nullable Predicate<T> predicate, @NonNull Consumer<T> okActivity,
              @Nullable Consumer<T> failedActivity, int okDelay, int failedDelay, int delay) {
            id = ++count;
            this.consumer = consumer;
            this.product = product;
            this.predicate = predicate;
            this.okActivity = okActivity;
            this.failedActivity = failedActivity;
            this.delay = delay;
            okRunnable = () -> {
                okActivity.accept(product);
                orders.remove(Order.this);
            };
            if (predicate != null && failedActivity != null) {
                failedRunnable = () -> {
                    failedActivity.accept(product);
                    orders.remove(Order.this);
                };
            }
            process = () -> {
                if (predicate != null){
                    if (predicate.test(product)) {
                        handler.postDelayed(okRunnable, okDelay);
                    } else {
                        if (failedRunnable != null) {
                            handler.postDelayed(failedRunnable, failedDelay);
                        }else{
                            orders.remove(Order.this);
                        }
                    }
                }else{
                    handler.postDelayed(okRunnable, okDelay);
                }

            };
            orders.add(this);
        }


        void execute(){
            handler.postDelayed(process, delay);
        }

        void cancel(){
            handler.removeCallbacks(okRunnable);
            if (failedRunnable != null) {
                handler.removeCallbacks(failedRunnable);
            }
            handler.removeCallbacks(process);
            orders.remove(this);
        }

    }


    private static class RetryOrder<T> extends Order<T> {
        int maxTimesToTry; // 最大尝试次数（包括首次）
        int interval;   // 每次尝试的间隔时长。单位：毫秒

        RetryOrder(@NonNull Object consumer, @Nullable T product, @NonNull Predicate<T> predicate, @NonNull Consumer<T> activity,
                   @Nullable Consumer<T> activityIfFailed, int okDelay, int failedDelay, int delay, int maxTimesToTry, int interval) {
            super(consumer, product, predicate, activity, activityIfFailed, okDelay, failedDelay, delay);
            this.maxTimesToTry = maxTimesToTry;
            this.interval = interval;
            process = new Runnable() {
                int triedTimes;
                @Override
                public void run() {
                    ++triedTimes;
                    if (predicate.test(product)) {
                        handler.postDelayed(okRunnable, okDelay);
                    } else {
                        if (triedTimes == maxTimesToTry) {
                            if (failedRunnable != null) {
                                handler.postDelayed(failedRunnable, failedDelay);
                            }else{
                                orders.remove(RetryOrder.this);
                            }
                        } else {
                            handler.postDelayed(this, interval);
                        }
                    }
                }
            };
        }

    }

}
