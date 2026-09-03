import http from './index'
import type { ApiResponse } from '@/types/common'
import type { ModelPricing, ModelProviderGroup } from '@/types/model'

/** GET /models —— 启用中的模型目录（来自 model_pricing 表） */
export async function getModelCatalog(): Promise<ApiResponse<ModelPricing[]>> {
  const res = await http.get<ApiResponse<ModelPricing[]>>('/models')
  return res.data
}

/** 供应商展示名与 OpenAI 兼容 Base URL（选择供应商时自动回填连接配置；Anthropic 需经兼容网关） */
export const PROVIDER_META: Record<string, { name: string; baseUrl?: string }> = {
  deepseek: { name: 'DeepSeek', baseUrl: 'https://api.deepseek.com' },
  zhipu: { name: '智谱 GLM', baseUrl: 'https://open.bigmodel.cn/api/paas/v4' },
  moonshot: { name: 'Kimi 月之暗面', baseUrl: 'https://api.moonshot.cn/v1' },
  minimax: { name: 'MiniMax', baseUrl: 'https://api.minimaxi.com/v1' },
  xiaomi: { name: '小米 MiMo', baseUrl: 'https://api.xiaomimimo.com/v1' },
  openai: { name: 'OpenAI', baseUrl: 'https://api.openai.com/v1' },
  anthropic: { name: 'Anthropic（需兼容网关）' },
}

/** 把目录按 provider 分组（保持后端返回顺序：provider、id 升序） */
export function groupModelsByProvider(list: ModelPricing[]): ModelProviderGroup[] {
  const groups: ModelProviderGroup[] = []
  for (const m of list) {
    let g = groups.find(x => x.key === m.provider)
    if (!g) {
      g = { key: m.provider, name: PROVIDER_META[m.provider]?.name || m.provider, models: [] }
      groups.push(g)
    }
    g.models.push(m)
  }
  return groups
}
