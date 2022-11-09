package com.apitable.starter.vika.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.vika.client.api.VikaApiClient;
import cn.vika.client.api.exception.ApiException;
import cn.vika.client.api.model.ApiQueryParam;
import cn.vika.client.api.model.CellFormat;
import cn.vika.client.api.model.CreateRecordRequest;
import cn.vika.client.api.model.FieldKey;
import cn.vika.client.api.model.HttpResult;
import cn.vika.client.api.model.Pager;
import cn.vika.client.api.model.Record;
import cn.vika.client.api.model.RecordMap;
import cn.vika.client.api.model.UpdateRecord;
import cn.vika.client.api.model.UpdateRecordRequest;
import cn.vika.core.utils.JacksonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apitable.starter.vika.core.model.BillingOrder;
import com.apitable.starter.vika.core.model.BillingOrderItem;
import com.apitable.starter.vika.core.model.BillingOrderPayment;
import com.apitable.starter.vika.core.model.DestinationBill;
import com.apitable.starter.vika.core.model.DingTalkAgentAppInfo;
import com.apitable.starter.vika.core.model.DingTalkDaTemplateInfo;
import com.apitable.starter.vika.core.model.DingTalkGoodsInfo;
import com.apitable.starter.vika.core.model.DingTalkOrderInfo;
import com.apitable.starter.vika.core.model.DingTalkSubscriptionInfo;
import com.apitable.starter.vika.core.model.GlobalWidgetInfo;
import com.apitable.starter.vika.core.model.GmPermissionInfo;
import com.apitable.starter.vika.core.model.IntegralRewardInfo;
import com.apitable.starter.vika.core.model.MemberField;
import com.apitable.starter.vika.core.model.OnlineTemplateInfo;
import com.apitable.starter.vika.core.model.OriginalWhite;
import com.apitable.starter.vika.core.model.RecommendTemplateInfo;
import com.apitable.starter.vika.core.model.UserContactInfo;
import com.apitable.starter.vika.core.model.UserOrder;
import com.apitable.starter.vika.core.model.template.RecommendInfo;
import com.apitable.starter.vika.core.model.template.RecommendInfo.AlbumGroup;
import com.apitable.starter.vika.core.model.template.RecommendInfo.BannerInfo;
import com.apitable.starter.vika.core.model.template.RecommendInfo.TemplateGroup;
import com.apitable.starter.vika.core.model.template.Template;
import com.apitable.starter.vika.core.model.template.TemplateAlbum;
import com.apitable.starter.vika.core.model.template.TemplateCategory;
import com.apitable.starter.vika.core.model.template.TemplateCenterConfigInfo;
import com.apitable.starter.vika.core.model.template.TemplateConfigDatasheetParam;

/**
 * <p>
 * vika sdk implementation class
 * </p>
 *
 */
public class VikaTemplate extends VikaAccessor implements VikaOperations {
    private static final Integer MAX_PAGE_SIZE = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(VikaTemplate.class);

    public VikaTemplate(String hostUrl, String token) {
        super(hostUrl, token);
    }

    @Override
    public List<GmPermissionInfo> getGmPermissionConfiguration(String dstId) {
        LOGGER.info("Get GM Permission Configuration Information");
        String resource = "ACTION";
        String role = "PERMISSION_UNIT";
        // build query criteria
        ApiQueryParam queryParam = new ApiQueryParam()
                .withFields(Arrays.asList(resource, role))
                .withFilter("{ShouldUpdate} = 1");
        // query results
        Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
        List<GmPermissionInfo> infos = new ArrayList<>(records.getTotalItems());
        while (records.hasNext()) {
            for (Record record : records.next()) {
                // get unit ids
                JSONArray jsonArray = JSONUtil.parseArray(record.getFields().get(role));
                List<Long> unitIds = new ArrayList<>();
                jsonArray.jsonIter().forEach(unit -> unitIds.add(unit.getLong("id")));
                // build information
                GmPermissionInfo info = new GmPermissionInfo(record.getFields().get(resource).toString(), unitIds);
                infos.add(info);
            }
        }
        return infos;
    }

    @Override
    public List<TemplateCenterConfigInfo> getTemplateCenterConfigInfos(String host, String token, TemplateConfigDatasheetParam param) {
        List<TemplateCenterConfigInfo> infos = new ArrayList<>();
        // get template config
        VikaApiClient client = this.getClient(host, token);
        Map<String, RecommendInfo> i18nToRecommendMap = this.getI18nToRecommendMap(client, param.getRecommendDatasheetId(), param.getRecommendViewId());
        Map<String, List<TemplateCategory>> i18nToCategoriesMap = this.getI18nToCategoriesMap(client, param.getCategoryDatasheetId(), param.getCategoryViewId());
        Map<String, List<TemplateAlbum>> i18nToAlbumsMap = this.getI18nToAlbumsMap(client, param.getAlbumDatasheetId(), param.getAlbumViewId());
        Map<String, List<Template>> i18nToTemplatesMap = this.getI18nToTemplatesMap(client, param.getTemplateDatasheetId(), param.getTemplateViewId());
        // build config info
        for (Entry<String, RecommendInfo> entry : i18nToRecommendMap.entrySet()) {
            TemplateCenterConfigInfo info = new TemplateCenterConfigInfo();
            String i18n = entry.getKey();
            info.setI18n(i18n);
            info.setRecommend(entry.getValue());
            info.setTemplateCategories(i18nToCategoriesMap.get(i18n));
            info.setAlbums(i18nToAlbumsMap.get(i18n));
            info.setTemplate(i18nToTemplatesMap.get(i18n));
            infos.add(info);
        }
        return infos;
    }

