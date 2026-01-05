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

class BloodAnalysisPredictor:
    """血常规分析预测器"""

    def __init__(self, model_path):
        """
        初始化预测器
        :param model_path: 模型文件路径
        """
        print(f"[DEBUG] 加载血常规模型文件: {model_path}", file=sys.stderr)

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

            print(f"[DEBUG] 模型加载成功: RandomForest分类器", file=sys.stderr)
            print(f"[DEBUG] 特征数量: {len(self.feature_names)}", file=sys.stderr)
            print(f"[DEBUG] 参考范围数量: {len(self.reference_ranges)}", file=sys.stderr)

        except Exception as e:
            print(f"[ERROR] 加载模型失败: {e}", file=sys.stderr)
            raise

    def get_user_data_from_db(self, user_id, check_date):
        """
        从数据库获取用户血常规数据
        实际使用时需要替换为真实的数据库查询
        """
        print(f"[DEBUG] 获取血常规数据: user_id={user_id}, check_date={check_date}", file=sys.stderr)

        # 这里应该是真实的数据库查询
        # 为了示例，我们创建模拟数据

        # 模拟血常规项目数据
        blood_items = [
            {'小项名称': '血小板总数', '检验结果': '250'},
            {'小项名称': '白细胞总数', '检验结果': '7.2'},
            {'小项名称': '血红蛋白浓度', '检验结果': '135'},
            {'小项名称': '中性粒细胞百分比', '检验结果': '65'},
            {'小项名称': '淋巴细胞百分比', '检验结果': '25'},
            {'小项名称': '单核细胞百分比', '检验结果': '6'},
            {'小项名称': '嗜酸性细胞百分比', '检验结果': '3'},
            {'小项名称': '嗜碱性细胞百分比', '检验结果': '0.5'},
            {'小项名称': '红细胞总数', '检验结果': '4.8'}
        ]

        # 创建DataFrame
        df = pd.DataFrame(blood_items)

        # 添加用户信息
        df['用户ID'] = user_id
        df['检查日期'] = check_date
        df['性别'] = '男'  # 示例数据
        df['年龄'] = 40  # 示例数据

        print(f"[DEBUG] 模拟数据创建完成，记录数: {len(df)}", file=sys.stderr)
        return df

    def preprocess_data(self, raw_df):
        """
        预处理数据（与训练时一致）
        """
        print("[DEBUG] 开始血常规数据预处理", file=sys.stderr)

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
        print("[DEBUG] 开始血常规特征工程", file=sys.stderr)

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
                    df_features[f'{feature}_异常'] = (
                            (df_features[feature] < low) | (df_features[feature] > high)
                    ).astype(int)
                elif isinstance(ref_range, dict):
                    low = ref_range.get('lower', ref_range.get('low', 0))
                    high = ref_range.get('upper', ref_range.get('high', 100))
                    df_features[f'{feature}_异常'] = (
                            (df_features[feature] < low) | (df_features[feature] > high)
                    ).astype(int)

        # 4. 只选择模型需要的特征
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
        print("[DEBUG] 开始血常规模型预测", file=sys.stderr)

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

    def analyze_blood_results(self, features_df):
        """
        分析血常规结果
        """
        print("[DEBUG] 分析血常规结果", file=sys.stderr)

        analysis = {
            'abnormal_indicators': [],
            'key_abnormalities': [],
            'anemia_indication': False,
            'infection_indication': False,
            'thrombocytopenia_indication': False,
            'total_abnormal_count': 0
        }

        # 检查关键指标
        key_indicators = ['白细胞总数', '血红蛋白浓度', '血小板总数']

        for indicator in key_indicators:
            if indicator in features_df.columns:
                value = features_df[indicator].iloc[0] if len(features_df) > 0 else None
                if pd.notna(value) and indicator in self.reference_ranges:
                    ref_range = self.reference_ranges[indicator]

                    if isinstance(ref_range, tuple):
                        low, high = ref_range
                    elif isinstance(ref_range, dict):
                        low = ref_range.get('lower', ref_range.get('low', 0))
                        high = ref_range.get('upper', ref_range.get('high', 100))
                    else:
                        continue

                    is_abnormal = value < low or value > high

                    if is_abnormal:
                        analysis['total_abnormal_count'] += 1

                        abnormal_info = {
                            '指标': indicator,
                            '值': float(value),
                            '参考范围': f"{low}-{high}",
                            '状态': '偏低' if value < low else '偏高'
                        }

                        analysis['abnormal_indicators'].append(abnormal_info)
                        analysis['key_abnormalities'].append(abnormal_info)

                        # 检查具体病症指示
                        if indicator == '血红蛋白浓度' and value < low:
                            analysis['anemia_indication'] = True
                        elif indicator == '白细胞总数' and value > high:
                            analysis['infection_indication'] = True
                        elif indicator == '血小板总数' and value < low:
                            analysis['thrombocytopenia_indication'] = True

        # 检查其他指标
        other_indicators = ['中性粒细胞百分比', '淋巴细胞百分比', '单核细胞百分比',
                            '嗜酸性细胞百分比', '嗜碱性细胞百分比', '红细胞总数']

        for indicator in other_indicators:
            if f'{indicator}_异常' in features_df.columns:
                is_abnormal = features_df[f'{indicator}_异常'].iloc[0] if len(features_df) > 0 else 0
                if is_abnormal:
                    analysis['total_abnormal_count'] += 1

        return analysis

    def generate_report(self, user_id, check_date, features_df, prediction, probability, analysis):
        """
        生成血常规预测报告
        """
        print("[DEBUG] 生成血常规预测报告", file=sys.stderr)

        # 风险等级判断
        risk_score = probability[1] * 100  # 异常概率

        if risk_score >= 70:
            risk_level = "高风险"
            if analysis['anemia_indication']:
                recommendation = "发现贫血迹象，建议进行铁蛋白、维生素B12等进一步检查"
            elif analysis['infection_indication']:
                recommendation = "白细胞异常升高，提示可能存在感染，建议临床评估"
            elif analysis['thrombocytopenia_indication']:
                recommendation = "血小板减少，建议复查血常规并排查相关疾病"
            else:
                recommendation = "血常规多项指标异常，建议专科就诊"
            clinical_urgency = "紧急"
        elif risk_score >= 40:
            risk_level = "中风险"
            recommendation = "部分血常规指标异常，建议1-2周后复查"
            clinical_urgency = "建议随访"
        else:
            risk_level = "低风险"
            recommendation = "血常规指标基本正常，建议保持健康生活习惯"
            clinical_urgency = "常规"

        # 构建结果
        result = {
            "user_id": user_id,
            "check_date": check_date,
            "model_type": "blood",
            "prediction": int(prediction),
            "prediction_label": "异常" if prediction == 1 else "正常",
            "risk_probability": round(risk_score, 2),
            "risk_level": risk_level,
            "clinical_urgency": clinical_urgency,
            "total_abnormal_count": analysis['total_abnormal_count'],
            "key_abnormalities": analysis['key_abnormalities'],
            "anemia_indication": analysis['anemia_indication'],
            "infection_indication": analysis['infection_indication'],
            "thrombocytopenia_indication": analysis['thrombocytopenia_indication'],
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
                    "error": "无法处理用户血常规数据",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 3. 特征工程
            features_df, feature_names = self.create_features(processed_data)

            if features_df.empty:
                return {
                    "error": "无法创建血常规特征",
                    "user_id": user_id,
                    "check_date": check_date,
                    "success": False
                }

            # 4. 分析结果
            analysis = self.analyze_blood_results(features_df)

            # 5. 预测
            prediction, probability = self.predict(features_df)

            if prediction is None:
                return {
                    "error": "血常规模型预测失败",
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
                "user_id": args.user_id,
                "check_date": args.check_date,
                "success": False
            }

def main():
    """
    主函数 - 用于命令行调用
    """
    parser = argparse.ArgumentParser(description='血常规模型预测')
    parser.add_argument('--user-id', type=int, required=True, help='用户ID')
    parser.add_argument('--check-date', type=str, required=True, help='检查日期 (YYYY-MM-DD)')
    parser.add_argument('--model-path', type=str, required=True, help='模型文件路径')

    args = parser.parse_args()

    try:
        # 初始化预测器
        predictor = BloodAnalysisPredictor(args.model_path)

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