package com.vikadata.api.shared.cache.service;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.vikadata.api.AbstractIntegrationTest;
import com.vikadata.api.shared.cache.bean.CategoryDto;
import com.vikadata.api.shared.cache.service.TemplateConfigCacheService;
import com.vikadata.api.template.service.ITemplatePropertyService;

import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;

/**
 * @author tao
 */
@Disabled("no assert")
public class TemplateConfigInRedisImplTest extends AbstractIntegrationTest {

    private final static String CATEGORIES_LIST_CONFIG =
            "[{\"templateIds\":[\"tpl1\",\"tpl2\"],\"categoryCode\":\"pc1\",\"categoryName\":\"cn1\"},"
                    + "{\"templateIds\":[\"tpl3\",\"tpl4\"],\"categoryCode\":\"pc2\",\"categoryName\":\"cn2\"}]";

    @SpyBean
    private TemplateConfigCacheService templateConfigCacheService;

    @SpyBean
    private ITemplatePropertyService templatePropertyService;

    @Test
    void testGetCategoriesListConfigCacheByLangFromCache() {
        String lang = "zh_CN";
        String key = StrUtil.format("vikadata:config:{}:{}", "template", lang + ":online");
        redisTemplate.opsForValue().set(key, CATEGORIES_LIST_CONFIG);
        String cache = templateConfigCacheService.getCategoriesListConfigCacheByLang(lang);
        assertThat(cache).isEqualTo(CATEGORIES_LIST_CONFIG);
        redisTemplate.delete(key);
    }

    @Test
    void testGetCategoriesListConfigCacheByLangFromDB() {
        String lang = "zh_CN";
        List<CategoryDto> categories = CollUtil.newArrayList(
                CategoryDto.builder().categoryCode("pc1")
                        .categoryName("cn1")
                        .templateIds(CollUtil.newArrayList("tpl1", "tpl2"))
                        .build(),
                CategoryDto.builder().categoryCode("pc2")
                        .categoryName("cn2")
                        .templateIds(CollUtil.newArrayList("tpl3", "tpl4"))
                        .build()
        );
        willReturn(categories).given(templatePropertyService).getCategories(lang);
        String db = templateConfigCacheService.getCategoriesListConfigCacheByLang(lang);
        assertThat(db).isEqualTo(CATEGORIES_LIST_CONFIG);
    }

}