    private Map<String, RecommendInfo> getI18nToRecommendMap(VikaApiClient client, String datasheetId, String viewId) {
        // build query param
        ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                .withFilter("{CHECK} = 1")
                .withView(viewId);
        // get all records
        List<Record> records = client.getRecordApi().getRecords(datasheetId, queryParam).all();
        // convert to object
        ObjectMapper mapper = new ObjectMapper();
        List<RecommendTemplateInfo> infos = records.stream()
                .filter(record -> record.getFields() != null && !record.getFields().isEmpty())
                .map(record -> mapper.convertValue(record.getFields(), RecommendTemplateInfo.class))
                .collect(Collectors.toList());

        // build i18n to recommend map
        Map<String, List<RecommendTemplateInfo>> i18nToInfosMap = infos.stream().collect(Collectors.groupingBy(RecommendTemplateInfo::getI18n));
        Map<String, RecommendInfo> i18nToRecommendMap = new HashMap<>(i18nToInfosMap.size());
        for (Entry<String, List<RecommendTemplateInfo>> i18nEntry : i18nToInfosMap.entrySet()) {
            // build top banner info
            List<BannerInfo> top = i18nEntry.getValue().stream().filter(info -> info.getLayout().equals("BANNER"))
                    .map(info -> new BannerInfo(info.getSubjectValue(), info.getBanners().get(0).getToken(), info.getTitle(), info.getDescription(), info.getColor()))
                    .collect(Collectors.toList());

            // build album/template custom group
            List<AlbumGroup> albumGroups = new ArrayList<>();
            List<TemplateGroup> templateGroups = new ArrayList<>();
            Map<String, List<RecommendTemplateInfo>> layoutToInfosMap = i18nEntry.getValue().stream().filter(info -> !info.getLayout().equals("BANNER"))
                    .collect(Collectors.groupingBy(RecommendTemplateInfo::getLayout));
            for (Entry<String, List<RecommendTemplateInfo>> layoutEntry : layoutToInfosMap.entrySet()) {
                Map<String, List<String>> groupToSubjectsMap = layoutEntry.getValue().stream()
                        .collect(Collectors.groupingBy(RecommendTemplateInfo::getCustomGroup, Collectors.mapping(RecommendTemplateInfo::getSubjectValue, Collectors.toList())));
                switch (layoutEntry.getKey()) {
                    case "ALBUM_GROUP":
                        groupToSubjectsMap.forEach((k, v) -> albumGroups.add(new AlbumGroup(k, v)));
                        break;
                    case "TEMPLATE_GROUP":
                        groupToSubjectsMap.forEach((k, v) -> templateGroups.add(new TemplateGroup(k, v)));
                        break;
                    default:
                        break;
                }
            }
            i18nToRecommendMap.put(i18nEntry.getKey(), new RecommendInfo(top, albumGroups, templateGroups));
        }
        return i18nToRecommendMap;
    }

    private Map<String, List<TemplateCategory>> getI18nToCategoriesMap(VikaApiClient client, String datasheetId, String viewId) {
        // build query param
        ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                .withCellFormat(CellFormat.STRING)
                .withFilter("{CHECK} = 1")
                .withView(viewId);
        // get all records
        List<Record> records = client.getRecordApi().getRecords(datasheetId, queryParam).all();

        // build i18n to categories map
        Map<String, List<TemplateCategory>> i18nToCategoriesMap = new HashMap<>();
        for (Record record : records) {
            Map<String, Object> fields = record.getFields();
            if (fields == null || fields.isEmpty()) {
                continue;
            }
            String i18n = MapUtil.getStr(fields, "i18n");
            TemplateCategory category = new TemplateCategory();
            category.setName(MapUtil.getStr(fields, "TEMPLATE_CATEGORY"));
            if (fields.containsKey("TEMPLATE_ALBUM")) {
                category.setAlbumNames(Arrays.asList(MapUtil.getStr(fields, "TEMPLATE_ALBUM").split(", ")));
            }
            if (fields.containsKey("TEMPLATE")) {
                category.setTemplateNames(Arrays.asList(MapUtil.getStr(fields, "TEMPLATE").split(", ")));
            }
            if (i18nToCategoriesMap.containsKey(i18n)) {
                i18nToCategoriesMap.get(i18n).add(category);
            }
            else {
                List<TemplateCategory> categories = new ArrayList<>();
                categories.add(category);
                i18nToCategoriesMap.put(i18n, categories);
            }
        }
        return i18nToCategoriesMap;
    }

