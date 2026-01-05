-- 创建有用的视图和函数
USE ai_health_check;

-- 当日可挂号医生视图
CREATE OR REPLACE VIEW v_today_available_doctors AS
SELECT
    d.doctor_id,
    d.real_name as doctor_name,
    u.gender,
    d.level,
    dept.dept_id,
    dept.dept_name,
    dept.dept_type,
    d.normal_fee,
    d.urgent_fee,
    -- 今日已预约数量
    COALESCE(today_stats.appointment_count, 0) as today_appointment_count,
    -- 可预约时间段数量
    COALESCE(schedule_stats.slot_count, 0) as available_slot_count,
    -- 排班信息（聚合）
    GROUP_CONCAT(DISTINCT s.time_slot ORDER BY sd.start_time) as available_time_slots
FROM doctors d
         JOIN users u ON d.user_id = u.user_id
         JOIN departments dept ON d.dept_id = dept.dept_id
-- 当日排班
         LEFT JOIN schedules s ON d.doctor_id = s.doctor_id
    AND s.work_day =
        CASE DAYOFWEEK(CURDATE())
            WHEN 1 THEN '周日'
            WHEN 2 THEN '周一'
            WHEN 3 THEN '周二'
            WHEN 4 THEN '周三'
            WHEN 5 THEN '周四'
            WHEN 6 THEN '周五'
            WHEN 7 THEN '周六'
            END
         LEFT JOIN shift_definitions sd ON s.time_slot = sd.time_slot
-- 今日预约统计
         LEFT JOIN (
    SELECT
        doctor_id,
        COUNT(*) as appointment_count
    FROM appointments
    WHERE appointment_date = CURDATE()
      AND status IN ('pending', 'confirmed')
    GROUP BY doctor_id
) today_stats ON d.doctor_id = today_stats.doctor_id
-- 排班统计
         LEFT JOIN (
    SELECT
        doctor_id,
        COUNT(*) as slot_count
    FROM schedules
    WHERE work_day =
          CASE DAYOFWEEK(CURDATE())
              WHEN 1 THEN '周日'
              WHEN 2 THEN '周一'
              WHEN 3 THEN '周二'
              WHEN 4 THEN '周三'
              WHEN 5 THEN '周四'
              WHEN 6 THEN '周五'
              WHEN 7 THEN '周六'
              END
    GROUP BY doctor_id
) schedule_stats ON d.doctor_id = schedule_stats.doctor_id
WHERE s.schedule_id IS NOT NULL  -- 只显示当天有排班的医生
GROUP BY d.doctor_id, d.real_name, u.gender, d.level, dept.dept_id, dept.dept_name, dept.dept_type,
         d.normal_fee, d.urgent_fee, today_stats.appointment_count, schedule_stats.slot_count;

-- 医生详细时间段视图
CREATE OR REPLACE VIEW v_doctor_today_slots AS
SELECT
    s.schedule_id,
    s.doctor_id,
    d.real_name as doctor_name,
    s.time_slot,
    sd.start_time,
    sd.end_time,
    sd.shift_type,
    d.level,
    d.normal_fee,
    d.urgent_fee,
    -- 是否已被预约
    CASE WHEN a.appointment_id IS NOT NULL THEN TRUE ELSE FALSE END as is_booked,
    -- 预约ID（如果已被预约）
    a.appointment_id,
    -- 加急名额信息
    uq.total_limit as urgent_total_limit,
    uq.used_count as urgent_used_count,
    uq.available_count as urgent_available_count,
    -- 患者信息（如果已被预约）
    a.patient_id,
    p.real_name as patient_name
FROM schedules s
         JOIN shift_definitions sd ON s.time_slot = sd.time_slot
         JOIN doctors d ON s.doctor_id = d.doctor_id
         LEFT JOIN appointments a ON s.schedule_id = a.schedule_id
    AND a.appointment_date = CURDATE()
    AND a.status IN ('pending', 'confirmed')
         LEFT JOIN patients p ON a.patient_id = p.patient_id
         LEFT JOIN urgent_quotas uq ON uq.quota_date = CURDATE()
    AND uq.time_slot = s.time_slot
WHERE s.work_day =
      CASE DAYOFWEEK(CURDATE())
          WHEN 1 THEN '周日'
          WHEN 2 THEN '周一'
          WHEN 3 THEN '周二'
          WHEN 4 THEN '周三'
          WHEN 5 THEN '周四'
          WHEN 6 THEN '周五'
          WHEN 7 THEN '周六'
          END
ORDER BY
    s.doctor_id,
    sd.start_time;

-- 获取中文星期几函数
CREATE FUNCTION get_chinese_weekday(date_val DATE) RETURNS VARCHAR(10)
    DETERMINISTIC
BEGIN
    RETURN CASE DAYOFWEEK(date_val)
               WHEN 1 THEN '周日'
               WHEN 2 THEN '周一'
               WHEN 3 THEN '周二'
               WHEN 4 THEN '周三'
               WHEN 5 THEN '周四'
               WHEN 6 THEN '周五'
               WHEN 7 THEN '周六'
        END;
END;