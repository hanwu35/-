#!/usr/bin/env python3
"""
肾功能模型预测器
从数据库获取用户数据，执行肾功能异常预测
"""

import sys
import json
import argparse
import os
import pandas as pd
import numpy as np
from datetime import datetime
import joblib
import warnings
import sys
import io

# 强制UTF-8编码
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
warnings.filterwarnings('ignore')

class KidneyFunctionPredictor:
    """肾功能分析预测器"""

    def __init__(self, model_path):
        """
        初始化预测器
        :param model_path: 模型文件路径
        """
        print(f"[DEBUG] 加载肾功能模型文件: {model_path}", file=sys.stderr)

        if not os.path.exists(model_path):
            raise FileNotFoundError(f"模型文件不存在: {model_path}")

        try:
            # 加载模型数据
            self.model_data = joblib.load(model_path)

            # 提取各个组件
            self.model = self.model_data['model']  # RandomForest模型
            self.scaler = self.model_data['scaler']  # 标准化器
            self.imputer = self.model_data['imputer']  # 缺失值填充器
            self.label_encoder = self.model_data['label_encoder']  # 标签编码器
            self.feature_names = self.model_data['feature_names']  # 特征列表
            self.reference_ranges = self.model_data['reference_ranges']  # 参考范围
            self.disease_knowledge_base = self.model_data.get('disease_knowledge_base', {})

            print(f"[DEBUG] 模型加载成功: RandomForest分类器", file=sys.stderr)
            print(f"[DEBUG] 特征数量: {len(self.feature_names)}", file=sys.stderr)
            print(f"[DEBUG] 参考范围数量: {len(self.reference_ranges)}", file=sys.stderr)

        except Exception as e:
            print(f"[ERROR] 加载模型失败: {e}", file=sys.stderr)
            raise

    def get_user_data_from_db(self, user_id, check_date):
        """
        从数据库获取用户肾功能数据
        实际使用时需要替换为真实的数据库查询
        """
        print(f"[DEBUG] 获取肾功能数据: user_id={user_id}, check_date={check_date}", file=sys.stderr)

        # 这里应该是真实的数据库查询
        # 为了示例，我们创建模拟数据

        # 模拟肾功能项目数据
        kidney_items = [
            {'小项名称': '尿酸', '检验结果': '420'},
            {'小项名称': '尿素氮', '检验结果': '6.5'},
            {'小项名称': '肌酐', '检验结果': '85'},
            {'小项名称': '尿微量白蛋白', '检验结果': '25'},
            {'小项名称': '胱抑素C', '检验结果': '1.2'}
        ]

        # 创建DataFrame
        df = pd.DataFrame(kidney_items)

        # 添加用户信息
        df['用户ID'] = user_id
        df['检查日期'] = check_date
        df['性别'] = '男'  # 示例数据
        df['年龄'] = 45  # 示例数据

        print(f"[DEBUG] 模拟数据创建完成，记录数: {len(df)}", file=sys.stderr)
        return df

    def preprocess_data(self, raw_df):
        """
        预处理数据（与训练时一致）
        """
        print("[DEBUG] 开始肾功能数据预处理", file=sys.stderr)

        df_processed = raw_df.copy()

        # 1. 转换为宽格式
        required_columns = []
        if '用户ID' in df_processed.columns:
            required_columns.append('用户ID')
        if '性别' in df_processed.columns:
            required_columns.append('性别')
        if '年龄' in df_processed.columns:
            required_columns.append('年龄')
        if '检查日期' in df_processed.columns:
            required_columns.append('检查日期')

        try:
            pivot_df = df_processed.pivot_table(
                index=required_columns,
                columns='小项名称',
                values='检验结果',
                aggfunc='first'
            ).reset_index()

            pivot_df.columns.name = None

            print(f"[DEBUG] 宽格式转换完成，形状: {pivot_df.shape}", file=sys.stderr)
            print(f"[DEBUG] 转换后列名: {list(pivot_df.columns)}", file=sys.stderr)

        except Exception as e:
            print(f"[ERROR] 转换为宽格式失败: {e}", file=sys.stderr)
            return pd.DataFrame()

        return pivot_df

    def create_features(self, df):
        """
        创建特征（与训练时一致）
        """
        print("[DEBUG] 开始肾功能特征工程", file=sys.stderr)

        if df.empty:
            return pd.DataFrame(), []

        df_features = df.copy()

        # 1. 性别编码
        if '性别' in df_features.columns:
            try:
                # 使用加载的label_encoder
                df_features['性别编码'] = self.label_encoder.transform(df_features['性别'])
            except:
                # 如果编码失败，使用映射
                gender_mapping = {'男': 1, '女': 0}
                df_features['性别编码'] = df_features['性别'].map(gender_mapping).fillna(0)

        # 2. 数值型特征处理
        key_indicators = list(self.reference_ranges.keys())
        for indicator in key_indicators:
            if indicator in df_features.columns:
                df_features[indicator] = pd.to_numeric(df_features[indicator], errors='coerce')

        # 3. 创建异常特征
        for feature, ref_range in self.reference_ranges.items():
            if feature in df_features.columns:
                if isinstance(ref_range, tuple) and len(ref_range) == 2:
                    low, high = ref_range
                    df_features[f'{feature}_偏低'] = (df_features[feature] < low).astype(int)
                    df_features[f'{feature}_偏高'] = (df_features[feature] > high).astype(int)
                    df_features[f'{feature}_异常'] = (
                            (df_features[feature] < low) | (df_features[feature] > high)
                    ).astype(int)

        # 4. 创建综合特征
        abnormal_features = [col for col in df_features.columns if '_异常' in col]
        if abnormal_features:
            df_features['总异常数'] = df_features[abnormal_features].sum(axis=1)

        # 肾功能严重异常（肌酐和尿素氮同时异常）
        key_abnormal_features = [f'{feat}_异常' for feat in ['肌酐', '尿素氮'] if f'{feat}_异常' in df_features.columns]
        if key_abnormal_features:
            df_features['肾功能严重异常'] = df_features[key_abnormal_features].max(axis=1)

        # 5. 只选择模型需要的特征
        available_features = []
        for feature in self.feature_names:
            if feature in df_features.columns:
                available_features.append(feature)
            else:
                print(f"[WARN] 模型需要特征 '{feature}' 但数据中不存在", file=sys.stderr)

        print(f"[DEBUG] 特征工程完成，可用特征: {len(available_features)}", file=sys.stderr)

        # 如果特征缺失，用默认值填充
        if available_features:
            features_df = df_features[available_features].copy()
            # 填充缺失值
            for col in features_df.columns:
                if features_df[col].dtype in ['float64', 'int64']:
                    features_df[col] = features_df[col].fillna(features_df[col].mean())
                else:
                    features_df[col] = features_df[col].fillna(0)
        else:
            features_df = pd.DataFrame()

        return features_df, available_features

    def predict(self, features_df):
        """
        执行预测
        """
        print("[DEBUG] 开始肾功能模型预测", file=sys.stderr)

        if features_df.empty:
            return None, None

        try:
            # 1. 确保特征顺序与训练时一致
            X = features_df[self.feature_names]

            # 2. 处理缺失值
            X_imputed = self.imputer.transform(X)

            # 3. 标准化
            X_scaled = self.scaler.transform(X_imputed)

            # 4. 预测
            predictions = self.model.predict(X_scaled)
            probabilities = self.model.predict_proba(X_scaled)

            print(f"[DEBUG] 预测完成，结果: {predictions[0]}", file=sys.stderr)
            print(f"[DEBUG] 预测概率: {probabilities[0]}", file=sys.stderr)

            return predictions[0], probabilities[0]

        except Exception as e:
            print(f"[ERROR] 预测失败: {e}", file=sys.stderr)
            return None, None

    def analyze_abnormal_indicators(self, features_df):
        """
        分析异常指标（与训练时一致）
        """
        print("[DEBUG] 分析肾功能异常指标", file=sys.stderr)

        abnormal_analysis = []
        disease_votes = {}

        for feature, ref_range in self.reference_ranges.items():
            if feature in features_df.columns:
                value = features_df[feature].iloc[0] if len(features_df) > 0 else None

                if pd.notna(value):
                    if isinstance(ref_range, tuple):
                        low, high = ref_range
                    elif isinstance(ref_range, dict):
                        low = ref_range.get('lower', ref_range.get('low', 0))
                        high = ref_range.get('upper', ref_range.get('high', 100))
                    else:
                        continue

                    status = "正常"
                    possible_diseases = []

                    if value < low:
                        status = "降低"
                        if feature in self.disease_knowledge_base and '降低' in self.disease_knowledge_base[feature]:
                            possible_diseases = self.disease_knowledge_base[feature]['降低']
                    elif value > high:
                        status = "升高"
                        if feature in self.disease_knowledge_base and '升高' in self.disease_knowledge_base[feature]:
                            possible_diseases = self.disease_knowledge_base[feature]['升高']

                    if status != "正常":
                        abnormal_analysis.append({
                            '指标': feature,
                            '检测值': float(value),
                            '状态': status,
                            '参考范围': f"{low:.1f}-{high:.1f}",
                            '可能疾病': possible_diseases
                        })

                        for disease in possible_diseases:
                            disease_votes[disease] = disease_votes.get(disease, 0) + 1

        # 综合肾功能评估
        if abnormal_analysis:
            abnormal_count = len(abnormal_analysis)
            if abnormal_count >= 2 and '综合肾功能' in self.disease_knowledge_base:
                for disease in self.disease_knowledge_base['综合肾功能']['异常']:
                    disease_votes[disease] = disease_votes.get(disease, 0) + 2

        return abnormal_analysis, disease_votes

    def generate_diagnosis_hypotheses(self, disease_votes):
        """
        生成诊断假设（与训练时一致）
        """
        sorted_diseases = sorted(disease_votes.items(), key=lambda x: x[1], reverse=True)
        diagnosis_hypotheses = []

        for disease, count in sorted_diseases:
            if count >= 3:
                confidence = "高"
            elif count == 2:
                confidence = "中"
            else:
                confidence = "低"

            diagnosis_hypotheses.append({
                '疾病': disease,
                '置信度': confidence,
                '支持指标数': count
            })

        # 只返回前5个诊断假设
        return diagnosis_hypotheses[:5]

    def generate_investigation_suggestions(self, diagnosis_hypotheses, abnormal_analysis):
        """
        生成检查建议（与训练时一致）
        """
        suggestions = []

        high_confidence_diseases = [d for d in diagnosis_hypotheses if d['置信度'] == '高']
        medium_confidence_diseases = [d for d in diagnosis_hypotheses if d['置信度'] == '中']

        if high_confidence_diseases:
            suggestions.append(f"优先排查: {', '.join([d['疾病'] for d in high_confidence_diseases])}")
        if medium_confidence_diseases:
            suggestions.append(f"同时排查: {', '.join([d['疾病'] for d in medium_confidence_diseases])}")

        for analysis in abnormal_analysis:
            if analysis['指标'] == '肌酐' and analysis['状态'] == '升高':
                suggestions.append("建议进行尿常规、肾脏B超、肾小球滤过率(eGFR)检查")
            elif analysis['指标'] == '尿酸' and analysis['状态'] == '升高':
                suggestions.append("建议进行关节检查，监测痛风发作风险")

        if not suggestions:
            suggestions.append("肾功能指标基本正常，建议定期复查")
        else:
            suggestions.append("建议咨询肾内科医生进行详细诊断")

        return suggestions

    def generate_health_advice(self, abnormal_analysis):
        """
        生成健康建议（与训练时一致）
        """
        advice = []

        for analysis in abnormal_analysis:
            if analysis['指标'] == '尿酸' and analysis['状态'] == '升高':
                advice.extend([
                    "低嘌呤饮食：避免动物内脏、海鲜、啤酒等高嘌呤食物",
                    "多饮水：每日饮水量2000ml以上",
                    "控制体重：避免肥胖"
                ])
            elif analysis['指标'] == '肌酐' and analysis['状态'] == '升高':
                advice.extend([
                    "低盐饮食：每日食盐<6g",
                    "优质低蛋白饮食：适量控制蛋白质摄入",
                    "控制血压：维持在130/80mmHg以下"
                ])

        if not advice:
            advice.append("保持健康生活方式：均衡饮食、适量运动、充足饮水")
        else:
            advice = list(set(advice))
            advice.append("定期体检，监测肾功能变化")

        return advice

    def generate_report(self, user_id, check_date, features_df, prediction, probability,
                        abnormal_analysis, diagnosis_hypotheses, investigation_suggestions, health_advice):
        """
        生成肾功能预测报告
        """
        print("[DEBUG] 生成肾功能预测报告", file=sys.stderr)

        # 风险等级判断
        risk_score = probability[1] * 100  # 异常概率

        if risk_score >= 70:
            risk_level = "高风险"
            clinical_urgency = "紧急"
            if any(d['置信度'] == '高' for d in diagnosis_hypotheses):
                recommendation = "存在高风险疾病可能性，建议立即就医"
            else:
                recommendation = "肾功能严重异常，需立即肾内科就诊"
        elif risk_score >= 40:
            risk_level = "中风险"
            clinical_urgency = "建议随访"
            recommendation = "肾功能指标异常，建议专科门诊就诊"
        else:
            risk_level = "低风险"
            clinical_urgency = "常规"
            recommendation = "肾功能指标基本正常，建议定期复查"

        # 计算总异常数
        total_abnormal_count = len(abnormal_analysis)

        # 构建结果
        result = {
            "user_id": user_id,
            "check_date": check_date,
            "model_type": "kidney",
            "prediction": int(prediction),
            "prediction_label": "异常" if prediction == 1 else "正常",
            "risk_probability": round(risk_score, 2),
            "risk_level": risk_level,
            "clinical_urgency": clinical_urgency,
            "total_abnormal_count": total_abnormal_count,
            "abnormal_indicators": abnormal_analysis,
            "diagnosis_hypotheses": diagnosis_hypotheses,
            "investigation_suggestions": investigation_suggestions,
            "health_advice": health_advice,
            "recommendation": recommendation,
            "model_confidence": round(max(probability) * 100, 2),
            "prediction_time": datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            "success": True
        }

        return result

    def predict_for_user(self, user_id, check_date):
        """
        为指定用户执行预测
        """
        try:
            # 1. 获取数据
            raw_data = self.get_user_data_from_db(user_id, check_date)

            # 2. 预处理
            processed_data = self.preprocess_data(raw_data)

            if processed_data.empty:
                return {
                    "error": "无法处理用户肾功能数据",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 3. 特征工程
            features_df, feature_names = self.create_features(processed_data)

            if features_df.empty:
                return {
                    "error": "无法创建肾功能特征",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 4. 分析异常指标
            abnormal_analysis, disease_votes = self.analyze_abnormal_indicators(features_df)

            # 5. 生成诊断假设
            diagnosis_hypotheses = self.generate_diagnosis_hypotheses(disease_votes)

            # 6. 生成检查建议
            investigation_suggestions = self.generate_investigation_suggestions(diagnosis_hypotheses, abnormal_analysis)

            # 7. 生成健康建议
            health_advice = self.generate_health_advice(abnormal_analysis)

            # 8. 预测
            prediction, probability = self.predict(features_df)

            if prediction is None:
                return {
                    "error": "肾功能模型预测失败",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 9. 生成报告
            report = self.generate_report(user_id, check_date, features_df, prediction, probability,
                                          abnormal_analysis, diagnosis_hypotheses,
                                          investigation_suggestions, health_advice)

            return report

        except Exception as e:
            return {
                "error": str(e),
                "user_id": user_id,
                "check_date": check_date,
                "success": False
            }

def main():
    """
    主函数 - 用于命令行调用
    """
    parser = argparse.ArgumentParser(description='肾功能模型预测')
    parser.add_argument('--user-id', type=int, required=True, help='用户ID')
    parser.add_argument('--check-date', type=str, required=True, help='检查日期 (YYYY-MM-DD)')
    parser.add_argument('--model-path', type=str, required=True, help='模型文件路径')

    args = parser.parse_args()

    try:
        # 初始化预测器
        predictor = KidneyFunctionPredictor(args.model_path)

        # 执行预测
        result = predictor.predict_for_user(args.user_id, args.check_date)

        # 输出JSON结果
        print(json.dumps(result, ensure_ascii=False, indent=2))

        # 如果失败，返回非零退出码
        if not result.get('success', False):
            sys.exit(1)

    except Exception as e:
        error_result = {
            "error": str(e),
            "user_id": args.user_id,
            "check_date": args.check_date,
            "success": False
        }
        print(json.dumps(error_result, ensure_ascii=False))
        sys.exit(1)

if __name__ == "__main__":
    main()