-- 第三部分：重名处理
USE ai_health_check;

-- 找出所有重名的医生及其科室信息
SELECT
    d.real_name,
    COUNT(*) as 重名次数,
    GROUP_CONCAT(d.doctor_code ORDER BY d.doctor_code) as 医生编号,
    GROUP_CONCAT(dept.dept_name ORDER BY dept.dept_name) as 科室列表
FROM doctors d
         JOIN departments dept ON d.dept_id = dept.dept_id
GROUP BY d.real_name
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC;

-- 更新所有重名医生的姓名，添加科室编号
UPDATE doctors d
    JOIN departments dept ON d.dept_id = dept.dept_id
SET d.real_name = CONCAT(d.real_name, '_', dept.dept_code)
WHERE d.real_name IN (
    SELECT real_name
    FROM (
             SELECT real_name, COUNT(*) as cnt
             FROM doctors
             GROUP BY real_name
             HAVING COUNT(*) > 1
         ) AS tmp
);

-- 检查添加科室编号后是否还有重名
SELECT
    d.real_name,
    COUNT(*) as 剩余重名次数,
    GROUP_CONCAT(d.doctor_code) as 医生编号
FROM doctors d
GROUP BY d.real_name
HAVING COUNT(*) > 1;

-- 对同一科室内仍有重名的，添加01、02编号
UPDATE doctors d1
    JOIN (
        SELECT
            d2.doctor_id,
            d2.real_name,
            dept.dept_code,
            ROW_NUMBER() OVER (PARTITION BY d2.real_name ORDER BY d2.doctor_id) as rn
        FROM doctors d2
                 JOIN departments dept ON d2.dept_id = dept.dept_id
        WHERE d2.real_name IN (
            SELECT real_name
            FROM doctors
            GROUP BY real_name
            HAVING COUNT(*) > 1
        )
    ) ranked ON d1.doctor_id = ranked.doctor_id
SET d1.real_name =
        CASE
            WHEN ranked.rn = 1 THEN CONCAT(SUBSTRING_INDEX(d1.real_name, '_', 1), '_', ranked.dept_code)
            ELSE CONCAT(SUBSTRING_INDEX(d1.real_name, '_', 1), '_', ranked.dept_code, '_', LPAD(ranked.rn, 2, '0'))
            END;

-- 先找出患者内部的重名
SELECT real_name, COUNT(*) as 重名次数
FROM patients
GROUP BY real_name
HAVING COUNT(*) > 1;

-- 为患者内部的重名添加01、02编号
UPDATE patients p1
    JOIN (
        SELECT
            p2.patient_id,
            p2.real_name,
            ROW_NUMBER() OVER (PARTITION BY p2.real_name ORDER BY p2.patient_id) as rn
        FROM patients p2
        WHERE p2.real_name IN (
            SELECT real_name
            FROM patients
            GROUP BY real_name
            HAVING COUNT(*) > 1
        )
    ) ranked ON p1.patient_id = ranked.patient_id
SET p1.real_name =
        CASE
            WHEN ranked.rn = 1 THEN CONCAT(p1.real_name, '_01')
            ELSE CONCAT(SUBSTRING_INDEX(p1.real_name, '_', 1), '_', LPAD(ranked.rn, 2, '0'))
            END;

-- 验证医生姓名唯一性
SELECT
    '医生姓名唯一性检查' as 检查项,
    CASE
        WHEN COUNT(DISTINCT real_name) = COUNT(*) THEN '通过'
        ELSE '失败'
        END as 结果,
    COUNT(*) as 总记录数,
    COUNT(DISTINCT real_name) as 唯一姓名数
FROM doctors;

-- 验证患者姓名唯一性
SELECT
    '患者姓名唯一性检查' as 检查项,
    CASE
        WHEN COUNT(DISTINCT real_name) = COUNT(*) THEN '通过'
        ELSE '失败'
        END as 结果,
    COUNT(*) as 总记录数,
    COUNT(DISTINCT real_name) as 唯一姓名数
FROM patients;

-- 查看修改后的医生姓名示例
SELECT
    d.doctor_code,
    d.real_name as 修改后姓名,
    dept.dept_name as 科室
FROM doctors d
         JOIN departments dept ON d.dept_id = dept.dept_id
WHERE d.real_name LIKE '%_KS%' OR d.real_name LIKE '%_0%'
LIMIT 20;

-- 查看修改后的患者姓名示例
SELECT
    p.patient_code,
    p.real_name as 修改后姓名
FROM patients p
WHERE p.real_name LIKE '%_0%'
LIMIT 10;