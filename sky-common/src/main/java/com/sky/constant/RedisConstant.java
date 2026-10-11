package com.sky.constant;

/**
 * Redis 缓存相关常量
 */
public class RedisConstant {

    /**
     * 店铺营业状态
     * <p>注意:这是"业务状态"而不是缓存,因此**不设过期时间** ——
     * 一旦过期,用户端会读到"打烊",造成营业中却无法下单。
     */
    public static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    /**
     * 菜品缓存 key 前缀(完整 key = 前缀 + 分类id)
     */
    public static final String DISH_CACHE_KEY_PREFIX = "dish_";

    /**
     * 菜品缓存过期时间(分钟)
     */
    public static final long DISH_CACHE_TTL_MINUTES = 30;

    /**
     * 空结果缓存过期时间(秒)
     * <p>缓存空值是为了防"缓存穿透":不存在的分类不再每次都打到数据库;TTL 取短一些,
     * 保证新增菜品后最多 1 分钟就能看到。
     */
    public static final long EMPTY_CACHE_TTL_SECONDS = 60;

    /**
     * Spring Cache(@Cacheable)的默认过期时间(分钟)
     * <p>作为 @CacheEvict 之外的兜底,避免漏清理的缓存长期驻留脏数据。
     */
    public static final long DEFAULT_CACHE_TTL_MINUTES = 30;
}
