package com.vikadata.api.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.vikadata.api.space.mapper.SpaceMenuMapper;
import com.vikadata.api.space.service.ISpaceMenuService;
import com.vikadata.entity.SpaceMenuEntity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SpaceMenuServiceImpl extends ServiceImpl<SpaceMenuMapper, SpaceMenuEntity> implements ISpaceMenuService {

	@Override
	public SpaceMenuEntity findByMenuCode(String menuCode) {
		log.info("find the space menu by menu code");
		List<SpaceMenuEntity> allSpaceMenuList = baseMapper.selectList(null);
		return CollUtil.findOne(allSpaceMenuList, menuEntity -> menuEntity.getMenuCode().equals(menuCode));
	}
}
