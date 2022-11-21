import { LoggerService, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { FastifyAdapter, NestFastifyApplication } from '@nestjs/platform-fastify';
import * as Sentry from '@sentry/node';
import * as Tracing from '@sentry/tracing';
import { Client } from '@sentry/types';
import { environment, isDevMode } from 'app.environment';
import { AppModule } from 'app.module';
import { useContainer } from 'class-validator';
import helmet from 'fastify-helmet';
import fastifyMultipart from 'fastify-multipart';
import { protobufPackage } from 'grpc/generated/serving/SocketServingService';
import { I18nService } from 'nestjs-i18n';
import { join } from 'path';
import { initHttpHook, initSwagger } from 'shared/adapters/adapters.init';
import { APPLICATION_NAME, GRPC_MAX_PACKAGE_SIZE } from 'shared/common';
import { GlobalExceptionFilter } from 'shared/filters';
import { SentryTraces } from 'shared/helpers/sentry/sentry.traces.sampler';
import { HttpResponseInterceptor } from 'shared/interceptor';
import { TracingHandlerInterceptor } from 'shared/interceptor/sentry.handlers.interceptor';
import { WINSTON_MODULE_NEST_PROVIDER } from 'shared/logger/winston.constants';
import { ZIPKIN_MODULE_OPTIONS, ZIPKIN_MODULE_PROVIDER } from 'shared/services/zipkin/zipkin.constants';
import { IZipkinModuleOptions } from 'shared/services/zipkin/zipkin.interface';
import { ZipkinService } from 'shared/services/zipkin/zipkin.service';
import { FastifyZipkinPlugin } from './shared/helpers';

/**
 * entrance method
 */
async function bootstrap() {
  const fastifyAdapter = new FastifyAdapter({ logger: isDevMode, bodyLimit: GRPC_MAX_PACKAGE_SIZE });
  fastifyAdapter.register(fastifyMultipart);
  // registe helmet in fastify to avoid conflict with swagger
  fastifyAdapter.register(helmet, {
    // update script-src to be compatible with swagger
    contentSecurityPolicy: {
      directives: {
        'default-src': ["'self'"],
        'base-uri': ["'self'"],
        'block-all-mixed-content': [],
        'font-src': ["'self'", 'https:', 'data:'],
        'frame-ancestors': ["'self'"],
        'img-src': ["'self'", 'data:'],
        'object-src': ["'none'"],
        'script-src': ["'self'", "'unsafe-inline'"],
        'script-src-attr': ["'none'"],
        'style-src': ["'self'", 'https:', "'unsafe-inline'"],
        'upgrade-insecure-requests': [],
      },
    },
  });

  const nestApp = await NestFactory.create<NestFastifyApplication>(AppModule, fastifyAdapter);

  const zipkinOptions = nestApp.get<IZipkinModuleOptions>(ZIPKIN_MODULE_OPTIONS);
  if (zipkinOptions.enabled && zipkinOptions.endpoint) {
    const zipkinService = nestApp.get<ZipkinService>(ZIPKIN_MODULE_PROVIDER);
    await nestApp.register(FastifyZipkinPlugin.fastifyZipkinPlugin, {
      serviceName: `${environment}-room-server`,
      port: Number(process.env.PORT),
      tracer: zipkinService.tracer,
    });
  }

  const logger = nestApp.get<LoggerService>(WINSTON_MODULE_NEST_PROVIDER);

  nestApp.useLogger(logger);

  initSwagger(nestApp);
  initHttpHook(nestApp);

  const PORT = process.env.PORT || 3333;

  const configService = nestApp.get(ConfigService);

  const sentrySampleRate = configService.get<number>('sentry.tracesSampleRate', 0.2);
  const sentryDsn = process.env.SENTRY_DSN || configService.get<string>('sentry.dsn');

  Sentry.init({
    debug: isDevMode,
    // would not report errors in dev mode
    enabled: Boolean(!isDevMode && sentryDsn),
    dsn: sentryDsn,
    environment: process.env.ENV,
    integrations: [
      new Sentry.Integrations.Http({ tracing: true }),
      new Tracing.Integrations.Mysql(),
      new Sentry.Integrations.OnUncaughtException({
        onFatalError: err => {
          if (err.name === 'SentryError') {
            console.log(err);
          } else {
            (Sentry.getCurrentHub().getClient<Client>() as Client).captureException(err);
            process.exit(1);
          }
        },
      }),
      new Sentry.Integrations.OnUnhandledRejection({ mode: 'warn' }),
    ],
    tracesSampler: new SentryTraces(sentrySampleRate).tracesSampler(),
    ignoreErrors: ['ServerException', 'ApiException'],
  });

  // express performance traces
  // nestApp.use(Sentry.Handlers.requestHandler());

  // global exception filter
  nestApp.useGlobalFilters(new GlobalExceptionFilter(logger, nestApp.get<I18nService>(I18nService)));

  // tracing all the requests by sentry
  nestApp.useGlobalInterceptors(new TracingHandlerInterceptor());

  // global intercept with standard format
  nestApp.useGlobalInterceptors(new HttpResponseInterceptor());

  // global pipes for custom validation
  nestApp.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      stopAtFirstError: true,
    }),
  );

  // enable shutdown hooks
  nestApp.enableShutdownHooks();

  // print running environment
  logger.log(`Application[${APPLICATION_NAME}]-Env[${environment}]`, 'Bootstrap');
  // grpc
  const grpcUrl = configService.get<string>('grpc.url');
  logger.log(`The grpc url is [${grpcUrl}]`);
  nestApp.connectMicroservice<MicroserviceOptions>({
    transport: Transport.GRPC,
    options: {
      url: grpcUrl,
      package: [protobufPackage],
      protoPath: [join(__dirname, 'grpc/generated/serving/RoomServingService.proto'), join(__dirname, 'grpc/generated/common/Core.proto')],
      // 100M
      maxSendMessageLength: GRPC_MAX_PACKAGE_SIZE,
      maxReceiveMessageLength: GRPC_MAX_PACKAGE_SIZE,
      loader: {
        json: true,
      },
    },
  });
  await nestApp.startAllMicroservices();
  // eslint-disable-next-line react-hooks/rules-of-hooks
  useContainer(nestApp.select(AppModule), { fallbackOnErrors: true });
  // listening port
  await nestApp.listen(+PORT, '0.0.0.0');
  // print server info
  logger.log(`The service is running, please visit it: [ ${await nestApp.getUrl()} ]`, 'Bootstrap');
}

bootstrap();
