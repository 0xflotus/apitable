package com.vikadata.api.shared.support.serializer;

import java.io.IOException;

import cn.hutool.core.util.BooleanUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.vikadata.api.shared.holder.SpaceHolder;
import com.vikadata.api.space.vo.SpaceGlobalFeature;
import com.vikadata.api.shared.util.information.InformationUtil;

/**
 * <p>
 * Mobile phone number encryption serialization
 * </p>
 *
 * @author Chambers
 */
public class MobilePhoneHideSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        SpaceGlobalFeature feature = SpaceHolder.getGlobalFeature();
        if (feature == null || BooleanUtil.isTrue(feature.getMobileShowable())) {
            gen.writeString(value);
            return;
        }
        gen.writeString(InformationUtil.hideMiddle(value));
    }
}
