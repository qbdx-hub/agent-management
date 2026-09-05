-- 成员终端权限开关：移动端终端从「仅 owner/admin」放开为空间级可配置
-- 1=空间成员均可用（默认放开，回填存量行）；0=仅空间 owner/admin 可用
-- 开关由 PC 端「空间设置」管理（PUT /workspaces/{id}/settings，仅 owner/admin 可改）

ALTER TABLE workspace
  ADD COLUMN member_terminal_enabled TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '成员终端开关：0-终端仅空间 owner/admin 可用，1-空间成员均可用（默认放开）';
