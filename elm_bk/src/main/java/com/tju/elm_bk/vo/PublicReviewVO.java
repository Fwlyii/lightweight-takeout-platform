package com.tju.elm_bk.vo;

import java.time.LocalDateTime;

/** Public reviews never expose order IDs, customer IDs or account names. */
public record PublicReviewVO(Long id, String customerName, Integer rating, String content,
                             String images, String merchantReply, LocalDateTime replyTime,
                             LocalDateTime createTime) {
    public static PublicReviewVO from(ReviewVO review) {
        return new PublicReviewVO(review.getId(), "匿名顾客", review.getRating(), review.getContent(),
                review.getImages(), review.getMerchantReply(), review.getReplyTime(), review.getCreateTime());
    }
}
