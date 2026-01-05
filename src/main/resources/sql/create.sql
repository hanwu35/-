-- 第一部分：数据库和表结构
USE ai_health_check;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_database = utf8mb4;
SET character_set_results = utf8mb4;
SET character_set_server = utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 用户表（添加姓名字段）
DROP TABLE IF EXISTS users;
CREATE TABLE users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       account VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
                       gender VARCHAR(10) NOT NULL,
                       age INT NOT NULL,
                       category ENUM('患者', '医生') NOT NULL,
                       INDEX idx_account (account),
                       INDEX idx_real_name (real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 科室表
DROP TABLE IF EXISTS departments;
CREATE TABLE departments (
                             dept_id INT AUTO_INCREMENT PRIMARY KEY,
                             dept_code VARCHAR(20) NOT NULL UNIQUE COMMENT '科室编号如KS001',
                             dept_name VARCHAR(100) NOT NULL UNIQUE,
                             dept_type VARCHAR(50) COMMENT '科室类型如内科、外科等',
                             introduction TEXT,
                             director_id INT NULL,
                             INDEX idx_dept_name (dept_name),
                             INDEX idx_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 医生表（添加姓名字段）
DROP TABLE IF EXISTS doctors;
CREATE TABLE doctors (
                         doctor_id INT AUTO_INCREMENT PRIMARY KEY,
                         user_id INT NOT NULL UNIQUE,
                         doctor_code VARCHAR(20) NOT NULL UNIQUE COMMENT '医生编号如YS001',
                         real_name VARCHAR(50) NOT NULL COMMENT '医生姓名',
                         dept_id INT NOT NULL,
                         level ENUM('普通', '专家') NOT NULL DEFAULT '普通',
                         FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         INDEX idx_dept_id (dept_id),
                         INDEX idx_doctor_code (doctor_code),
                         INDEX idx_doctor_name (real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 修改医生表（添加费用字段）
ALTER TABLE doctors
    ADD COLUMN normal_fee DECIMAL(10,2) DEFAULT 10.00,
    ADD COLUMN urgent_fee DECIMAL(10,2) DEFAULT 15.00,
    ADD INDEX idx_fee (normal_fee, urgent_fee);

-- 4. 患者表（添加姓名字段）
DROP TABLE IF EXISTS patients;
CREATE TABLE patients (
                          patient_id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL UNIQUE,
                          patient_code VARCHAR(20) NOT NULL UNIQUE COMMENT '患者编号如20251202000001',
                          real_name VARCHAR(50) NOT NULL COMMENT '患者姓名',
                          FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                          INDEX idx_patient_code (patient_code),
                          INDEX idx_patient_name (real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 排班表
DROP TABLE IF EXISTS schedules;
CREATE TABLE schedules (
                           schedule_id INT AUTO_INCREMENT PRIMARY KEY,
                           doctor_id INT NOT NULL,
                           work_day ENUM('周一', '周二', '周三', '周四', '周五', '周六', '周日') NOT NULL,
                           time_slot VARCHAR(50) NOT NULL,
                           FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
                           UNIQUE KEY uk_doctor_slot (doctor_id, work_day, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 班次定义表
DROP TABLE IF EXISTS shift_definitions;
CREATE TABLE IF NOT EXISTS shift_definitions (
                                                 shift_id INT AUTO_INCREMENT PRIMARY KEY,
                                                 time_slot VARCHAR(50) NOT NULL UNIQUE,
                                                 start_time TIME NOT NULL,
                                                 end_time TIME NOT NULL,
                                                 shift_type ENUM('day', 'evening', 'night') NOT NULL,
                                                 is_night_shift BOOLEAN DEFAULT FALSE,
                                                 INDEX idx_time_slot (time_slot),
                                                 INDEX idx_shift_type (shift_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 预约挂号表
DROP TABLE IF EXISTS appointments;
CREATE TABLE appointments (
                              appointment_id INT AUTO_INCREMENT PRIMARY KEY,
                              patient_id INT NOT NULL,
                              doctor_id INT NOT NULL,
                              schedule_id INT NOT NULL,
                              appointment_date DATE NOT NULL COMMENT '预约日期（只能是当天）',
                              time_slot VARCHAR(50) NOT NULL COMMENT '预约时间段',
                              status ENUM('pending', 'confirmed', 'completed', 'cancelled') DEFAULT 'pending',
                              fee DECIMAL(10, 2) NOT NULL COMMENT '挂号费用',
                              is_urgent BOOLEAN DEFAULT FALSE COMMENT '是否加急',
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
                              FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
                              FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE,
                              UNIQUE KEY uk_doctor_time (doctor_id, appointment_date, time_slot),
                              UNIQUE KEY uk_patient_time (patient_id, appointment_date, time_slot),
                              INDEX idx_status (status),
                              INDEX idx_date (appointment_date),
                              INDEX idx_doctor_date (doctor_id, appointment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 加急名额限制表（用于实现100个加急限制）
DROP TABLE IF EXISTS urgent_quotas;
CREATE TABLE urgent_quotas (
                               quota_id INT AUTO_INCREMENT PRIMARY KEY,
                               quota_date DATE NOT NULL COMMENT '日期',
                               time_slot VARCHAR(50) NOT NULL COMMENT '时间段',
                               total_limit INT NOT NULL DEFAULT 100 COMMENT '总加急名额限制',
                               used_count INT NOT NULL DEFAULT 0 COMMENT '已使用加急名额',
                               available_count INT GENERATED ALWAYS AS (total_limit - used_count) VIRTUAL COMMENT '剩余可用名额',
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               UNIQUE KEY uk_date_slot (quota_date, time_slot),
                               INDEX idx_date (quota_date),
                               INDEX idx_time_slot (time_slot),
                               INDEX idx_available (available_count),
                               CHECK (used_count <= total_limit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 健康指标专业表 ====================

-- 2. 创建血液预测结果表
CREATE TABLE health_blood (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id INT NOT NULL,
                              check_date DATE NOT NULL COMMENT '检查日期',

    -- 血液指标（原始数据）
                              hemoglobin DECIMAL(6,2) COMMENT '血红蛋白 (g/L)',
                              white_blood_cell DECIMAL(6,2) COMMENT '白细胞计数 (×10⁹/L)',
                              platelet DECIMAL(6,2) COMMENT '血小板计数 (×10⁹/L)',
                              blood_glucose DECIMAL(6,2) COMMENT '血糖 (mmol/L)',

    -- 血液模型预测结果
                              prediction_result VARCHAR(20) COMMENT '预测结果: 正常/异常',
                              abnormal_probability DECIMAL(5,2) COMMENT '异常概率 (%)',
                              normal_probability DECIMAL(5,2) COMMENT '正常概率 (%)',
                              abnormal_count INT COMMENT '异常指标数',
                              risk_level VARCHAR(20) COMMENT '风险等级: 正常/低风险/中风险/高风险',
                              recommendation TEXT COMMENT '建议',
                              model_confidence DECIMAL(5,2) COMMENT '模型置信度 (%)',
                              abnormal_indicators JSON COMMENT '异常指标详情',
                              model_version VARCHAR(50) DEFAULT '1.0' COMMENT '模型版本',
                              prediction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '预测时间',

                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                              FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                              INDEX idx_user_date (user_id, check_date),
                              UNIQUE KEY uk_user_check_date (user_id, check_date),
                              INDEX idx_prediction_time (prediction_time),
                              INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 创建尿液预测结果表
CREATE TABLE health_urine (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id INT NOT NULL,
                              check_date DATE NOT NULL COMMENT '检查日期',

    -- 尿液指标（原始数据）
                              urine_protein VARCHAR(20) COMMENT '尿蛋白',
                              urine_glucose VARCHAR(20) COMMENT '尿糖',
                              urine_specific_gravity DECIMAL(5,3) COMMENT '尿比重',
                              urine_ph DECIMAL(3,1) COMMENT '尿PH',
                              nitrite VARCHAR(20) COMMENT '亚硝酸盐',
                              ketone VARCHAR(20) COMMENT '尿酮体',
                              bilirubin VARCHAR(20) COMMENT '胆红素',
                              leukocyte_esterase VARCHAR(20) COMMENT '白细胞脂酶',
                              occult_blood VARCHAR(20) COMMENT '尿潜血',
                              urobilinogen VARCHAR(20) COMMENT '尿胆原',
                              vitamin_c VARCHAR(20) COMMENT '维生素C',
                              microscopy TEXT COMMENT '镜检结果',

    -- 尿液模型预测结果
                              prediction_result VARCHAR(20) COMMENT '预测结果: 低风险/高风险',
                              risk_probability DECIMAL(5,2) COMMENT '风险概率 (%)',
                              risk_score DECIMAL(5,2) COMMENT '风险评分',
                              risk_level VARCHAR(20) COMMENT '风险等级: 正常/低风险/中风险/高风险',
                              key_indicator_count INT COMMENT '关键指标数',
                              recommendation TEXT COMMENT '建议',
                              model_confidence DECIMAL(5,2) COMMENT '模型置信度 (%)',
                              key_indicators JSON COMMENT '关键指标详情',
                              model_version VARCHAR(50) DEFAULT '1.0' COMMENT '模型版本',
                              prediction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '预测时间',

                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                              FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                              INDEX idx_user_date (user_id, check_date),
                              UNIQUE KEY uk_user_check_date (user_id, check_date),
                              INDEX idx_prediction_time (prediction_time),
                              INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 创建肾功能预测结果表
CREATE TABLE health_kidney (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id INT NOT NULL,
                               check_date DATE NOT NULL COMMENT '检查日期',

    -- 肾功能指标（原始数据）
                               creatinine DECIMAL(6,2) COMMENT '肌酐 (μmol/L)',
                               urea_nitrogen DECIMAL(6,2) COMMENT '尿素氮 (mmol/L)',
                               uric_acid DECIMAL(6,2) COMMENT '尿酸 (μmol/L)',

    -- 肾功能模型预测结果
                               prediction_result VARCHAR(20) COMMENT '预测结果: 低风险/高风险',
                               risk_probability DECIMAL(5,2) COMMENT '风险概率 (%)',
                               risk_score DECIMAL(5,2) COMMENT '风险评分',
                               risk_level VARCHAR(20) COMMENT '风险等级: 正常/低风险/中风险/高风险',
                               abnormal_count INT COMMENT '异常指标数',
                               recommendation TEXT COMMENT '建议',
                               model_confidence DECIMAL(5,2) COMMENT '模型置信度 (%)',
                               abnormal_indicators JSON COMMENT '异常指标详情',
                               diagnosis_hypotheses JSON COMMENT '诊断假设',
                               model_version VARCHAR(50) DEFAULT '1.0' COMMENT '模型版本',
                               prediction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '预测时间',

                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                               FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               INDEX idx_user_date (user_id, check_date),
                               UNIQUE KEY uk_user_check_date (user_id, check_date),
                               INDEX idx_prediction_time (prediction_time),
                               INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 创建肝功能预测结果表
CREATE TABLE health_liver (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id INT NOT NULL,
                              check_date DATE NOT NULL COMMENT '检查日期',

    -- 肝功能指标（原始数据）
                              alt DECIMAL(6,2) COMMENT '谷丙转氨酶 (U/L)',
                              ast DECIMAL(6,2) COMMENT '谷草转氨酶 (U/L)',
                              total_bilirubin DECIMAL(6,2) COMMENT '总胆红素 (μmol/L)',
                              direct_bilirubin DECIMAL(6,2) COMMENT '直接胆红素 (μmol/L)',
                              indirect_bilirubin DECIMAL(6,2) COMMENT '间接胆红素 (μmol/L)',
                              albumin DECIMAL(6,2) COMMENT '白蛋白 (g/L)',
                              globulin DECIMAL(6,2) COMMENT '球蛋白 (g/L)',
                              total_protein DECIMAL(6,2) COMMENT '总蛋白 (g/L)',
                              ag_ratio DECIMAL(4,2) COMMENT 'A/G',
                              ggt DECIMAL(6,2) COMMENT '谷氨酰转酞酶 (IU/L)',
                              alp DECIMAL(6,2) COMMENT '碱性磷酸酶 (IU/L)',

    -- 肝功能模型预测结果
                              prediction_result VARCHAR(20) COMMENT '预测结果: 低风险/高风险',
                              risk_probability DECIMAL(5,2) COMMENT '风险概率 (%)',
                              risk_score DECIMAL(5,2) COMMENT '风险评分',
                              risk_level VARCHAR(20) COMMENT '风险等级: 正常/低风险/中风险/高风险',
                              abnormal_count INT COMMENT '异常指标数',
                              recommendation TEXT COMMENT '建议',
                              model_confidence DECIMAL(5,2) COMMENT '模型置信度 (%)',
                              abnormal_indicators JSON COMMENT '异常指标详情',
                              diagnosis_hypotheses JSON COMMENT '诊断假设',
                              model_version VARCHAR(50) DEFAULT '1.0' COMMENT '模型版本',
                              prediction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '预测时间',

                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                              FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                              INDEX idx_user_date (user_id, check_date),
                              UNIQUE KEY uk_user_check_date (user_id, check_date),
                              INDEX idx_prediction_time (prediction_time),
                              INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ==================== 健康指标原始数据表（按项目存储） ====================

-- 1. 血检项目表
DROP TABLE IF EXISTS health_blood_items;
CREATE TABLE health_blood_items (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id INT NOT NULL,
                                    card_number VARCHAR(50) COMMENT '卡号',
                                    gender VARCHAR(10) COMMENT '性别',
                                    age INT COMMENT '年龄',
                                    check_date DATE NOT NULL COMMENT '检查日期',

                                    item_name VARCHAR(100) NOT NULL COMMENT '小项名称（中文）',
                                    item_value VARCHAR(100) COMMENT '检验结果',
                                    unit VARCHAR(50) COMMENT '单位',

                                    feature_name VARCHAR(50) COMMENT '特征名称（英文）',
                                    numeric_value DOUBLE COMMENT '数值化结果',

                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                    INDEX idx_user_date (user_id, check_date),
                                    INDEX idx_item_name (item_name),
                                    INDEX idx_feature_name (feature_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 尿检项目表
DROP TABLE IF EXISTS health_urine_items;
CREATE TABLE health_urine_items (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id INT NOT NULL,
                                    card_number VARCHAR(50) COMMENT '卡号',
                                    gender VARCHAR(10) COMMENT '性别',
                                    age INT COMMENT '年龄',
                                    check_date DATE NOT NULL COMMENT '检查日期',

                                    item_name VARCHAR(100) NOT NULL COMMENT '小项名称（中文）',
                                    item_value VARCHAR(100) COMMENT '检验结果',
                                    unit VARCHAR(50) COMMENT '单位',

                                    feature_name VARCHAR(50) COMMENT '特征名称（英文）',
                                    numeric_value DOUBLE COMMENT '数值化结果',

                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                    INDEX idx_user_date (user_id, check_date),
                                    INDEX idx_item_name (item_name),
                                    INDEX idx_feature_name (feature_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 肝功能项目表
DROP TABLE IF EXISTS health_liver_items;
CREATE TABLE health_liver_items (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id INT NOT NULL,
                                    card_number VARCHAR(50) COMMENT '卡号',
                                    gender VARCHAR(10) COMMENT '性别',
                                    age INT COMMENT '年龄',
                                    check_date DATE NOT NULL COMMENT '检查日期',

                                    item_name VARCHAR(100) NOT NULL COMMENT '小项名称（中文）',
                                    item_value VARCHAR(100) COMMENT '检验结果',
                                    unit VARCHAR(50) COMMENT '单位',

                                    feature_name VARCHAR(50) COMMENT '特征名称（英文）',
                                    numeric_value DOUBLE COMMENT '数值化结果',

                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                    INDEX idx_user_date (user_id, check_date),
                                    INDEX idx_item_name (item_name),
                                    INDEX idx_feature_name (feature_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 肾功能项目表
DROP TABLE IF EXISTS health_kidney_items;
CREATE TABLE health_kidney_items (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_id INT NOT NULL,
                                     card_number VARCHAR(50) COMMENT '卡号',
                                     gender VARCHAR(10) COMMENT '性别',
                                     age INT COMMENT '年龄',
                                     check_date DATE NOT NULL COMMENT '检查日期',

                                     item_name VARCHAR(100) NOT NULL COMMENT '小项名称（中文）',
                                     item_value VARCHAR(100) COMMENT '检验结果',
                                     unit VARCHAR(50) COMMENT '单位',

                                     feature_name VARCHAR(50) COMMENT '特征名称（英文）',
                                     numeric_value DOUBLE COMMENT '数值化结果',

                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                     FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                     INDEX idx_user_date (user_id, check_date),
                                     INDEX idx_item_name (item_name),
                                     INDEX idx_feature_name (feature_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 特征向量表（模型输入格式） ====================

DROP TABLE IF EXISTS health_features;
CREATE TABLE health_features (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id INT NOT NULL,
                                 check_date DATE NOT NULL COMMENT '检查日期',

    -- 患者基本信息
                                 age INT COMMENT '年龄',
                                 gender_code INT COMMENT '性别编码（0=男，1=女）',

    -- 血检特征（使用示例数据的单位）
                                 platelet DOUBLE COMMENT '血小板总数（10^9/L）',
                                 white_blood_cell DOUBLE COMMENT '白细胞总数（10^9/L）',
                                 hemoglobin DOUBLE COMMENT '血红蛋白浓度（g/L）',
                                 red_blood_cell DOUBLE COMMENT '红细胞总数（10^12/L）',
                                 lymphocyte_percentage DOUBLE COMMENT '淋巴细胞百分比（%）',
                                 monocyte_percentage DOUBLE COMMENT '单核细胞百分比（%）',
                                 eosinophil_percentage DOUBLE COMMENT '嗜酸性细胞百分比（%）',
                                 basophil_percentage DOUBLE COMMENT '嗜碱性细胞百分比（%）',
                                 neutrophil_percentage DOUBLE COMMENT '中性粒细胞百分比（%）',
                                 blood_glucose DOUBLE COMMENT '血糖（mmol/L）',

    -- 尿检特征
                                 urine_protein DOUBLE COMMENT '尿蛋白（0=阴性，0.5=弱阳，1=阳性）',
                                 urine_glucose DOUBLE COMMENT '尿葡萄糖',
                                 urine_specific_gravity DOUBLE COMMENT '尿比重',
                                 nitrite DOUBLE COMMENT '亚硝酸盐',
                                 ketone DOUBLE COMMENT '尿酮体',
                                 bilirubin DOUBLE COMMENT '胆红素',
                                 leukocyte_esterase DOUBLE COMMENT '白细胞脂酶',
                                 vitamin_c DOUBLE COMMENT '维生素C',
                                 urine_ph DOUBLE COMMENT '尿PH',
                                 occult_blood DOUBLE COMMENT '尿潜血',
                                 urobilinogen DOUBLE COMMENT '尿胆原',

    -- 肝功能特征
                                 alt DOUBLE COMMENT '谷丙转氨酶（IU/L）',
                                 ast DOUBLE COMMENT '谷草转氨酶（IU/L）',
                                 total_bilirubin DOUBLE COMMENT '总胆红素（umol/L）',
                                 direct_bilirubin DOUBLE COMMENT '直接胆红素（umol/L）',
                                 indirect_bilirubin DOUBLE COMMENT '间接胆红素（umol/L）',
                                 albumin DOUBLE COMMENT '白蛋白（g/L）',
                                 globulin DOUBLE COMMENT '球蛋白（g/L）',
                                 total_protein DOUBLE COMMENT '总蛋白（g/L）',
                                 ag_ratio DOUBLE COMMENT 'A/G',
                                 ast_alt_ratio DOUBLE COMMENT '谷草/谷丙',
                                 ggt DOUBLE COMMENT '谷氨酰转酞酶（IU/L）',
                                 alp DOUBLE COMMENT '碱性磷酸酶（IU/L）',

    -- 肾功能特征
                                 creatinine DOUBLE COMMENT '肌酐（umol/L）',
                                 urea_nitrogen DOUBLE COMMENT '尿素氮（mmol/L）',
                                 uric_acid DOUBLE COMMENT '尿酸（umol/L）',

    -- 状态标志
                                 has_blood_data BOOLEAN DEFAULT FALSE,
                                 has_urine_data BOOLEAN DEFAULT FALSE,
                                 has_liver_data BOOLEAN DEFAULT FALSE,
                                 has_kidney_data BOOLEAN DEFAULT FALSE,

                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                 FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                 UNIQUE KEY uk_user_date (user_id, check_date),
                                 INDEX idx_date (check_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 项目映射配置表 ====================

DROP TABLE IF EXISTS item_mapping_config;
CREATE TABLE item_mapping_config (
                                     id INT AUTO_INCREMENT PRIMARY KEY,

    -- 中文项目名称（支持多个别名，用逗号分隔）
                                     chinese_names VARCHAR(500) NOT NULL COMMENT '中文项目名称（多个别名）',

    -- 英文特征名称
                                     feature_name VARCHAR(50) NOT NULL UNIQUE COMMENT '特征名称（英文）',

    -- 分类
                                     category ENUM('blood', 'urine', 'liver', 'kidney') NOT NULL COMMENT '检测类别',

    -- 数值化规则
                                     value_type ENUM('numeric', 'text_positive') NOT NULL DEFAULT 'numeric' COMMENT '值类型',

    -- 默认单位（模型训练使用的单位）
                                     default_unit VARCHAR(50) COMMENT '默认单位',

    -- 单位转换系数（原始单位到默认单位的转换）
                                     unit_conversion DOUBLE DEFAULT 1.0 COMMENT '单位转换系数',

    -- 显示信息
                                     display_name VARCHAR(100) COMMENT '显示名称',
                                     description TEXT COMMENT '描述',

                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                     INDEX idx_category (category),
                                     INDEX idx_feature_name (feature_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 添加外键约束
ALTER TABLE doctors
    ADD CONSTRAINT fk_doctors_dept
        FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
            ON DELETE RESTRICT;

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_director
        FOREIGN KEY (director_id) REFERENCES doctors(doctor_id)
            ON DELETE SET NULL;

SET FOREIGN_KEY_CHECKS = 1;