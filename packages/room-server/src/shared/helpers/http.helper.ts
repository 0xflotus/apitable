import { IAuthHeader } from 'shared/interfaces/axios.interfaces';

/**
 * set auth headers
 * @param cookie Cookie KEY
 * @param token Authorization Header
 */
export function createAuthHeaders({ cookie, token }: IAuthHeader): any {
  if (cookie) {
    return {
      Cookie: cookie,
    };
  }

  if (token) {
    return {
      Authorization: token,
    };
  }
}

export function withSpaceIdHeader(header: any, spaceId?: string) {
  return spaceId ? { ...header, 'X-Space-Id': spaceId } : header;
}
