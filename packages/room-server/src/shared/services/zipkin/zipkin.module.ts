import { DynamicModule, Global, Module } from '@nestjs/common';
import {
  IZipkinModuleAsyncOptions,
  IZipkinModuleOptions,
} from './zipkin.interface';
import {
  createZipkinOptionAsyncProviders,
  createZipkinOptionProviders,
} from './zipkin.providers';
import { ZipkinService } from './zipkin.service';

@Global()
@Module({
  providers: [ZipkinService],
  exports: [ZipkinService],
  })
export class ZipkinModule {

  static forRoot(options: IZipkinModuleOptions): DynamicModule {
    const providers = createZipkinOptionProviders(options);
    return {
      module: ZipkinModule,
      providers,
      exports: providers,
    };
  }

  static forRootAsync(options: IZipkinModuleAsyncOptions): DynamicModule {
    const providers = createZipkinOptionAsyncProviders(options);
    return {
      module: ZipkinModule,
      imports: options.imports,
      providers,
      exports: providers,
    } as DynamicModule;
  }
}