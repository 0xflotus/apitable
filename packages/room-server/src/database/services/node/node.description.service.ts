import { Injectable } from '@nestjs/common';
import { NodeDescRepository } from '../../repositories/node.desc.repository';

@Injectable()
export class NodeDescriptionService {
  constructor(private readonly repository: NodeDescRepository) {}

  async getDescription(nodeId: string): Promise<string | null> {
    const rawData = await this.repository.selectDescriptionByNodeId(nodeId);
    if (rawData) return rawData.description;
    return null;
  }
}
