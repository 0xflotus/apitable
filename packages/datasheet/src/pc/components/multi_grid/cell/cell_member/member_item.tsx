import { IUnitValue, IUserValue, MemberType, t, Strings } from '@apitable/core';
import classNames from 'classnames';
import { Avatar, AvatarSize, AvatarType } from 'pc/components/common';
import { getSocialWecomUnitName } from 'pc/components/home/social_platform';
import * as React from 'react';
import { useSelector } from 'react-redux';
import styles from './styles.module.less';
import MemberIcon from 'static/icon/space/space_icon_account.svg';
import { getThemeColors } from '@apitable/components';

interface IMemberItemProps {
  unitInfo: IUnitValue | IUserValue;
  style?: React.CSSProperties;
  selected?: boolean;
  showTeams?: boolean;
}

export function isUnitLeave(unit: IUnitValue | IUserValue) {
  if (!unit) return true;
  if (unit.isDeleted) { return true; }
  return unit.type === MemberType.Member && !unit.isActive;
}

export const MemberItem: React.FC<IMemberItemProps> = props => {
  const { unitInfo, children, style, selected, showTeams } = props;
  const { unitId, avatar, name, type, userId, isSelf, desc, isMemberNameModified, team, email, isActive } = unitInfo as any;
  const spaceInfo = useSelector(state => state.space.curSpaceInfo);
  const colors = getThemeColors();

  const title = getSocialWecomUnitName({
    name,
    isModified: isMemberNameModified,
    spaceInfo
  });

  if (showTeams) {
    return (
      <div className={styles.memberWithTeams}>
        <Avatar
          id={unitId || userId!}
          title={name}
          size={AvatarSize.Size24}
          src={avatar}
          type={type === MemberType.Member ? AvatarType.Member : AvatarType.Team}
          style={{ minWidth: 20 }}
          defaultIcon={isSelf ? <MemberIcon width={12} height={12} fill={colors.defaultBg} /> : undefined}
        />
        <div className={styles.memberWithTeamsDesc}>
          <div className={classNames('unitName', styles.unitName)}>
            {title}
            {!isActive && <div className={styles.unInvited}>{t(Strings.pending_invite)}</div>}
          </div>
          {team && <div className={styles.teams}>{team}</div>}
          {email && <div className={styles.teams}>{email}</div>}
        </div>
      </div>
    );
  }
  
  return (
    <span
      className={classNames('unitMember', {
        [styles.unitItemWrapper]: true,
        [styles.unitTeam]: unitInfo.type === MemberType.Team,
        [styles.unitMember]: unitInfo.type === MemberType.Member,
        [styles.unitTeam]: unitInfo.type === MemberType.Role,
        [styles.isLeave]: isUnitLeave(unitInfo),
        [styles.selected]: selected,
      })}
      style={style}
    >
      <Avatar
        id={unitId || userId!}
        title={name}
        size={AvatarSize.Size20}
        src={avatar}
        type={type === MemberType.Member ? AvatarType.Member : AvatarType.Team}
        style={{ minWidth: 20 }}
        defaultIcon={isSelf ? <MemberIcon width={12} height={12} fill={colors.defaultBg} /> : undefined}
      />
      <span className={classNames('unitName', styles.unitName)}>{title}</span>
      {desc && <span className={styles.unitDesc}>{`（${desc}）`}</span>}
      {children}
    </span>
  );
};