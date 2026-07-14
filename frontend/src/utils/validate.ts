import type { FormItemRule } from 'element-plus'

export const requiredRule = (label: string): FormItemRule => ({
  required: true,
  message: `${label}不能为空`,
  trigger: 'blur',
})

export const emailRule: FormItemRule = {
  type: 'email',
  message: '请输入正确的邮箱格式',
  trigger: 'blur',
}

export const urlRule: FormItemRule = {
  type: 'url',
  message: '请输入正确的 URL 格式',
  trigger: 'blur',
}

export const nameRule = (label: string): FormItemRule => ({
  min: 1,
  max: 50,
  message: `${label}长度需在 1-50 个字符之间`,
  trigger: 'blur',
})

export const maxLengthRule = (max: number): FormItemRule => ({
  max,
  message: `不能超过 ${max} 个字符`,
  trigger: 'blur',
})

export const positiveIntRule: FormItemRule = {
  type: 'integer',
  min: 1,
  message: '必须为正整数',
  trigger: 'blur',
}
