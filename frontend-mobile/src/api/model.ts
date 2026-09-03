import http from './http'
import type { ApiResponse, ModelCatalogItem } from '@/types'

/** 系统真实接入的模型目录（backend model_pricing 表中启用的模型） */
export async function getModelCatalog(): Promise<ApiResponse<ModelCatalogItem[]>> {
  const res = await http.get<ApiResponse<ModelCatalogItem[]>>('/models')
  return res.data
}
