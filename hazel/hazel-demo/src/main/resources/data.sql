-- 清空并重新插入库存数据
DELETE FROM inventory;
INSERT INTO inventory (product_name, quantity) VALUES
                                                   ('iPhone 15', 100),
                                                   ('MacBook Pro', 50),
                                                   ('AirPods', 200);