/**
 * 模型定价目录（对应后端 model_pricing 表，GET /models 返回）
 * 价格单位：美元 / 千 token；maxTokens 存的是上下文窗口大小
 */
export interface ModelPricing {
  id: number
  provider: string
  modelName: string
  displayName: string | null
  maxTokens: number | null
  inputPricePer1k: number | null
  outputPricePer1k: number | null
  enabled: number
  createdAt: string
}

/** 目录按 provider 分组后的一条供应商组 */
export interface ModelProviderGroup {
  key: string
  name: string
  models: ModelPricing[]
}