    private Map<String, List<TemplateAlbum>> getI18nToAlbumsMap(VikaApiClient client, String datasheetId, String viewId) {
        // build query param
        ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                .withCellFormat(CellFormat.STRING)
                .withFilter("{SHELF_STATUS} = 1")
                .withView(viewId);
        // get all records
        List<Record> records = client.getRecordApi().getRecords(datasheetId, queryParam).all();

        // build i18n to albums map
        Map<String, List<TemplateAlbum>> i18nToAlbumsMap = new HashMap<>();
        for (Record record : records) {
            Map<String, Object> fields = record.getFields();
            if (fields == null || fields.isEmpty()) {
                continue;
            }
            String i18n = MapUtil.getStr(fields, "i18n");
            TemplateAlbum album = new TemplateAlbum();
            album.setName(MapUtil.getStr(fields, "ALBUM_NAME"));
            album.setCover(Utils.getRelativePath(MapUtil.getStr(fields, "ALBUM_COVER")));
            album.setDescription(MapUtil.getStr(fields, "ALBUM_DESC", ""));
            album.setContent(MapUtil.getStr(fields, "ALBUM_CONTENT", "{}"));
            album.setPublisherName(MapUtil.getStr(fields, "PUBLISHER_NAME"));
            album.setPublisherLogo(Utils.getRelativePath(MapUtil.getStr(fields, "PUBLISHER_LOGO")));
            album.setPublisherDesc(MapUtil.getStr(fields, "PUBLISHER_DESC"));
            if (fields.containsKey("TEMPLATE")) {
                album.setTemplateNames(Arrays.asList(MapUtil.getStr(fields, "TEMPLATE").split(", ")));
            }
            if (fields.containsKey("TEMPLATE_TAG")) {
                album.setTemplateTags(Arrays.asList(MapUtil.getStr(fields, "TEMPLATE_TAG").split(", ")));
            }
            if (i18nToAlbumsMap.containsKey(i18n)) {
                i18nToAlbumsMap.get(i18n).add(album);
            }
            else {
                List<TemplateAlbum> albums = new ArrayList<>();
                albums.add(album);
                i18nToAlbumsMap.put(i18n, albums);
            }
        }
        return i18nToAlbumsMap;
    }

    private Map<String, List<Template>> getI18nToTemplatesMap(VikaApiClient client, String datasheetId, String viewId) {
        // build query param
        ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                .withCellFormat(CellFormat.STRING)
                .withFilter("{SHELF_STATUS} = 1")
                .withView(viewId);
        // get all records
        List<Record> records = client.getRecordApi().getRecords(datasheetId, queryParam).all();

        // build i18n to templates map
        Map<String, List<Template>> i18nToAlbumsMap = new HashMap<>();
        for (Record record : records) {
            Map<String, Object> fields = record.getFields();
            if (fields == null || fields.isEmpty()) {
                continue;
            }
            String i18n = MapUtil.getStr(fields, "i18n");
            Template template = new Template();
            template.setName(MapUtil.getStr(fields, "TEMPLATE_NAME"));
            if (fields.containsKey("TEMPLATE_TAG")) {
                template.setTemplateTags(Arrays.asList(MapUtil.getStr(fields, "TEMPLATE_TAG").split(", ")));
            }
            if (i18nToAlbumsMap.containsKey(i18n)) {
                i18nToAlbumsMap.get(i18n).add(template);
            }
            else {
                List<Template> templates = new ArrayList<>();
                templates.add(template);
                i18nToAlbumsMap.put(i18n, templates);
            }
        }
        return i18nToAlbumsMap;
    }

