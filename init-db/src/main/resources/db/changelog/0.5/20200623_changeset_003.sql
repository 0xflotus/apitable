ALTER TABLE `${table.prefix}datasheet_operation`
    ADD COLUMN `created_by` bigint(20) DEFAULT NULL COMMENT '创建用户' AFTER `is_deleted`,
    ADD COLUMN `updated_by` bigint(20) DEFAULT NULL COMMENT '最后一次更新用户' AFTER `created_by`;
