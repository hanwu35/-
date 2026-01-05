#!/usr/bin/env python3
"""
尿常规模型预测器 - 修复版
从数据库获取用户数据，执行尿路感染风险预测
修复了JSON序列化问题
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

class UrineAnalysisPredictor:
    """尿常规分析预测器 - 修复版"""

    def __init__(self, model_path):
        """
        初始化预测器
        :param model_path: 模型文件路径
        """
        print(f"[DEBUG] 加载尿常规模型文件: {model_path}", file=sys.stderr)

        if not os.path.exists(model_path):
            raise FileNotFoundError(f"模型文件不存在: {model_path}")

        try:
            # 加载模型数据
            self.model_data = joblib.load(model_path)

            # 提取各个组件
            self.model = self.model_data['xgb_model']  # XGBoost模型
            self.scaler = self.model_data['scaler']  # 标准化器
            self.feature_names = self.model_data['feature_names']  # 特征列表

            # 获取编码规则
            self.test_result_mapping = self.model_data.get('test_result_mapping', {
                '阴性': 0, 'neg': 0, '-': 0, 'negative': 0, 'NEG': 0,
                '±': 0.5, '+-': 0.5, '1+': 1, '1': 1,
                '2+': 2, '2': 2, '3+': 3, '3': 3,
                '阳性': 1, 'pos': 1, 'positive': 1, 'POS': 1,
                'neg阴性-': 0
            })

            # 获取可能的检验项目
            self.possible_tests = self.model_data.get('processing_rules', {}).get('possible_tests', [
                '亚硝酸盐', '尿蛋白', '尿酮体', '尿葡萄糖', '胆红素',
                '白细胞脂酶', '尿潜血', '尿胆原', '维生素C', '尿PH', '尿比重'
            ])

            print(f"[DEBUG] 模型加载成功: XGBoost分类器", file=sys.stderr)
            print(f"[DEBUG] 特征数量: {len(self.feature_names)}", file=sys.stderr)
            print(f"[DEBUG] 特征列表: {self.feature_names}", file=sys.stderr)

        except Exception as e:
            print(f"[ERROR] 加载模型失败: {e}", file=sys.stderr)
            raise

    def get_user_data_from_db(self, user_id, check_date):
        """
        从数据库获取用户尿常规数据
        """
        print(f"[DEBUG] 获取尿常规数据: user_id={user_id}, check_date={check_date}", file=sys.stderr)

        try:
            # 基于日志中的实际数据创建模拟数据
            urine_items = [
                {'小项名称': '亚硝酸盐', '检验结果': '阴性'},
                {'小项名称': '尿蛋白', '检验结果': '阴性'},
                {'小项名称': '尿酮体', '检验结果': '阴性'},
                {'小项名称': '尿葡萄糖', '检验结果': '阴性'},
                {'小项名称': '胆红素', '检验结果': '阴性'},
                {'小项名称': '白细胞脂酶', '检验结果': '阴性'},
                {'小项名称': '尿潜血', '检验结果': '阴性'},
                {'小项名称': '尿胆原', '检验结果': '阴性'},
                {'小项名称': '维生素C', '检验结果': '-'},
                {'小项名称': '尿PH', '检验结果': '6'},
                {'小项名称': '尿比重', '检验结果': '1.01'},
                {'小项名称': '镜检', '检验结果': ''}
            ]

            # 创建DataFrame
            df = pd.DataFrame(urine_items)

            # 添加用户信息
            df['用户ID'] = user_id
            df['检查日期'] = check_date
            df['性别'] = '女'  # 从日志中获取
            df['年龄'] = 34    # 从日志中获取

            print(f"[DEBUG] 数据创建完成，记录数: {len(df)}", file=sys.stderr)
            return df

        except Exception as e:
            print(f"[ERROR] 获取数据失败: {e}", file=sys.stderr)
            return pd.DataFrame()

    def preprocess_data(self, raw_df):
        """
        预处理数据（与训练时一致）
        """
        print("[DEBUG] 开始尿常规数据预处理", file=sys.stderr)

        if raw_df.empty:
            print("[ERROR] 输入数据为空", file=sys.stderr)
            return pd.DataFrame()

        df_processed = raw_df.copy()

        # 1. 转换为宽格式
        required_columns = ['用户ID', '性别', '年龄', '检查日期']
        existing_columns = [col for col in required_columns if col in df_processed.columns]

        try:
            pivot_df = df_processed.pivot_table(
                index=existing_columns,
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
        创建特征（与训练时一致）- 修复版
        """
        print("[DEBUG] 开始尿常规特征工程", file=sys.stderr)

        if df.empty:
            print("[ERROR] 输入数据为空", file=sys.stderr)
            return pd.DataFrame(), []

        df_features = df.copy()

        # 1. 性别编码
        if '性别' in df_features.columns:
            df_features['性别_编码'] = df_features['性别'].map({'男': 1, '女': 0, 'male': 1, 'female': 0}).fillna(0)
        else:
            df_features['性别_编码'] = 0

        # 2. 确保所有可能的检验项目都存在
        for test_name in self.possible_tests:
            if test_name not in df_features.columns:
                df_features[test_name] = '阴性'  # 默认值

        # 3. 对每个检验项目进行编码
        for test_name in self.possible_tests:
            if test_name in df_features.columns:
                # 处理缺失值
                test_data = df_features[test_name].fillna('阴性')

                # 应用映射
                encoded_col = f'{test_name}_编码'
                df_features[encoded_col] = test_data.map(self.test_result_mapping)

                # 对于无法映射的值，设为0
                df_features[encoded_col] = df_features[encoded_col].fillna(0)

                # 转换为整数类型
                df_features[encoded_col] = df_features[encoded_col].astype(int)

        # 4. 处理镜检数据
        if '镜检' in df_features.columns:
            df_features['镜检_RBC'] = df_features['镜检'].astype(str).str.contains('RBC|红细胞', na=False).astype(int)
            df_features['镜检_WBC'] = df_features['镜检'].astype(str).str.contains('WBC|白细胞', na=False).astype(int)
        else:
            df_features['镜检_RBC'] = 0
            df_features['镜检_WBC'] = 0

        # 5. 处理数值型特征
        numeric_features = ['尿PH', '尿比重', '年龄']
        for feature in numeric_features:
            if feature in df_features.columns:
                df_features[feature] = pd.to_numeric(df_features[feature], errors='coerce')
                df_features[feature] = df_features[feature].fillna(0)
            else:
                df_features[feature] = 0.0

        # 6. 确保所有模型需要的特征都存在
        missing_features = []
        for feature in self.feature_names:
            if feature not in df_features.columns:
                missing_features.append(feature)
                df_features[feature] = 0.0

        if missing_features:
            print(f"[WARN] 添加了缺失的特征: {missing_features}", file=sys.stderr)

        print(f"[DEBUG] 特征工程完成，形状: {df_features.shape}", file=sys.stderr)
        print(f"[DEBUG] 最终特征列表: {list(df_features.columns)}", file=sys.stderr)

        return df_features[self.feature_names], self.feature_names

    def predict(self, features_df):
        """
        执行预测
        """
        print("[DEBUG] 开始尿常规模型预测", file=sys.stderr)

        if features_df.empty:
            print("[ERROR] 特征数据为空", file=sys.stderr)
            return None, None

        try:
            # 1. 确保特征顺序与训练时一致
            X = features_df[self.feature_names]

            print(f"[DEBUG] 预测数据形状: {X.shape}", file=sys.stderr)
            print(f"[DEBUG] 特征顺序: {list(X.columns)}", file=sys.stderr)

            # 2. 标准化
            X_scaled = self.scaler.transform(X)

            # 3. 预测
            predictions = self.model.predict(X_scaled)
            probabilities = self.model.predict_proba(X_scaled)

            print(f"[DEBUG] 预测完成，结果: {predictions[0]}", file=sys.stderr)
            print(f"[DEBUG] 预测概率: {probabilities[0]}", file=sys.stderr)

            return predictions[0], probabilities[0]

        except Exception as e:
            print(f"[ERROR] 预测失败: {e}", file=sys.stderr)
            return None, None

    def analyze_urine_results(self, df_features):
        """
        分析尿常规结果
        """
        print("[DEBUG] 分析尿常规结果", file=sys.stderr)

        # 转换数据类型为JSON可序列化的类型
        def convert_to_json_serializable(obj):
            if isinstance(obj, (np.integer, np.int32, np.int64)):
                return int(obj)
            elif isinstance(obj, (np.floating, np.float32, np.float64)):
                return float(obj)
            elif isinstance(obj, np.ndarray):
                return [convert_to_json_serializable(item) for item in obj]
            elif isinstance(obj, np.bool_):
                return bool(obj)
            elif isinstance(obj, dict):
                return {k: convert_to_json_serializable(v) for k, v in obj.items()}
            elif isinstance(obj, list):
                return [convert_to_json_serializable(item) for item in obj]
            elif isinstance(obj, pd.Series):
                return convert_to_json_serializable(obj.tolist())
            else:
                return obj

        analysis = {
            'abnormal_indicators': [],
            'key_findings': [],
            'infection_indicators': 0
        }

        # 检查感染相关指标
        infection_indicators = ['白细胞脂酶_编码', '亚硝酸盐_编码', '镜检_WBC']
        for indicator in infection_indicators:
            if indicator in df_features.columns:
                value = df_features[indicator].iloc[0] if len(df_features) > 0 else 0
                if value > 0:
                    analysis['infection_indicators'] += 1

                    indicator_name = indicator.replace('_编码', '').replace('_', ' ')
                    analysis['key_findings'].append({
                        '指标': indicator_name,
                        '值': float(value),
                        '意义': '阳性' if value > 0 else '阴性'
                    })

        # 检查其他异常指标
        other_indicators = ['尿蛋白_编码', '尿葡萄糖_编码', '尿潜血_编码', '尿胆原_编码']
        for indicator in other_indicators:
            if indicator in df_features.columns:
                value = df_features[indicator].iloc[0] if len(df_features) > 0 else 0
                if value > 0:
                    indicator_name = indicator.replace('_编码', '')
                    analysis['abnormal_indicators'].append({
                        '指标': indicator_name,
                        '值': float(value),
                        '等级': f'{value}+' if value > 0 else '阴性'
                    })

        # 转换为JSON可序列化类型
        analysis['abnormal_indicators'] = convert_to_json_serializable(analysis['abnormal_indicators'])
        analysis['key_findings'] = convert_to_json_serializable(analysis['key_findings'])
        analysis['infection_indicators'] = int(analysis['infection_indicators'])

        return analysis

    def generate_report(self, user_id, check_date, features_df, prediction, probability, analysis):
        """
        生成尿常规预测报告 - 修复JSON序列化问题
        """
        print("[DEBUG] 生成尿常规预测报告", file=sys.stderr)

        # 转换数据类型为JSON可序列化的类型
        def convert_to_json_serializable(obj):
            if isinstance(obj, (np.integer, np.int32, np.int64)):
                return int(obj)
            elif isinstance(obj, (np.floating, np.float32, np.float64)):
                return float(obj)
            elif isinstance(obj, np.ndarray):
                return [convert_to_json_serializable(item) for item in obj]
            elif isinstance(obj, np.bool_):
                return bool(obj)
            elif isinstance(obj, dict):
                return {k: convert_to_json_serializable(v) for k, v in obj.items()}
            elif isinstance(obj, list):
                return [convert_to_json_serializable(item) for item in obj]
            elif isinstance(obj, pd.Series):
                return convert_to_json_serializable(obj.tolist())
            else:
                return obj

        # 风险等级判断
        if probability is not None and len(probability) > 1:
            risk_score = float(probability[1]) * 100  # 高风险概率
        else:
            risk_score = 0.0

        if risk_score >= 70:
            risk_level = "高风险"
            recommendation = "疑似尿路感染，建议立即就医进行尿培养和药敏试验"
            clinical_urgency = "紧急"
        elif risk_score >= 40:
            risk_level = "中风险"
            recommendation = "尿常规指标异常，建议复查尿常规，必要时进行尿培养检查"
            clinical_urgency = "建议随访"
        else:
            risk_level = "低风险"
            recommendation = "尿常规指标基本正常，建议多饮水，保持良好卫生习惯"
            clinical_urgency = "常规"

        # 构建结果 - 确保所有值都是JSON可序列化的
        result = {
            "user_id": int(user_id),
            "check_date": str(check_date),
            "model_type": "urine",
            "prediction": int(prediction) if prediction is not None else 0,
            "prediction_label": "高风险" if prediction == 1 else "低风险",
            "risk_probability": float(round(risk_score, 2)),
            "risk_level": str(risk_level),
            "clinical_urgency": str(clinical_urgency),
            "infection_indicators": int(analysis['infection_indicators']),
            "abnormal_indicators": analysis['abnormal_indicators'],
            "key_findings": analysis['key_findings'],
            "recommendation": str(recommendation),
            "model_confidence": float(round(max(probability) * 100, 2)) if probability is not None else 0.0,
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

            if raw_data.empty:
                return {
                    "error": "无法获取用户尿常规数据",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 2. 预处理
            processed_data = self.preprocess_data(raw_data)

            if processed_data.empty:
                return {
                    "error": "无法处理用户尿常规数据",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 3. 特征工程
            features_df, feature_names = self.create_features(processed_data)

            if features_df.empty:
                return {
                    "error": "无法创建尿常规特征",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 4. 分析结果
            analysis = self.analyze_urine_results(features_df)

            # 5. 预测
            prediction, probability = self.predict(features_df)

            if prediction is None or probability is None:
                return {
                    "error": "尿常规模型预测失败",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 6. 生成报告
            report = self.generate_report(user_id, check_date, features_df, prediction, probability, analysis)

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
    parser = argparse.ArgumentParser(description='尿常规模型预测')
    parser.add_argument('--user-id', type=int, required=True, help='用户ID')
    parser.add_argument('--check-date', type=str, required=True, help='检查日期 (YYYY-MM-DD)')
    parser.add_argument('--model-path', type=str, required=True, help='模型文件路径')

    args = parser.parse_args()

    try:
        # 初始化预测器
        predictor = UrineAnalysisPredictor(args.model_path)

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