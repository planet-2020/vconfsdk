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
     * NOTE: 该方法是异步的；
     *       该方法内部在主线程执行，若有耗时操作，请用户将耗时操作投递到后台线程。
     * @param consumer 消费者
     * @param product 消费品
     * @param precondition 前置条件。满足该条件才能正常消费。若为null表示没有条件约束。
     * @param activity 满足前置条件时的消费行为
     * @param activityIfFailed 不满足前置条件时的行为
     * @param delay 执行订单的延迟。单位：毫秒
     * @return 订单号。一次消费生成一个订单，订单号大于0且唯一。
     * */
    public static <T> int consume(@NonNull Object consumer,
                                   @Nullable T product,
                                   @NonNull Predicate<T> precondition,
                                   @NonNull Consumer<T> activity,
                                   @Nullable Consumer<T> activityIfFailed,
                                   int delay
    ){
        Order<?> order = new Order<>(consumer, product, precondition, activity, activityIfFailed, delay);
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
     * @param precondition 前置条件
     * @param activity 满足前置条件情况下的消费行为
     * @param activityIfFailed 最大尝试次数后仍失败时的行为
     * @param delay 执行订单的延迟。单位：毫秒
     * @param maxTimesToTry 最大尝试次数。若<=0则表示无限次。（尝试次数包括首次，例如：尝试次数为1则表示仅试一次不重试，为2表示若失败重试一次）
     * @param interval 重试间隔时长。单位：毫秒
     * @return 订单号。一次消费生成一个订单，订单号大于0且唯一。
     * */
    public static <T> int tryConsume(@NonNull Object consumer, @Nullable T product, @NonNull Predicate<T> precondition, @NonNull Consumer<T> activity,
                                      @Nullable Consumer<T> activityIfFailed, int delay, int maxTimesToTry, int interval){
        RetryOrder<T> order = new RetryOrder<>(consumer, product, precondition, activity, activityIfFailed, delay, maxTimesToTry, interval);
        order.execute();
        return order.id;
    }


    /**
     * 立即执行订单
     * @param orderId 订单号。
     * @return 成功返回true，若没有该订单则返回false。
     * */
    public static boolean driveOrder(int orderId){
        Order<?> order = findOrder(orderId);
        if (order != null) {
            order.drive();
        }
        return order != null;
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
        Predicate<T> precondition; // 前置条件。若为null表示没有前置条件。
        Consumer<T> activity; // 满足前置条件时的消费行为
        Consumer<T> activityIfFailed; // 不满足前置条件时的行为
        int delay; // 订单执行延迟。单位：毫秒
        Runnable process; // 订单执行流程
        long createTimestamp = System.currentTimeMillis();

        Order(@NonNull Object consumer, @Nullable T product, @Nullable Predicate<T> precondition, @NonNull Consumer<T> activity,
              @Nullable Consumer<T> activityIfFailed, int delay) {
            id = ++count;
            this.consumer = consumer;
            this.product = product;
            this.precondition = precondition;
            this.activity = activity;
            this.activityIfFailed = activityIfFailed;
            this.delay = delay;
            process = () -> {
                if (precondition != null){
                    if (precondition.test(product)) {
                        activity.accept(product);
                    } else {
                        if (activityIfFailed != null) activityIfFailed.accept(product);
                    }
                }else{
                    activity.accept(product);
                }
                orders.remove(Order.this);
            };
            orders.add(this);
        }

        /**
         * 订单加入执行队列
         * */
        void execute(){
            handler.postDelayed(process, delay);
        }

        /**
         * 立即执行订单
         * */
        void drive(){
            handler.removeCallbacks(process);
            if (precondition != null){
                if (precondition.test(product)) {
                    activity.accept(product);
                } else {
                    if (activityIfFailed != null) activityIfFailed.accept(product);
                }
            }else{
                activity.accept(product);
            }
            orders.remove(this);
        }

        /**
         * 取消订单
         * */
        void cancel(){
            handler.removeCallbacks(process);
            orders.remove(this);
        }

    }


    private static class RetryOrder<T> extends Order<T> {
        int maxTimesToTry; // 最大尝试次数（包括首次）
        int interval;   // 每次尝试的间隔时长。单位：毫秒

        RetryOrder(@NonNull Object consumer, @Nullable T product, @NonNull Predicate<T> precondition, @NonNull Consumer<T> activity,
                   @Nullable Consumer<T> activityIfFailed, int delay, int maxTimesToTry, int interval) {
            super(consumer, product, precondition, activity, activityIfFailed, delay);
            this.maxTimesToTry = maxTimesToTry;
            this.interval = interval;
            process = new Runnable() {
                int triedTimes;
                @Override
                public void run() {
                    ++triedTimes;
                    if (precondition.test(product)) {
                        activity.accept(product);
                    } else {
                        if (triedTimes == maxTimesToTry) {
                            if (activityIfFailed != null) activityIfFailed.accept(product);
                        } else {
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
