import { Column, Entity, PrimaryColumn } from 'typeorm';

/**
 * Workbench-Node Description
 */
@Entity('vika_node_desc')
export class NodeDescEntity {
  @PrimaryColumn('bigint')
    id: string;

  @Column({
    name: 'node_id',
    nullable: false,
    unique: true,
    comment: 'node Id(related#vika_node#node_id)',
    length: 50,
  })
    nodeId: string;

  @Column({
    name: 'description',
    type: 'text',
    nullable: false,
    comment: 'node description',
  })
    description: string | '';

  @Column('timestamp', {
    name: 'created_at',
    comment: 'created time',
    default: () => 'CURRENT_TIMESTAMP',
  })
    createdAt: Date;

  @Column('timestamp', {
    name: 'updated_at',
    nullable: true,
    comment: 'updated time',
    default: () => 'CURRENT_TIMESTAMP',
  })
    updatedAt: Date | null;
}
