USE ai_health_check;
-- 确保"胆红素"只对应尿检项目
UPDATE item_mapping_config
SET chinese_names = '总胆红素,TBIL'
WHERE id = 23 AND chinese_names LIKE '%胆红素%';

-- 验证修改结果
SELECT id, chinese_names, feature_name, category, value_type
FROM item_mapping_config
WHERE chinese_names LIKE '%胆红素%';