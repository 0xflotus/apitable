import { generateRandomString } from '@apitable/core';
import { INestApplication } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { enableSwagger } from 'app.environment';
import { NodeRepository } from 'database/repositories/node.repository';
import { DatasheetMetaService } from 'database/services/datasheet/datasheet.meta.service';
import { DatasheetService } from 'database/services/datasheet/datasheet.service';
import { DeveloperService } from 'database/services/developer/developer.service';
import { FastifyInstance } from 'fastify';
import {
  CheckboxFieldPropertyDto,
  CurrencyFieldPropertyDto,
  DateTimeFieldPropertyDto,
  FormulaFieldPropertyDto,
  LinkFieldPropertyDto,
  LookupFieldPropertyDto,
  MemberFieldPropertyDto,
  NumberFieldPropertyDto,
  RatingFieldPropertyDto,
  SelectFieldPropertyDto,
  SingleTextPropertyDto,
  UserPropertyDto,
} from 'fusion/dtos/field.property.dto';
import { MiddlewareModule } from 'shared/middleware/middleware.module';
import { SharedModule } from 'shared/shared.module';
import {
  AUTHORIZATION_PREFIX,
  DATASHEET_ENRICH_SELECT_FIELD,
  DATASHEET_HTTP_DECORATE,
  DATASHEET_LINKED,
  DATASHEET_MEMBER_FIELD,
  DATASHEET_META_HTTP_DECORATE,
  NODE_INFO,
  REQUEST_AT,
  REQUEST_HOOK_FOLDER,
  REQUEST_HOOK_PRE_NODE,
  REQUEST_ID,
  SERVER_TIME,
  SPACE_ID_HTTP_DECORATE,
  SwaggerConstants,
  USER_HTTP_DECORATE,
} from '../common';
import { FusionApiVersion } from '../enums';
import { GlobalModule } from '../global.module';

export const initSwagger = (app: INestApplication) => {
  // wouldn't be enabled in production
  if (enableSwagger) {
    const options = new DocumentBuilder()
      .setTitle(SwaggerConstants.TITLE)
      .setDescription(SwaggerConstants.DESCRIPTION)
      .setVersion(FusionApiVersion.V10)
      .addBearerAuth({
        type: 'http',
        description: 'developer token',
      })
      .addCookieAuth('SESSION')
      .build();
    const document = SwaggerModule.createDocument(app, options, {
      extraModels: [
        SingleTextPropertyDto,
        NumberFieldPropertyDto,
        CurrencyFieldPropertyDto,
        SelectFieldPropertyDto,
        MemberFieldPropertyDto,
        UserPropertyDto,
        CheckboxFieldPropertyDto,
        RatingFieldPropertyDto,
        DateTimeFieldPropertyDto,
        LinkFieldPropertyDto,
        LookupFieldPropertyDto,
        FormulaFieldPropertyDto,
      ],
    });
    SwaggerModule.setup('nest/v1/docs', app, document);
  }
};

export const initHttpHook = (app: INestApplication) => {
  const fastify = app.getHttpAdapter().getInstance() as FastifyInstance;
  fastify.decorateRequest(USER_HTTP_DECORATE, null);
  fastify.decorateRequest(DATASHEET_META_HTTP_DECORATE, null);
  fastify.decorateRequest(DATASHEET_LINKED, null);
  fastify.decorateRequest(DATASHEET_ENRICH_SELECT_FIELD, null);
  fastify.decorateRequest(DATASHEET_MEMBER_FIELD, null);
  // TODO: REQUEST_ID should be returned by api-gateway, so that we could trace all the services
  // TODO: support multiple languages
  fastify.decorateRequest(REQUEST_ID, null);
  fastify.decorateRequest(REQUEST_AT, null);

  fastify.addHook('preHandler', async request => {
    request[REQUEST_AT] = Date.now();
    request[REQUEST_ID] = generateRandomString();
    if (request.headers.authorization) {
      const developerService = app.select(MiddlewareModule).get(DeveloperService);
      request[USER_HTTP_DECORATE] = await developerService.getUserInfoByApiKey(request.headers.authorization.substr(AUTHORIZATION_PREFIX.length));
    }
    if (request.params['spaceId']) {
      request[SPACE_ID_HTTP_DECORATE] = request.params['spaceId'];
    }
    if (request.params['nodeId']) {
      const nodeRepository = app.select(GlobalModule).get(NodeRepository);
      const nodeInfo = await nodeRepository.getNodeInfo(request.params['nodeId']);
      if (nodeInfo) {
        request[NODE_INFO] = nodeInfo;
        request[SPACE_ID_HTTP_DECORATE] = nodeInfo.spaceId;
      }
    }
    // datasheetId param should be defined in the fusion api controller by query parameter(datasheets/:datasheetId)
    if (request.params['datasheetId']) {
      const datasheetService = app.select(SharedModule).get(DatasheetService);
      const datasheet = await datasheetService.getDatasheet(request.params['datasheetId']);
      if (datasheet) {
        // TODO: should be optimized
        request[DATASHEET_HTTP_DECORATE] = datasheet;
        request[SPACE_ID_HTTP_DECORATE] = datasheet.spaceId;
        const metaService = app.select(SharedModule).get(DatasheetMetaService);
        request[DATASHEET_META_HTTP_DECORATE] = await metaService.getMetaDataByDstId(request.params['datasheetId']);
        request[DATASHEET_LINKED] = {};
        request[DATASHEET_ENRICH_SELECT_FIELD] = {};
        request[DATASHEET_MEMBER_FIELD] = new Set();
      }
    }
    if (request.body && request.body['folderId']) {
      const nodeRepository = app.select(GlobalModule).get(NodeRepository);
      const folderId = request.body['folderId'];
      const folder = await nodeRepository.getNodeInfo(folderId);
      request[REQUEST_HOOK_FOLDER] = folder;
    }
    if (request.body && request.body['preNodeId']) {
      const nodeRepository = app.select(GlobalModule).get(NodeRepository);
      const preNodeId = request.body['preNodeId'];
      const preNode = await nodeRepository.getNodeInfo(preNodeId);
      request[REQUEST_HOOK_PRE_NODE] = preNode;
    }
    return;
  });
  fastify.addHook('onSend', (request, reply, payload, done) => {
    // add request-id to Headers
    // TODO: REQUEST_ID should be returned by api-gateway, so that we could trace all the services
    reply.header(REQUEST_ID, request[REQUEST_ID]);
    const serverTime = Date.now() - request[REQUEST_AT];
    reply.header(SERVER_TIME, 'total;dur=' + serverTime);
    done();
  });
};
