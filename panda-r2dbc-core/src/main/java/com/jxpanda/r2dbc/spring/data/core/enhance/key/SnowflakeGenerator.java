package com.jxpanda.r2dbc.spring.data.core.enhance.key;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于雪花算法的ID生成器
 * 雪花算法生成的ID是Long类型
 * 这里使用抽象类的设计，可以在实例化对象的时候，通过匿名内部类的机制，重写cast函数，把Long对象转为期望的类型
 * 例如：转为String类型，以此来提供一定程度上ID类型的灵活性
 */
public abstract class SnowflakeGenerator<T extends Serializable> implements IdGenerator<T> {

    private final Snowflake snowflake;

    public SnowflakeGenerator() {
        snowflake = new Snowflake(0, 0);
    }

    public SnowflakeGenerator(int dataCenterId, int workerId) {
        snowflake = new Snowflake(dataCenterId, workerId);
    }

    protected abstract T cast(Long id);

    @Override
    public T generate() {
        return cast(snowflake.next());
    }

    /**
     * 基于Twitter的Snowflake算法实现分布式高效有序ID生产黑科技(sequence)——升级版Snowflake
     *
     * <br>
     * Snowflake的结构如下(每部分用-分开):<br>
     * <br>
     * workerId
     * 0 - 「0000000000 0000000000 0000000000 0000000000 0」 - 「00000」 - 「00000」 - 000000000000 <br>
     * ↑                         时间戳                       datacenterId                序列号
     * <br>
     * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
     * <br>
     * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
     * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下START_TIME属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
     * <br>
     * 10位的数据机器位，可以部署在1024个节点，包括5位dataCenterId和5位workerId<br>
     * <br>
     * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
     * <br>
     * <br>
     * 加起来刚好64位，为一个Long型。<br>
     * Snowflake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，Snowflake每秒能够产生26万ID左右。
     * <p>
     * <p>
     * 特性：
     * 1.支持自定义允许时间回拨的范围<p>
     * 2.解决跨毫秒起始值每次为0开始的情况（避免末尾必定为偶数，而不便于取余使用问题）<p>
     * 3.时间回拨方案思考：1024个节点中分配10个点作为时间回拨序号（连续10次时间回拨的概率较小）
     * <p>
     * 抄来的
     * 没错，我连注释都抄
     * 当然，被我修改过一下，源代码地址如下
     * 优化开源项目：<a href="https://gitee.com/yu120/sequence">...</a>
     */
    private static class Snowflake {
        /**
         * 起始时间戳（选了2000年千禧年）
         * 起始时间戳选择的越接近当前时间，生成的id长度越短
         **/
        private final static long START_TIME = 946656000000L;

        /**
         * dataCenterId占用的位数：5
         **/
        private final static long DATA_CENTER_ID_BITS = 5L;
        /**
         * workerId占用的位数：5
         **/
        private final static long WORKER_ID_BITS = 5L;
        /**
         * 序列号占用的位数：12（表示只允许workId的范围为：0-4095）
         **/
        private final static long SEQUENCE_BITS = 12L;

        /**
         * workerId可以使用范围：0-31
         **/
        private final static long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        /**
         * dataCenterId可以使用范围：0-31
         **/
        private final static long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

        /**
         * workerId的偏移值，从序列号占用的位数开始，也就是偏移12位
         * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 「00000」 - 000000000000
         * workerId
         */
        private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;
        /**
         * datacenterId的偏移量是workerId的偏移量+workerId的位数
         * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 「00000」 - 00000 - 000000000000
         * datacenterId
         */
        private final static long DATA_CENTER_ID_SHIFT = WORKER_ID_SHIFT + WORKER_ID_BITS;
        /**
         * 时间戳的偏移量
         * 0 - 「0000000000 0000000000 0000000000 0000000000 0」 - 00000 - 00000 - 000000000000
         * 时间戳
         */
        private final static long TIMESTAMP_LEFT_SHIFT = DATA_CENTER_ID_SHIFT + DATA_CENTER_ID_BITS;

        /**
         * 用mask防止溢出:位与运算保证计算的结果范围始终是 0-4095
         **/
        private final static long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

        private final long workerId;
        private final long dataCenterId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;

        private final long timeOffset;

        public Snowflake() {
            this(0, 0);
        }

        /**
         * @param dataCenterId 数据中心ID,数据范围为0~255
         * @param workerId     工作机器ID,数据范围为0~255
         */
        public Snowflake(long dataCenterId, long workerId) {
            this(dataCenterId, workerId, 5L);
        }

        /**
         * 基于Snowflake创建分布式ID生成器
         *
         * @param dataCenterId 数据中心ID,数据范围为0~31
         * @param workerId     工作机器ID,数据范围为0~31
         * @param timeOffset   允许时间回拨的毫秒量,建议5ms
         */
        public Snowflake(long dataCenterId, long workerId, long timeOffset) {
            if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
                throw new IllegalArgumentException("Data Center Id can't be greater than " + MAX_DATA_CENTER_ID + " or less than 0");
            }
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("Worker Id can't be greater than " + MAX_WORKER_ID + " or less than 0");
            }

