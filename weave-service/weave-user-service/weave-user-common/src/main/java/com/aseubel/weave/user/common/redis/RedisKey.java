package com.aseubel.weave.user.common.redis;

/**
 * @author Aseubel
 * @date 2025/6/28 下午9:22
 */
public class RedisKey {
    public static final String APP = "weave:";

    public static final String REDIS_TOKEN_KEY_PREFIX = "auth:token:";
    public static final String REDIS_SMS_CODE_PREFIX = "sms:code:";

    public static final String POST_LIKE_RECENT_KEY = "post:like:recent";
    public static final String POST_LIKE_COUNT_KEY = "post:like:count";
    public static final String POST_VIEW_COUNT_KEY = "post:view:count:";
    public static final String POST_COMMENT_COUNT_KEY = "post:comment:count:";
    public static final String POST_LIKE_STATUS_KEY = "post:like:status:";

    public static final String COMMENT_LIKE_RECENT_KEY = "comment:like:recent";
    public static final String COMMENT_LIKE_COUNT_KEY = "comment:like:count";
    public static final String COMMENT_VIEW_COUNT_KEY = "comment:view:count:";
    public static final String COMMENT_COMMENT_COUNT_KEY = "comment:comment:count:";
    public static final String COMMENT_LIKE_STATUS_KEY = "comment:like:status:";

    public static final String SUBMISSION_VOTE_RECENT_KEY = "submission:vote:recent";
    public static final String SUBMISSION_VOTE_COUNT_KEY = "submission:vote:count";
    public static final String SUBMISSION_VIEW_COUNT_KEY = "submission:view:count:";
    public static final String SUBMISSION_COMMENT_COUNT_KEY = "submission:comment:count:";
    public static final String SUBMISSION_VOTE_STATUS_KEY = "submission:vote:status:";

}
