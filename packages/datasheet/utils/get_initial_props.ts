import { getEnvVars } from 'get_env';
import { NextPageContext } from 'next';
import { FILTER_HEADERS } from './constant';

const filterCustomHeader = (headers?: Record<string, string | string[] | undefined>): Record<string, string> => {
  if (!headers) return {};
  const _headers = {};
  for (const k in headers) {
    if (!FILTER_HEADERS.map(item => item.toUpperCase()).includes(k.toUpperCase())) {
      continue;
    }
    _headers[k] = headers[k];
  }
  return _headers;
};

export const getInitialProps = async(context: { ctx: NextPageContext }) => {
  const envVars = getEnvVars();
  const cookie = context.ctx.req?.headers.cookie;
  const filterHeaders = filterCustomHeader(context.ctx.req?.headers);

  const baseResponse = {
    env: process.env.ENV,
    version: process.env.WEB_CLIENT_VERSION,
    envVars: JSON.stringify(envVars),
  };

  const host = process.env.API_PROXY;

  if (!host) {
    return {
      clientInfo: baseResponse
    };
  }

  const language = context.ctx.req?.headers['accept-language'];
  const headers: Record<string, string> = { ...filterHeaders };

  let locale = 'zh-CN';
  if (cookie) {
    headers.cookie = cookie;
    const getCookie = (name: string) => {
      const value = `; ${cookie}`;
      const parts = value.split(`; ${name}=`);
      if (parts.length >= 2) return parts[1].split(';').shift();
      return null;
    }
    // server lang
    const langParts = getCookie('lang');
    // client cache cookie while language toggle
    const localeParts = getCookie('client-lang');
    locale = localeParts || langParts || locale;
  }

  if (language) {
    headers['Accept-Language'] = language;
  }

  return {
    ...baseResponse,
    locale,
  }
}