            this.workerId = workerId;
            this.dataCenterId = dataCenterId;
            this.timeOffset = timeOffset;
        }

        /**
         * 获取ID
         *
         * @return long
         */
        public synchronized Long next() {
            long currentTimestamp = this.timestamp();

            // 闰秒：如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，这个时候应当抛出异常
            if (currentTimestamp < lastTimestamp) {
                // 校验时间偏移回拨量
                long offset = lastTimestamp - currentTimestamp;
                if (offset > timeOffset) {
                    throw new RuntimeException("Clock moved backwards, refusing to generate id for [" + offset + "ms]");
                }

                try {
                    // 时间回退timeOffset毫秒内，则允许等待2倍的偏移量后重新获取，解决小范围的时间回拨问题
                    this.wait(offset << 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                // 再次获取
                currentTimestamp = this.timestamp();
                // 再次校验
                if (currentTimestamp < lastTimestamp) {
                    throw new RuntimeException("Clock moved backwards, refusing to generate id for [" + offset + "ms]");
                }
            }

            // 同一毫秒内序列直接自增
            if (lastTimestamp == currentTimestamp) {
                // 通过位与运算保证计算的结果范围始终是 0-4095
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    currentTimestamp = this.tilNextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒内，序列号置为 1 - 3 随机数
                sequence = ThreadLocalRandom.current().nextLong(1, 3);
            }

            lastTimestamp = currentTimestamp;
            long currentOffsetTime = currentTimestamp - START_TIME;

            /*
             * 1.左移运算是为了将数值移动到对应的段(41、5、5。最后12那段因为本来就在最右，因此不用左移)
             * 2.然后对每个左移后的值(la、lb、lc、sequence)做位或运算，是为了把各个短的数据合并起来，合并成一个二进制数
             * 3.最后转换成10进制，就是最终生成的id
             */
            return (currentOffsetTime << TIMESTAMP_LEFT_SHIFT) |
                    // 数据中心位
                    (dataCenterId << DATA_CENTER_ID_SHIFT) |
                    // 工作ID位
                    (workerId << WORKER_ID_SHIFT) |
                    // 毫秒序列化位
                    sequence;

        }

        /**
         * 保证返回的毫秒数在参数之后(阻塞到下一个毫秒，直到获得新的时间戳)——CAS
         *
         * @param lastTimestamp last timestamp
         * @return next millis
         */
        private long tilNextMillis(long lastTimestamp) {
            long timestamp = this.timestamp();
            while (timestamp <= lastTimestamp) {
                // 如果发现时间回拨，则自动重新获取（可能会处于无限循环中）
                timestamp = this.timestamp();
            }
            return timestamp;
        }

        /**
         * 获得系统当前毫秒时间戳
         *
         * @return timestamp 毫秒时间戳
         */
        private long timestamp() {
            return SystemClock.INSTANCE.currentTimeMillis();
        }

        /**
         * System Clock
         * <p>
         * 高并发场景下System.currentTimeMillis()的性能问题的优化
         *
         * <p>System.currentTimeMillis()的调用比new一个普通对象要耗时的多（具体耗时高出多少我还没测试过，有人说是100倍左右）</p>
         * <p>System.currentTimeMillis()之所以慢是因为去跟系统打了一次交道</p>
         * <p>后台定时更新时钟，JVM退出时，线程自动回收</p>
         * <p>10亿：43410,206,210.72815533980582%</p>
         * <p>1亿：4699,29,162.0344827586207%</p>
         * <p>1000万：480,12,40.0%</p>
         * <p>100万：50,10,5.0%</p>
         *
         * @author lry
         */
        private enum SystemClock {

            INSTANCE(1);

            private final long period;
            private final AtomicLong now;
            private boolean started = false;
            private ScheduledExecutorService executorService;

            SystemClock(long period) {
                this.period = period;
                this.now = new AtomicLong(System.currentTimeMillis());
                initialize();
            }

            /**
             * initialize scheduled executor service
             */
            public void initialize() {
                if (started) {
                    return;
                }

                this.executorService = new ScheduledThreadPoolExecutor(1, runnable -> {
                    Thread thread = new Thread(runnable, "System Clock");
                    thread.setDaemon(true);
                    return thread;
                });
                executorService.scheduleAtFixedRate(() -> now.set(System.currentTimeMillis()),
                        this.period, this.period, TimeUnit.MILLISECONDS);
                Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
                started = true;
            }

            /**
             * The get current time milliseconds
             *
             * @return long time
             */
            public long currentTimeMillis() {
                return started ? now.get() : System.currentTimeMillis();
            }

            /**
             * The get string current time
             *
             * @return string time
             */
            public String currentTime() {
                return new Timestamp(currentTimeMillis()).toString();
            }

            /**
             * destroy of executor service
             */
            public void destroy() {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }

        }

    }

}
