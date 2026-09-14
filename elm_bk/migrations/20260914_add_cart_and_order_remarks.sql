-- 购物车备注（同一商家一条）与订单备注：顾客在购物车填写，下单时写入订单，商家端可见。
ALTER TABLE cart
    ADD COLUMN remarks VARCHAR(255) NULL DEFAULT NULL COMMENT '同一商家的备注（口味/餐具等）';

ALTER TABLE orders
    ADD COLUMN remarks VARCHAR(255) NULL DEFAULT NULL COMMENT '顾客备注（口味/餐具等）';
