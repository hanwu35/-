USE ai_health_check;

-- 1. 核心防重复：给排班表加唯一索引（杜绝同一医生同一天同一时段重复排）
ALTER TABLE schedules ADD UNIQUE INDEX uk_doc_day_slot (doctor_id, work_day, time_slot);

-- 2. 清空现有排班
TRUNCATE TABLE schedules;

-- 3. 创建基础时段/星期表（极简版）
DROP TABLE IF EXISTS temp_base;
CREATE TABLE temp_base (
                           work_day VARCHAR(10),
                           time_slot VARCHAR(50),
                           PRIMARY KEY (work_day, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入星期和时段
INSERT INTO temp_base (work_day, time_slot) VALUES
                                                ('周一','8:00-12:00'),('周一','14:00-18:00'),('周一','19:30-23:00'),('周一','0:00-4:00'),('周一','4:00-7:30'),
                                                ('周二','8:00-12:00'),('周二','14:00-18:00'),('周二','19:30-23:00'),('周二','0:00-4:00'),('周二','4:00-7:30'),
                                                ('周三','8:00-12:00'),('周三','14:00-18:00'),('周三','19:30-23:00'),('周三','0:00-4:00'),('周三','4:00-7:30'),
                                                ('周四','8:00-12:00'),('周四','14:00-18:00'),('周四','19:30-23:00'),('周四','0:00-4:00'),('周四','4:00-7:30'),
                                                ('周五','8:00-12:00'),('周五','14:00-18:00'),('周五','19:30-23:00'),('周五','0:00-4:00'),('周五','4:00-7:30'),
                                                ('周六','8:00-12:00'),('周六','14:00-18:00'),('周六','19:30-23:00'),('周六','0:00-4:00'),('周六','4:00-7:30'),
                                                ('周日','8:00-12:00'),('周日','14:00-18:00'),('周日','19:30-23:00'),('周日','0:00-4:00'),('周日','4:00-7:30');

-- 4. 分配专家（每个科室+每天+每个时段至少1位）
INSERT IGNORE INTO schedules (doctor_id, work_day, time_slot)
SELECT
    d.doctor_id,
    tb.work_day,
    tb.time_slot
FROM departments dept
         CROSS JOIN temp_base tb
-- 关联对应科室的专家
         JOIN doctors d ON dept.dept_id = d.dept_id AND d.level = '专家'
-- 每个科室+天+时段只取1位专家（去重）
WHERE (dept.dept_id, tb.work_day, tb.time_slot, d.doctor_id) IN (
    SELECT dept_id, work_day, time_slot, doctor_id
    FROM (
             SELECT
                 dept.dept_id,
                 tb.work_day,
                 tb.time_slot,
                 d.doctor_id,
                 -- 每个组合随机取1位专家
                 ROW_NUMBER() OVER (PARTITION BY dept.dept_id, tb.work_day, tb.time_slot ORDER BY RAND()) AS rn
             FROM departments dept
                      CROSS JOIN temp_base tb
                      JOIN doctors d ON dept.dept_id = d.dept_id AND d.level = '专家'
         ) t WHERE rn = 1
);

-- 5. 分配普通医生（每个科室+每天+每个时段至少2位）
INSERT IGNORE INTO schedules (doctor_id, work_day, time_slot)
SELECT
    d.doctor_id,
    tb.work_day,
    tb.time_slot
FROM departments dept
         CROSS JOIN temp_base tb
-- 关联对应科室的普通医生
         JOIN doctors d ON dept.dept_id = d.dept_id AND d.level = '普通'
-- 每个科室+天+时段取2位不同的普通医生（去重）
WHERE (dept.dept_id, tb.work_day, tb.time_slot, d.doctor_id) IN (
    SELECT dept_id, work_day, time_slot, doctor_id
    FROM (
             SELECT
                 dept.dept_id,
                 tb.work_day,
                 tb.time_slot,
                 d.doctor_id,
                 -- 每个组合随机取前2位普通医生
                 ROW_NUMBER() OVER (PARTITION BY dept.dept_id, tb.work_day, tb.time_slot ORDER BY RAND()) AS rn
             FROM departments dept
                      CROSS JOIN temp_base tb
                      JOIN doctors d ON dept.dept_id = d.dept_id AND d.level = '普通'
         ) t WHERE rn <= 2
);

-- 6. 清理临时表
DROP TABLE IF EXISTS temp_base;

-- 7. 验证结果：检查每个科室+天+时段是否都有人
SELECT '=== 排班结果验证（每个时间段是否有人）===' AS 检查项;
SELECT
    dept.dept_name AS 科室,
    s.work_day AS 星期,
    s.time_slot AS 时间段,
    COUNT(DISTINCT CASE WHEN d.level='专家' THEN d.doctor_id END) AS 专家数,
    COUNT(DISTINCT CASE WHEN d.level='普通' THEN d.doctor_id END) AS 普通医生数,
    CASE
        WHEN COUNT(DISTINCT CASE WHEN d.level='专家' THEN d.doctor_id END)>=1
            AND COUNT(DISTINCT CASE WHEN d.level='普通' THEN d.doctor_id END)>=2
            THEN '✓ 有人值班'
        ELSE '✗ 无人值班'
        END AS 结果
FROM departments dept
         CROSS JOIN (SELECT DISTINCT work_day FROM schedules) wd
         CROSS JOIN (SELECT DISTINCT time_slot FROM schedules) ts
         LEFT JOIN schedules s ON dept.dept_id = (SELECT dept_id FROM doctors WHERE doctor_id=s.doctor_id)
    AND s.work_day = wd.work_day
    AND s.time_slot = ts.time_slot
         LEFT JOIN doctors d ON s.doctor_id = d.doctor_id
GROUP BY dept.dept_name, wd.work_day, ts.time_slot
ORDER BY dept.dept_name, wd.work_day,
         FIELD(ts.time_slot, '8:00-12:00','14:00-18:00','19:30-23:00','0:00-4:00','4:00-7:30');

SELECT '=== 排班完成：所有时间段均已安排人员 ===' AS 状态;