    @Override
    public List<RecommendTemplateInfo> getRecommendTemplateConfiguration(String dstId, String viewId, String lang) {
        LOGGER.info("Get Recommend Template Configuration Information");
        // query result
        List<RecommendTemplateInfo> list = new ArrayList<>();
        try {
            // build query criteria
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                    .withFilter(StrUtil.format("{i18n} = \"{}\" && {RULE} = 1", lang))
                    .withView(viewId);
            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
            ObjectMapper mapper = new ObjectMapper();
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Map<String, Object> fields = record.getFields();
                    if (fields != null) {
                        // get unit ids
                        RecommendTemplateInfo info = mapper.convertValue(fields, RecommendTemplateInfo.class);
                        list.add(info);
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining popular recommended template configuration", e);
        }
        return list;
    }

    @Override
    public List<OnlineTemplateInfo> getOnlineTemplateConfiguration(String dstId, String lang) {
        LOGGER.info("Get Online Template Configuration Information");
        // query result
        List<OnlineTemplateInfo> list = new ArrayList<>();
        try {
            // build query criteria
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                    .withCellFormat(CellFormat.STRING)
                    .withFilter(StrUtil.format("{i18n} = \"{}\" && {RULE} = 1", lang));
            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Map<String, Object> fields = record.getFields();
                    if (fields != null) {
                        // get unit id
                        OnlineTemplateInfo info = new OnlineTemplateInfo();
                        info.setTemplateName(fields.get("TEMPLATE_NAME").toString());
                        Object category = fields.get("CATEGORY");
                        if (category != null) {
                            info.setTemplateCategoryName(category.toString().split(", "));
                        }
                        Object tag = fields.get("TEMPLATE_TAG");
                        if (tag != null) {
                            info.setTemplateTagName(tag.toString().split(", "));
                        }
                        list.add(info);
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining online template configuration", e);
        }
        return list;
    }

    @Override
    public List<String> getTemplateCategoryName(String dstId, String viewId, String lang) {
        LOGGER.info("Get Template Category Name Configuration Information");
        // query results
        List<String> list = new ArrayList<>();
        try {
            // build query criteria
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                    .withCellFormat(CellFormat.STRING)
                    .withFilter(StrUtil.format("{i18n} = \"{}\"", lang))
                    .withFields(CollUtil.newArrayList("TEMPLATE_CATEGORY"))
                    .withView(viewId);
            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Map<String, Object> fields = record.getFields();
                    if (fields != null) {
                        // get unit ids
                        Object templateCateGory = fields.get("TEMPLATE_CATEGORY");
                        list.add((String) templateCateGory);
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining template category configuration", e);
        }
        return list;
    }

    @Override
    public List<GlobalWidgetInfo> getGlobalWidgetPackageConfiguration(String dstId) {
        LOGGER.info("Get Widget Package Configuration Information");
        // query results
        List<GlobalWidgetInfo> result = new ArrayList<>();
        try {
            // build query criteria
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE).withCellFormat(CellFormat.STRING);

            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
            int widgetSort = 1;
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Map<String, Object> fields = record.getFields();
                    if (fields != null) {
                        GlobalWidgetInfo globalWidgetInfo = new GlobalWidgetInfo();
                        globalWidgetInfo.setPackageId(MapUtil.getStr(fields, "package_id"));
                        globalWidgetInfo.setPackageName(MapUtil.getStr(fields, "小组件名称"));
                        globalWidgetInfo.setIsEnabled(MapUtil.getBool(fields, "是否生效"));
                        globalWidgetInfo.setIsTemplate(MapUtil.getBool(fields, "是否模版"));
                        globalWidgetInfo.setVersion(MapUtil.getStr(fields, "版本"));
                        String openSourceAddres = MapUtil.getStr(fields, "开源地址");
                        if (StrUtil.isNotBlank(openSourceAddres)) {
                            globalWidgetInfo.setOpenSourceAddres(openSourceAddres);
                        }
                        String templateCover = MapUtil.getStr(fields, "模版封面图");
                        if (StrUtil.isNotBlank(templateCover)) {
                            // Get the image address of the relative path
                            globalWidgetInfo.setTemplateCover(StrUtil.removePrefix(URLUtil.getPath(ReUtil.getGroup1("\\((.+)\\)", templateCover)), "/"));
                        }
                        String website = MapUtil.getStr(fields, "小程序官网地址");
                        if (StrUtil.isNotBlank(website)) {
                            globalWidgetInfo.setWebsite(website);
                        }
                        globalWidgetInfo.setWidgetSort(widgetSort);
                        result.add(globalWidgetInfo);
                    }
                    widgetSort++;
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining online template configuration", e);
        }
        return result;
    }

    @Override
    public List<DingTalkAgentAppInfo> getDingTalkAgentAppConfiguration(String dstId) {
        LOGGER.info("Get ding talk agent app configuration");
        // query results
        List<DingTalkAgentAppInfo> list = new ArrayList<>();
        try {
            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Map<String, Object> fields = record.getFields();
                    if (fields != null && fields.get("rules") != null && fields.get("canDeploy") != null && fields.get("deletedAt") == null) {
                        // get uint ids
                        DingTalkAgentAppInfo info = BeanUtil.mapToBean(record.getFields(), DingTalkAgentAppInfo.class, true,
                                null);
                        list.add(info);
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining customized application configuration of DingTalk, please confirm whether it is normal?");
        }
        return list;
    }

    @Override
    public void saveStatisticsData(String dstId, String data) {
        // put record map into fields name, warp record into array node
        ObjectNode fieldMap = JsonNodeFactory.instance.objectNode().put("DATA", data);
        ObjectNode fields = JsonNodeFactory.instance.objectNode().set("fields", fieldMap);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(fields);
        try {
            // convert json to Map List
            List<RecordMap> recordMaps = JacksonConverter.unmarshalToList(RecordMap.class, arrayNode);
            // create record request
            CreateRecordRequest recordRequest = new CreateRecordRequest().withRecords(recordMaps);
            this.getClient().getRecordApi().addRecords(dstId, recordRequest);
        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<DingTalkGoodsInfo> getDingTalkGoodsInfo(String token, String host, String dstId, String featureDstId) {
        LOGGER.info("Get ding talk goods configuration");
        // query result
        List<DingTalkGoodsInfo> list = new ArrayList<>();
        Map<String, Record> featureMap = getBillingFeatures(token, host, featureDstId);
        if (featureMap.isEmpty()) {
            LOGGER.error("subscription plan property is empty");
            return list;
        }
        try {
            // build query criteria
            // field in order: ID，features , DingTalk specification code（domestic goods）,DingTalk specification code（paid application）,product
            // Filter by payment channel=DingTalk
            String featureField = "fldHU39KnAaUZ";
            String idField = "fldasdYwFe6mU";
            String internalGoodsField = "fldLSeE3vBxgy";
            String payGoodsField = "fldUOhzNbPG0q";
            String productField = "fldsL3S0yH3q7";
            String periodField = "fldm41L3XIYm3";
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                    .withCellFormat(CellFormat.STRING)
                    .withFieldKey(FieldKey.ID)
                    .withFields(ListUtil.toList(idField, featureField, internalGoodsField, payGoodsField,
                            productField, periodField))
                    .withFilter("{fld59r87u40rS} = '钉钉'");
            Pager<Record> records = this.getClient(host, token).getRecordApi().getRecords(dstId, queryParam);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Integer seats = null;
                    Long capacity = null;
                    Integer nodes = null;
                    if (record.getFields().get(featureField) == null) {
                        continue;
                    }
                    String[] featureIds = record.getFields().get(featureField).toString().split(", ");
                    String period = record.getFields().get(periodField).toString();
                    String id = record.getFields().get(idField).toString();
                    if (!id.contains(period.toLowerCase(Locale.ROOT))) {
                        LOGGER.error("There is a problem with the DingTalk subscription plan configuration:{}", id);
                        continue;
                    }
                    for (Object featureId : featureIds) {
                        if (featureMap.containsKey(featureId.toString())) {
                            // DingTalk only considers seats and capacity
                            Record feature = featureMap.get(featureId.toString());
                            Object function = feature.getFields().get("function");
                            if (ObjectUtil.equal(function, "seats")) {
                                seats = Integer.parseInt(feature.getFields().get("specification").toString());
                            }
                            if (ObjectUtil.equal(function, "storage_capacity")) {
                                capacity = Long.parseLong(feature.getFields().get("specification").toString());
                            }
                            if (ObjectUtil.equal(function, "nodes")) {
                                nodes = Integer.parseInt(feature.getFields().get("specification").toString());
                            }
                        }
                    }
                    // One subscription plan has two sku codes
                    if (seats != null && capacity != null && nodes != null) {
                        if (record.getFields().get(internalGoodsField) != null) {
                            DingTalkGoodsInfo info = new DingTalkGoodsInfo();
                            info.setSeats(seats);
                            info.setCapacity(capacity);
                            info.setNodes(nodes);
                            info.setPeriod(period);
                            info.setItemCode(record.getFields().get(internalGoodsField).toString());
                            info.setBillingPlanId(id);
                            info.setProduct(record.getFields().get(productField).toString());
                            info.setInternal(true);
                            list.add(info);
                        }
                        if (record.getFields().get(payGoodsField) != null) {
                            DingTalkGoodsInfo info = new DingTalkGoodsInfo();
                            info.setSeats(seats);
                            info.setCapacity(capacity);
                            info.setNodes(nodes);
                            info.setPeriod(period);
                            info.setItemCode(record.getFields().get(payGoodsField).toString());
                            info.setBillingPlanId(id);
                            info.setProduct(record.getFields().get(productField).toString());
                            info.setInternal(false);
                            list.add(info);
                        }
                    }
                    else {
                        LOGGER.error("There is a problem with the DingTalk subscription plan configuration:{}", record.getFields().get(idField));
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining customized application configuration of DingTalk, please confirm whether it is normal?");
        }
        return list;
    }


    @Override
    public Map<String, Record> getBillingFeatures(String token, String host, String featureDstId) {
        LOGGER.info("Get billing features configuration");
        // query result
        Map<String, Record> result = new HashMap<>();
        try {
            // build query criteria
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                    .withCellFormat(CellFormat.STRING)
                    .withFields(ListUtil.toList("id", "function", "specification"));
            Pager<Record> records = this.getClient(host, token).getRecordApi().getRecords(featureDstId, queryParam);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    result.put(record.getFields().get("id").toString(), record);
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining subscription plan property configuration, please confirm whether it is normal?");
        }
        return result;
    }

    @Override
    public void saveDingTalkSubscriptionInfo(String dstId, DingTalkSubscriptionInfo subscriptionInfo) {
        // put record map into fields name, warp record into array node
        ObjectNode fieldMap = JsonNodeFactory.instance.objectNode()
                .put("SPACE_ID", subscriptionInfo.getSpaceId())
                .put("SPACE_NAME", subscriptionInfo.getSpaceName())
                .put("ORDER_TYPE", subscriptionInfo.getOrderType())
                .put("GOODS_CODE", subscriptionInfo.getGoodsCode())
                .put("SUBSCRIPTION_TYPE", subscriptionInfo.getSubscriptionType())
                .put("SEAT", subscriptionInfo.getSeat())
                .put("SERVICE_START_TIME", subscriptionInfo.getServiceStartTime())
                .put("SERVICE_STOP_TIME", subscriptionInfo.getServiceStopTime())
                .put("ORDER_LABEL", subscriptionInfo.getOrderLabel())
                .put("DATA", subscriptionInfo.getData());
        ObjectNode fields = JsonNodeFactory.instance.objectNode().set("fields", fieldMap);
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(fields);
        try {
            // convert json to Map List
            List<RecordMap> recordMaps = JacksonConverter.unmarshalToList(RecordMap.class, arrayNode);
            // create record request
            CreateRecordRequest recordRequest = new CreateRecordRequest().withRecords(recordMaps);
            this.getClient().getRecordApi().addRecords(dstId, recordRequest);
        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<DingTalkOrderInfo> getDingTalkOrderInfoList(String dstId, String viewId) {
        LOGGER.info("Get DingTalk subscription order information");
        // query results
        Map<String, DingTalkOrderInfo> result = new HashMap<>();
        String orderIdField = "order_id";
        String goodsCodeField = "goods_code";
        String itemCodeField = "item_code";
        String corpIdField = "corp_id";
        String planIdField = "plan_id";
        String subscriptionTypeField = "subscription_type";
        String createdAtField = "created_at";
        try {
            // build query criteria
            ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                    .withCellFormat(CellFormat.STRING)
                    .withFieldKey(FieldKey.Name)
                    .withView(viewId)
                    .withFields(ListUtil.toList(orderIdField, goodsCodeField, itemCodeField, corpIdField, planIdField,
                            subscriptionTypeField, createdAtField));
            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    if (ObjectUtil.isNotNull(record.getFields())) {
                        DingTalkOrderInfo orderInfo = BeanUtil.toBean(record.getFields(), DingTalkOrderInfo.class);
                        result.put(orderInfo.getOrderId(), orderInfo);
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining subscription plan property configuration, please confirm whether it is normal?");
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public List<Map<String, Object>> fetchCustomerOrder(String host, String token, String dstId, String fiter) {
        ApiQueryParam queryParam = new ApiQueryParam()
                .withView("viwjYkzg2yXzw")
                .withFieldKey(FieldKey.Name)
                .withFilter(fiter);
        List<Record> records = this.getClient(host, token).getRecordApi().getRecords(dstId, queryParam).all();
        List<Map<String, Object>> result = new ArrayList<>();
        records.forEach(record -> result.add(record.getFields()));
        return result;
    }

    @Override
    public OriginalWhite fetchRecordOnWhiteList(String host, String token, String dstId) {
        // build query criteria
        ApiQueryParam queryParam = new ApiQueryParam(1, 1)
                .withView("viwfShmNeVy1e")
                .withCellFormat(CellFormat.STRING)
                .withFieldKey(FieldKey.Name)
                .withFilter("{迁移状态}='未处理'");
        Pager<Record> pager = this.getClient(host, token).getRecordApi().getRecords(dstId, queryParam);
        List<Record> records = pager.first();
        if (records.isEmpty()) {
            return null;
        }
        Record record = CollUtil.getFirst(records);
        Map<String, Object> recordMap = record.getFields();
        OriginalWhite white = new OriginalWhite();
        white.setRecordId(record.getRecordId());
        white.setSpaceId(MapUtil.getStr(recordMap, "空间站ID"));
        white.setSpaceName(MapUtil.getStr(recordMap, "空间站名称"));
        white.setCustomers(MapUtil.getStr(recordMap, "客户/企业"));
        white.setCustomerContact(MapUtil.getStr(recordMap, "联系方式"));
        white.setBillingProduct(MapUtil.getStr(recordMap, "基础订阅"));
        white.setBillingSeat(MapUtil.getInt(recordMap, "订阅席位"));
        white.setBillingDuration(MapUtil.getInt(recordMap, "订阅时长（单位：月）"));
        white.setPlans(MapUtil.get(recordMap, "订阅计划", new TypeReference<List<String>>() {}));
        white.setStartDate(MapUtil.getLong(recordMap, "开通时间"));
        white.setContact(MapUtil.get(recordMap, "成交人", new TypeReference<List<String>>() {}));
        white.setOriginalCreateDate(MapUtil.getLong(recordMap, "创建时间"));
        white.setDescription(MapUtil.getStr(recordMap, "备注"));
        white.setMigrateStatus(MapUtil.getStr(recordMap, "迁移状态"));
        return white;
    }

    @Override
    public void updateWhiteMigrateStatus(String host, String token, String dstId, String recordId, String statusName) {
        UpdateRecord record = new UpdateRecord()
                .withRecordId(recordId)
                .withField("迁移状态", statusName);
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest()
                .withRecords(Collections.singletonList(record));
        this.getClient(host, token).getRecordApi().updateRecords(dstId, updateRecordRequest);
    }

    @Override
    public void addRecordToBillingSheet(OriginalWhite originalWhite) {
        DestinationBill bill = new DestinationBill();
        bill.setSpaceId(originalWhite.getSpaceId());
        bill.setSpaceName(originalWhite.getSpaceName());
        // customer
        bill.setCustomers(originalWhite.getCustomers());
        bill.setCustomerContact(originalWhite.getCustomerContact());
        bill.setBillingType("开通");
        // counterpart
        if (CollUtil.isNotEmpty(originalWhite.getContact())) {
            List<MemberField> contact = originalWhite.getContact().stream().reduce(new ArrayList<>(),
                    (members, item) -> {
                        MemberField member = new MemberField();
                        member.setType("Member");
                        member.setName(item);
                        members.add(member);
                        return members;
                    },
                    (members, childMembers) -> {
                        members.addAll(childMembers);
                        return members;
                    });
            bill.setContact(contact);
        }
        if ("白银级".equals(originalWhite.getBillingProduct())) {
            bill.setBillingProduct("白银级空间（容量5G/人）");
        }
        else if ("企业级".equals(originalWhite.getBillingProduct())) {
            bill.setBillingProduct("企业级空间（容量10G/人）");
        }
        bill.setSeat(originalWhite.getBillingSeat());
        bill.setDuration(originalWhite.getBillingDuration());
        bill.setPlanName(originalWhite.getPlans());
        bill.setStartDate(originalWhite.getStartDate());
        bill.setOriginalCreateDate(originalWhite.getOriginalCreateDate());
        bill.setEnvironment("正式环境(vika.cn)");
        bill.setAttachments(originalWhite.getAttachments());
        bill.setRemark(originalWhite.getDescription());

        List<RecordMap> recordMaps = Collections.singletonList(new RecordMap().withFields(JacksonConverter.toMap(bill)));
        CreateRecordRequest recordRequest = new CreateRecordRequest()
                .withFieldKey(FieldKey.Name)
                .withRecords(recordMaps);
        getClient().getRecordApi().addRecords("dstR2lJMncE2hC79rr", recordRequest);
    }

    @Override
    public List<DingTalkDaTemplateInfo> getDingTalkDaTemplateInfo(String dstId, String viewId) {
        LOGGER.info("Get ding talk da template configuration");
        String templateIdField = "模版ID";
        String templateIconField = "dingtalk_icon";
        String templateNameField = "TEMPLATE_NAME";
        // query results
        List<DingTalkDaTemplateInfo> list = new ArrayList<>();
        ApiQueryParam queryParam = new ApiQueryParam(1, MAX_PAGE_SIZE)
                .withFieldKey(FieldKey.Name)
                .withFields(ListUtil.toList(templateIdField, templateIconField, templateNameField))
                .withView(viewId);
        try {
            Pager<Record> records = this.getClient().getRecordApi().getRecords(dstId, queryParam);
            while (records.hasNext()) {
                for (Record record : records.next()) {
                    Map<String, Object> fields = record.getFields();
                    if (fields != null) {
                        Object templateId = fields.get(templateIdField);
                        Object templateIcon = fields.get(templateIconField);
                        Object templateName = fields.get(templateNameField);
                        if (templateId != null && templateIcon != null) {
                            DingTalkDaTemplateInfo info = new DingTalkDaTemplateInfo();
                            info.setTemplateId(templateId.toString());
                            info.setTemplateName(templateName.toString());
                            JSONArray icons = JSONUtil.parseArray(templateIcon);
                            JSONObject icon = JSONUtil.parseObj(icons.get(0));
                            info.setIconUrl(icon.get("url").toString());
                            info.setIconName(icon.get("name").toString());
                            list.add(info);
                        }
                        else {
                            LOGGER.error("DingTalk template configuration error:{}:{}:{}", templateName, templateId, templateIcon);
                        }
                    }
                }
            }
        }
        catch (ApiException e) {
            LOGGER.error("Exception in obtaining the template of DingTalk, please confirm whether it is normal?", e);
        }
        return list;
    }

    @Override
    public UserOrder fetchOrderData() {
        ApiQueryParam queryParam = new ApiQueryParam(1, 1)
                .withView("viwEz1K51L12R")
                .withCellFormat(CellFormat.STRING)
                .withFieldKey(FieldKey.Name)
                .withFilter("AND({处理状态}='未处理', {审核上线}=TRUE())");
        Pager<Record> pager = this.getClient().getRecordApi().getRecords("dstR2lJMncE2hC79rr", queryParam);
        List<Record> records = pager.first();
        if (records.isEmpty()) {
            return null;
        }
        Record record = CollUtil.getFirst(records);
        Map<String, Object> recordMap = record.getFields();
        UserOrder userOrder = new UserOrder();
        userOrder.setRecordId(record.getRecordId());
        userOrder.setSpaceId(MapUtil.getStr(recordMap, "空间站ID"));
        userOrder.setPlanName(MapUtil.getStr(recordMap, "付费计划"));
        userOrder.setStartDate(MapUtil.getStr(recordMap, "权益生效时间"));
        userOrder.setMonths(MapUtil.getInt(recordMap, "周期时长(单位:月)"));
        return userOrder;
    }

    @Override
    public void updateOrderHandleStatus(String recordId, String statusName, String statusDesc) {
        UpdateRecord record = new UpdateRecord()
                .withRecordId(recordId)
                .withField("处理状态", statusName)
                .withField("处理备注", statusDesc);
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest()
                .withRecords(Collections.singletonList(record));
        this.getClient().getRecordApi().updateRecords("dstR2lJMncE2hC79rr", updateRecordRequest);
    }

    @Override
    public boolean executeCommand(String datasheetId, Map<String, Object> request) {
        try {
            HttpResult<Object> result = this.getExecuteCommandApi().executeCommand(datasheetId, request);
            return result.isSuccess();
        }
        catch (ApiException e) {
            LOGGER.error("Exception in executing custom Cmd：", e);
            return false;
        }
    }

    @Override
    public List<IntegralRewardInfo> getIntegralRewardInfo(String host, String token, String dstId, String viewId) {
        // build query criteria
        ApiQueryParam queryParam = new ApiQueryParam()
                .withView(viewId)
                .withFilter("{RULE} = 1");
        // query results
        Pager<Record> records = this.getClient(host, token).getRecordApi().getRecords(dstId, queryParam);
        List<IntegralRewardInfo> infos = new ArrayList<>(records.getTotalItems());
        while (records.hasNext()) {
            for (Record record : records.next()) {
                Map<String, Object> recordMap = record.getFields();
                // build information
                IntegralRewardInfo info = new IntegralRewardInfo();
                info.setRecordId(record.getRecordId());
                info.setAreaCode(MapUtil.getStr(recordMap, "AREA_CODE"));
                info.setTarget(MapUtil.getStr(recordMap, "TARGET"));
                info.setCount(MapUtil.getInt(recordMap, "COUNT"));
                info.setActivityName(MapUtil.getStr(recordMap, "ACTIVITY_NAME"));
                infos.add(info);
            }
        }
        return infos;
    }

    @Override
    public void updateIntegralRewardResult(String host, String token, String dstId, String recordId, String result, String processor) {
        UpdateRecord record = new UpdateRecord()
                .withRecordId(recordId)
                .withField("RESULT", result)
                .withField("PROCESS_TIME", LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .withField("PROCESSOR", processor);
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest()
                .withRecords(Collections.singletonList(record));
        this.getClient(host, token).getRecordApi().updateRecords(dstId, updateRecordRequest);
    }

    @Override
    public void syncOrder(BillingOrder order, List<BillingOrderItem> items, List<BillingOrderPayment> payments) {
        URI uri = URLUtil.toURI(getHostUrl());
        String env = Arrays.stream(uri.getHost().split("\\.")).iterator().next();
        JSONObject config = loadOrderConfig(env);
        // order list
        CreateRecordRequest orderRecord = new CreateRecordRequest()
                .withFieldKey(FieldKey.Name)
                .withRecords(Collections.singletonList(new RecordMap().withFields(JacksonConverter.toMap(order))));
        List<Record> records = getClient().getRecordApi().addRecords(config.getStr("order"), orderRecord);
        String recordId = records.iterator().next().getRecordId();

        // order detail
        List<RecordMap> orderItemRecordMaps = new ArrayList<>();
        items.forEach(item -> {
            item.setOrderIds(Collections.singletonList(recordId));
            orderItemRecordMaps.add(new RecordMap().withFields(JacksonConverter.toMap(item)));
        });
        getClient().getRecordApi().addRecords(config.getStr("order_item"), new CreateRecordRequest()
                .withFieldKey(FieldKey.Name)
                .withRecords(orderItemRecordMaps));

        // pay detail
        if (CollUtil.isEmpty(payments)) {
            return;
        }
        List<RecordMap> orderPaymentRecordMaps = new ArrayList<>();
        payments.forEach(item -> {
            item.setOrderIds(Collections.singletonList(recordId));
            orderPaymentRecordMaps.add(new RecordMap().withFields(JacksonConverter.toMap(item)));
        });
        getClient().getRecordApi().addRecords(config.getStr("order_payment"), new CreateRecordRequest()
                .withFieldKey(FieldKey.Name)
                .withRecords(orderPaymentRecordMaps));
    }

    @Override
    public List<UserContactInfo> getUserIdFromDatasheet(String host, String datasheetId, String viewId, String token) {
        // build return object
        List<UserContactInfo> userContactInfos = new ArrayList<>();
        // build query condition
        ApiQueryParam apiQueryParam = new ApiQueryParam()
                .withView(viewId);
        // read user's id from datasheet by vika api
        Pager<Record> records = this.getClient(host, token).getRecordApi().getRecords(datasheetId, apiQueryParam);
        while (records.hasNext()) {
            for (Record record : records.next()) {
                Map<String, Object> recordMap = record.getFields();
                UserContactInfo userContactInfo = new UserContactInfo();
                userContactInfo.setRecordId(record.getRecordId());
                userContactInfo.setUuid(MapUtil.getStr(recordMap, "USER_ID"));
                userContactInfos.add(userContactInfo);
            }
        }
        return userContactInfos;
    }

    @Override
    public void writeBackUserContactInfo(String host, String token, String dstId, UserContactInfo userContactInfo) {
        UpdateRecord updateRecord = new UpdateRecord()
                .withRecordId(userContactInfo.getRecordId())
                .withField("AREA_CODE", userContactInfo.getCode())
                .withField("MOBILE_PHONE", userContactInfo.getMobilePhone())
                .withField("EMAIL", userContactInfo.getEmail())
                .withField("STATUS", true);
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest()
                .withRecords(Collections.singletonList(updateRecord));
        this.getClient(host, token).getRecordApi().updateRecords(dstId, updateRecordRequest);
    }

    private JSONObject loadOrderConfig(String env) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/order.json");
        assert resourceAsStream != null;
        InputStreamReader reader = new InputStreamReader(resourceAsStream);
        String json = IoUtil.read(reader, true);
        return JSONUtil.parseObj(json).getJSONObject(env);
    }
}
